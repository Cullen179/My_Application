from pydantic import BaseModel

class FirebaseToken(BaseModel):
    """Model for Firebase ID token."""
    id_token: str

class CustomToken(BaseModel):
    """Model for Firebase custom token response."""
    custom_token: str
    uid: str 