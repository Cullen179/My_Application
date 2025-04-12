from fastapi import APIRouter, UploadFile, File, HTTPException, status, BackgroundTasks
from fastapi.responses import JSONResponse
from PIL import Image
import io
import os
import traceback
from typing import Optional
import uuid
import time
import numpy as np
from app.models.deepfake import DeepfakeDetectionResult
from app.services.deepfake_detector import deepfake_detector

router = APIRouter(prefix="/deepfake", tags=["deepfake"])

# Create a directory for temporary file storage
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

def cleanup_file(file_path: str, max_attempts=3, delay=1):
    """
    Remove temporary file after processing with retry logic.
    
    Args:
        file_path: Path to the file to remove
        max_attempts: Maximum number of deletion attempts
        delay: Delay in seconds between attempts
    """
    for attempt in range(max_attempts):
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
                print(f"Successfully removed temporary file: {file_path}")
                return
        except Exception as e:
            print(f"Error removing temporary file {file_path} (attempt {attempt+1}/{max_attempts}): {e}")
            if attempt < max_attempts - 1:
                time.sleep(delay)  # Wait before retry
    
    print(f"Failed to remove temporary file after {max_attempts} attempts: {file_path}")

@router.post("/detect", response_model=DeepfakeDetectionResult)
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

@router.get("/status")
def model_status():
    """Check if the deepfake detection model is initialized."""
    return {
        "initialized": deepfake_detector.initialized,
        "model": "not-lain/deepfake"
    } 