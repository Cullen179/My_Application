from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional
from enum import Enum

class DeepfakeDetectionResult(BaseModel):
    """Model for deepfake detection results"""
    confidences: Dict[str, float] = Field(default_factory=dict, description="Confidence scores for each class")
    predicted_class: str = Field(..., description="The predicted class (real or fake)")
    face_with_mask_base64: Optional[str] = Field(
        default=None, 
        description="Base64-encoded image showing areas that contributed to the classification"
    ) 