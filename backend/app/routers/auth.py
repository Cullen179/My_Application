from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import Dict

from app.core.firebase import firebase_initialized, verify_firebase_token, create_firebase_user
from app.dependencies.firebase_auth import get_current_firebase_user
from app.models.user import User, UserCreate
from app.models.token import FirebaseToken

router = APIRouter(prefix="/auth", tags=["authentication"])

@router.post("/register", response_model=User)
async def register(user_data: UserCreate):
    """Register a new user with Firebase."""
    if not firebase_initialized:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase authentication not configured"
        )
    
    user = await create_firebase_user(user_data)
    return user

@router.post("/verify-token", response_model=User)
async def verify_token(token: FirebaseToken):
    """Verify a Firebase ID token and return the user information."""
    if not firebase_initialized:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase authentication not configured"
        )
    
    firebase_user = await verify_firebase_token(token.id_token)
    
    # Convert Firebase user to our User model
    user = User(
        id=firebase_user.get("uid"),
        email=firebase_user.get("email"),
        full_name=firebase_user.get("name")
    )
    
    return user

@router.get("/me", response_model=User)
async def get_current_user_info(current_user: User = Depends(get_current_firebase_user)):
    """Return the current user based on the Firebase token in the Authorization header."""
    return current_user 