from fastapi import FastAPI, File, UploadFile, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import io
import traceback
from deepfake import DeepfakeDetector, DeepfakeDetectionResult

app = FastAPI()

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

# Initialize the deepfake detector
deepfake_detector = DeepfakeDetector()

@app.post("/detect_deepfake", response_model=DeepfakeDetectionResult)
async def detect_deepfake(file: UploadFile = File(...)):
    """
    Detect if an image is a deepfake.
    
    - **file**: The image file to analyze
    """
    # Initialize model if not already done
    if not deepfake_detector.initialized:
        try:
            deepfake_detector.initialize()
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Could not initialize deepfake model: {str(e)}"
            )
    
    # Validate file type
    if not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File must be an image"
        )
    
    try:
        # Read the file into memory
        contents = await file.read()
        
        # Log file info for debugging
        print(f"Processing file: {file.filename}, size: {len(contents)} bytes, content-type: {file.content_type}")
        
        # Process the image directly in memory
        try:
            image = Image.open(io.BytesIO(contents))
            print(f"Image opened successfully: {image.format}, mode: {image.mode}, size: {image.size}")
        except Exception as img_error:
            print(f"Error opening image: {str(img_error)}")
            traceback.print_exc()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Could not process image: {str(img_error)}"
            )
        
        # Run prediction on the image
        try:
            result = deepfake_detector.predict(image)
        except Exception as pred_error:
            print(f"Prediction error: {str(pred_error)}")
            traceback.print_exc()
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Prediction failed: {str(pred_error)}"
            )
        
        return DeepfakeDetectionResult(
            confidences=result["confidences"],
            predicted_class=result["predicted_class"],
            face_with_mask_base64=result["face_with_mask_base64"]
        )
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except Exception as e:
        print(f"Unexpected error: {str(e)}")
        traceback.print_exc()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing image: {str(e)}"
        ) 