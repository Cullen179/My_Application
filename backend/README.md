# FastAPI Firebase Authentication with Deepfake Detection

A FastAPI project with Firebase authentication and deepfake detection.

## Project Structure

```
backend/
│
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── core/
│   │   ├── config.py
│   │   └── firebase.py
│   ├── dependencies/
│   │   └── firebase_auth.py
│   ├── models/
│   │   ├── token.py
│   │   ├── user.py
│   │   └── deepfake.py
│   ├── services/
│   │   └── deepfake_detector.py
│   └── routers/
│       ├── __init__.py
│       ├── auth.py
│       ├── protected.py
│       └── deepfake.py
│
├── uploads/           # Temporary directory for uploaded images
├── .env
├── .env.example
├── firebase-credentials.json.example
├── requirements.txt
├── test_auth.html     # Test page for Firebase authentication
├── test_deepfake.html # Test page for deepfake detection
└── README.md
```

## Setup

### 1. Install pipx (if not already installed)

```
pip install pipx
```

### 2. Install FastAPI and Uvicorn using pipx

```
pipx install fastapi
pipx install uvicorn
```

### 3. Install project requirements

```
pip install -r requirements.txt
```

### 4. Setup environment variables

Rename the `.env.example` file to `.env` and update the values:

```
SECRET_KEY=your-secret-key
FIREBASE_CREDENTIALS_PATH=firebase-credentials.json
```

### 5. Setup Firebase credentials

To use Firebase authentication:

1. Create a Firebase project in the Firebase console
2. Generate a new service account key in Project settings > Service accounts
3. Save the JSON credentials file as `firebase-credentials.json` in the backend directory

## Running the API

Navigate to the project's backend directory and run:

```
uvicorn app.main:app --reload
```

Or use the alternative method:

```
python -m app.main
```

The API will be available at:
- API: http://localhost:8000
- Documentation: http://localhost:8000/docs
- Alternative documentation: http://localhost:8000/redoc

## Test Pages

The project includes two HTML test pages:

1. **test_auth.html** - For testing Firebase authentication
2. **test_deepfake.html** - For testing deepfake detection

To use these test pages, open them in a browser after starting the backend server.

## Authentication Flow

This API uses Firebase Authentication. The proper authentication flow is:

1. **Register a user** (if needed):
   - Call the `/api/v1/auth/register` endpoint to create a new user

2. **Authentication on Frontend**:
   - Use Firebase Client SDK in your frontend to authenticate users
   - Example with Firebase JS SDK:
   ```javascript
   // Initialize Firebase app with your config
   import { initializeApp } from "firebase/app";
   import { getAuth, signInWithEmailAndPassword } from "firebase/auth";
   
   const firebaseConfig = { /* your firebase config */ };
   const app = initializeApp(firebaseConfig);
   const auth = getAuth(app);
   
   // Login and get ID token
   async function login(email, password) {
     try {
       const userCredential = await signInWithEmailAndPassword(auth, email, password);
       const idToken = await userCredential.user.getIdToken();
       
       // Store the token for API calls
       localStorage.setItem('firebaseToken', idToken);
       
       return idToken;
     } catch (error) {
       console.error("Authentication failed:", error);
     }
   }
   ```

3. **Use the ID token for API requests**:
   ```javascript
   async function callProtectedAPI() {
     const token = localStorage.getItem('firebaseToken');
     
     const response = await fetch('http://localhost:8000/api/v1/protected', {
       headers: {
         'Authorization': `Bearer ${token}`
       }
     });
     
     return await response.json();
   }
   ```

## API Endpoints

### Authentication Endpoints

- `POST /api/v1/auth/register`: Register a new user with Firebase
- `POST /api/v1/auth/verify-token`: Verify a Firebase ID token
- `GET /api/v1/auth/me`: Get current user info from Firebase token

Example user registration:
```
curl -X POST "http://localhost:8000/api/v1/auth/register" -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"password123","full_name":"Test User"}'
```

Example token verification:
```
curl -X POST "http://localhost:8000/api/v1/auth/verify-token" -H "Content-Type: application/json" -d '{"id_token":"YOUR_FIREBASE_ID_TOKEN"}'
```

### Protected Routes

- `GET /api/v1/protected`: Protected endpoint requiring Firebase authentication

Example:
```
curl -X GET "http://localhost:8000/api/v1/protected" -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

### Deepfake Detection Endpoints

- `POST /api/v1/deepfake/detect`: Detect if an image is a deepfake
- `GET /api/v1/deepfake/status`: Check if the deepfake detection model is initialized

Example deepfake detection:
```
curl -X POST "http://localhost:8000/api/v1/deepfake/detect" -F "file=@/path/to/image.jpg"
```

The deepfake detection endpoint returns:
- `confidences`: Confidence scores for each class (real/fake)
- `predicted_class`: The predicted class (real/fake)
- `face_with_mask_base64`: Base64-encoded image showing the areas that contributed to the classification

## Health Check

- `GET /health`: Health check endpoint to verify the API is running

## Deepfake Detection Model

The project uses the "not-lain/deepfake" model from Hugging Face for deepfake detection. The model will be automatically downloaded when the application starts or when the first detection request is made.

Key features:
- Face detection for accurate analysis
- Explainability heatmaps showing which parts of the image influenced the decision
- Confidence scores for both "real" and "fake" classes