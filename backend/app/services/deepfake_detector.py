import torch
import base64
import io
from PIL import Image
from transformers import pipeline
import numpy as np
import os
import tempfile
import traceback
import warnings
from fastapi import HTTPException, status

# Disable facenet's attempt to move model to device
os.environ["FORCE_CPU"] = "1"

# Singleton class for the deepfake detector
class DeepfakeDetector:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(DeepfakeDetector, cls).__new__(cls)
            cls._instance.pipe = None
            cls._instance.initialized = False
        return cls._instance
    
    def initialize(self):
        """Initialize the deepfake detection model."""
        if not self.initialized:
            try:
                print("Initializing deepfake detection model (this may take several minutes)...")
                print("Downloading model files...")
                
                # Suppress all warnings
                warnings.filterwarnings("ignore")
                
                # Force CPU usage for model loading
                torch.hub.set_dir(os.path.join(os.path.expanduser("~"), ".cache", "torch"))
                
                # Monkey patch for meta parameters
                original_to = torch.nn.Module.to
                def safe_to(self, *args, **kwargs):
                    try:
                        return original_to(self, *args, **kwargs)
                    except NotImplementedError as e:
                        if "Cannot copy out of meta tensor" in str(e):
                            print("Ignoring meta tensor error in device transfer")
                            return self
                        raise
                torch.nn.Module.to = safe_to
                
                # Load model with specific device and parameters
                self.pipe = pipeline(
                    model="not-lain/deepfake", 
                    trust_remote_code=True,
                    device_map="cpu"
                )
                
                # Restore original method
                torch.nn.Module.to = original_to
                
                self.initialized = True
                print("Deepfake detection model loaded successfully!")
            except Exception as e:
                print(f"Error loading deepfake detection model: {e}")
                traceback.print_exc()
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail=f"Failed to load deepfake detection model: {str(e)}"
                )
    
    def predict(self, image_data):
        """
        Detect deepfake in the provided image.
        
        Args:
            image_data: PIL.Image object or file path
            
        Returns:
            Dict containing prediction results
        """
        if not self.initialized:
            self.initialize()
            
        temp_file = None
        
        try:
            # The model might be expecting a file path, so let's create a temporary file
            with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp:
                temp_file = temp.name
                
                # Ensure we have an RGB image
                if isinstance(image_data, str):
                    # It's a file path
                    img = Image.open(image_data).convert('RGB')
                else:
                    # It's a PIL Image
                    img = image_data.convert('RGB')
                
                # Save image to temp file
                img.save(temp_file, format='JPEG')
                print(f"Image saved to temporary file: {temp_file}")
            
            # Let's try to use the file path directly for the model
            print(f"Running prediction on file: {temp_file}")
            result = self.pipe.predict(temp_file)
            print(f"Prediction result keys: {list(result.keys())}")
            
            # Convert face_with_mask to base64 if it exists
            face_with_mask_base64 = None
            
            try:
                if "face_with_mask" in result and result["face_with_mask"] is not None:
                    face_mask = result["face_with_mask"]
                    print(f"face_with_mask type: {type(face_mask)}")
                    
                    # Check if it's a numpy array
                    if isinstance(face_mask, np.ndarray):
                        print(f"Processing numpy array with shape: {face_mask.shape}, dtype: {face_mask.dtype}")
                        
                        # Check if it's a valid image array
                        if len(face_mask.shape) < 2:
                            print("Invalid array shape for image conversion")
                            face_with_mask_base64 = None
                        else:
                            # Convert based on array dimensions and type
                            if face_mask.dtype == np.float32 or face_mask.dtype == np.float64:
                                # Scale float arrays to 0-255 range
                                face_mask = (face_mask * 255).astype(np.uint8)
                            
                            # Create a PIL image
                            try:
                                mask_img = Image.fromarray(face_mask)
                                print(f"Converted to PIL Image: {mask_img.size}, {mask_img.mode}")
                                
                                # Convert to base64
                                buffered = io.BytesIO()
                                mask_img.save(buffered, format="PNG")
                                face_with_mask_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
                            except Exception as convert_error:
                                print(f"Error converting numpy array to image: {convert_error}")
                                traceback.print_exc()
                    elif isinstance(face_mask, Image.Image):
                        # It's already a PIL Image
                        print(f"Processing PIL Image: {face_mask.size}, {face_mask.mode}")
                        buffered = io.BytesIO()
                        face_mask.save(buffered, format="PNG")
                        face_with_mask_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
                    else:
                        print(f"Unsupported face_with_mask type: {type(face_mask)}")
            except Exception as mask_error:
                print(f"Error processing face mask: {mask_error}")
                traceback.print_exc()
                # Continue without the mask visualization
                face_with_mask_base64 = None
            
            # Get predicted class (the one with highest confidence)
            confidences = result.get("confidences", {})
            if confidences:
                print(f"Confidence scores: {confidences}")
                predicted_class = max(confidences, key=confidences.get)
            else:
                print("No confidence scores found in result")
                predicted_class = "unknown"
            
            return {
                "confidences": confidences,
                "predicted_class": predicted_class,
                "face_with_mask_base64": face_with_mask_base64
            }
            
        except Exception as e:
            print(f"Error during prediction: {str(e)}")
            traceback.print_exc()
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Prediction failed: {str(e)}"
            )
        finally:
            # Clean up the temp file
            if temp_file and os.path.exists(temp_file):
                try:
                    os.unlink(temp_file)
                    print(f"Temporary file removed: {temp_file}")
                except Exception as e:
                    print(f"Error removing temp file: {str(e)}")

# Create singleton instance
deepfake_detector = DeepfakeDetector() 