from fastapi import APIRouter, Depends
from app.dependencies.firebase_auth import get_current_firebase_user
from app.models.user import User
from pydantic import BaseModel
from typing import Dict, Any

router = APIRouter(prefix="/protected", tags=["protected"])

class ProtectedResponse(BaseModel):
    message: str
    user: User
    data: Dict[str, Any]

@router.get("", response_model=ProtectedResponse)
async def protected_route(current_user: User = Depends(get_current_firebase_user)):
    """A protected route that requires Firebase authentication."""
    return {
        "message": "You have successfully accessed the protected endpoint!",
        "user": current_user,
        "data": {
            "timestamp": "2023-09-25T12:34:56Z",
            "resource_accessed": "test_resource",
            "access_level": "standard"
        }
    } 