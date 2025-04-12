from fastapi import Depends, HTTPException, status, Header
from typing import Optional

from app.core.firebase import verify_firebase_token
from app.models.user import User

async def get_current_firebase_user(
    authorization: Optional[str] = Header(None)
) -> User:
    """Dependency to get the current user from Firebase ID token."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    token = authorization.replace("Bearer ", "")
    firebase_user = await verify_firebase_token(token)
    
    # Create a User object from Firebase user data
    user = User(
        id=firebase_user.get("uid"),
        email=firebase_user.get("email"),
        full_name=firebase_user.get("name")
    )
    
    return user 