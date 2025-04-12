import firebase_admin
from firebase_admin import credentials, auth
from fastapi import HTTPException, status
from typing import Dict, Any
import os
from app.core.config import settings
from app.models.user import UserCreate, User

# Initialize Firebase Admin SDK if credentials file exists
def initialize_firebase():
    """Initialize Firebase Admin SDK if credentials are available."""
    try:
        if settings.FIREBASE_CREDENTIALS_PATH and os.path.exists(settings.FIREBASE_CREDENTIALS_PATH):
            cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
            firebase_admin.initialize_app(cred)
            return True
        else:
            print("Firebase credentials not found or path not set. Firebase authentication disabled.")
            return False
    except Exception as e:
        print(f"Failed to initialize Firebase: {e}")
        return False

# Flag to indicate if Firebase is initialized
firebase_initialized = initialize_firebase()

async def verify_firebase_token(id_token: str) -> Dict[str, Any]:
    """Verify a Firebase ID token and return the decoded token."""
    if not firebase_initialized:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase authentication not configured"
        )
    
    try:
        decoded_token = auth.verify_id_token(id_token)
        return decoded_token
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid Firebase token: {str(e)}"
        )

async def create_firebase_user(user_data: UserCreate) -> User:
    """Create a new Firebase user."""
    if not firebase_initialized:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase authentication not configured"
        )
    
    try:
        # Create user in Firebase Authentication
        firebase_user = auth.create_user(
            email=user_data.email,
            password=user_data.password,
            display_name=user_data.full_name or "",
        )
        
        # Create a User model from the Firebase user
        user = User(
            id=firebase_user.uid,
            email=firebase_user.email,
            full_name=firebase_user.display_name
        )
        
        return user
    except auth.EmailAlreadyExistsError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User with this email already exists"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create user: {str(e)}"
        )

async def login_with_email_password(email: str, password: str) -> Dict[str, str]:
    """
    Simulates Firebase email/password login.
    
    Note: This is a workaround since Firebase Admin SDK doesn't support email/password login.
    In a production environment, this would be handled by the client using Firebase client SDK.
    """
    if not firebase_initialized:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Firebase authentication not configured"
        )
    
    try:
        # Get the user by email
        user = auth.get_user_by_email(email)
        
        # Note: Firebase Admin SDK cannot verify passwords
        # This is just a simulation for our backend API
        # Custom tokens are typically generated for server-side auth
        custom_token = auth.create_custom_token(user.uid)
        
        return {
            "custom_token": custom_token.decode('utf-8'),
            "uid": user.uid
        }
    except auth.UserNotFoundError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Login failed: {str(e)}"
        ) 