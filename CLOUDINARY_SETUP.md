# Studiora - Course Management System

## Cloudinary Setup Instructions

This app uses Cloudinary for file uploads (photos, PDFs, videos). Follow these steps to configure Cloudinary:

### 1. Create a Cloudinary Account
- Go to [https://cloudinary.com/](https://cloudinary.com/)
- Sign up for a free account
- After signing up, you'll be taken to the Dashboard

### 2. Get Your Cloudinary Credentials
From your Cloudinary Dashboard, you'll find:
- **Cloud Name**
- **API Key**
- **API Secret**

### 3. Configure the App
Open the file: `app/src/main/java/com/example/studiora/util/CloudinaryHelper.kt`

Replace the placeholder values with your actual credentials:

```kotlin
private const val CLOUD_NAME = "your_cloud_name_here"
private const val API_KEY = "your_api_key_here"
private const val API_SECRET = "your_api_secret_here"
```

### 4. Cloudinary Upload Settings (Optional)
You can customize upload settings in Cloudinary Dashboard:
- Set upload presets
- Configure file size limits
- Set allowed file formats
- Enable/disable transformations

## Features

### Admin Features - Course Management
- **Manage Courses**: Admin can view all courses and manage them
- **Upload Course Materials**: Admin can upload study materials for any course
  - Upload photos (JPG, PNG, etc.)
  - Upload PDFs
  - Upload videos
  - Add links to external resources
- **Material Types Supported**:
  - PDF documents
  - Images (photos)
  - Videos
  - Links (URLs to external resources)
  - Other file types

### Teacher Features
- Teachers can also upload materials to their own courses
- Automatic file type detection
- Cloudinary integration for reliable file hosting

### File Upload Modes
1. **Upload File**: Select a file from your device (auto-uploaded to Cloudinary)
2. **Enter URL**: Manually enter a URL for external resources

## Security Notes

⚠️ **Important**: Never commit your Cloudinary credentials to version control!

For production apps:
1. Store credentials in `local.properties` or environment variables
2. Use BuildConfig to access credentials
3. Add `local.properties` to `.gitignore`

Example secure configuration:
```kotlin
// In local.properties
cloudinary.cloud.name=your_cloud_name
cloudinary.api.key=your_api_key
cloudinary.api.secret=your_api_secret

// In build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", 
            "\"${project.findProperty("cloudinary.cloud.name")}\"")
        // ... similar for API_KEY and API_SECRET
    }
}

// In CloudinaryHelper.kt
private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
```

## Dependencies Added

The following dependencies have been added to support file uploads:
- `cloudinary-android:2.5.0` - Cloudinary Android SDK
- `okhttp:4.12.0` - HTTP client (required by Cloudinary)

## Troubleshooting

### Upload Fails
- Verify your Cloudinary credentials are correct
- Check your internet connection
- Ensure the file size is within Cloudinary's limits (10MB for free tier)

### "Permission Denied" errors
- Make sure you're logged in as an admin or teacher
- Check Firebase database rules allow authenticated users to write

### File type not supported
- The app supports: Images, PDFs, Videos, and other file types
- Cloudinary automatically handles most file formats

## Usage

### For Admin:
1. Navigate to "Manage Courses" from the admin dashboard
2. Click on a course
3. Click "Manage Materials" button
4. Click the "+" button to add new material
5. Choose between "Upload File" or "Enter URL"
6. Fill in the title, description, and select/upload the file
7. Click "Upload" or "Add"

### For Teachers:
1. Navigate to "My Courses"
2. Select a course
3. Click on the course to view materials
4. Use the "+" button to upload new materials (same as admin process)

## Firebase Configuration

Make sure your Firebase Realtime Database rules allow:
- Teachers to upload materials to their courses
- Admins to manage all materials
- Students to read materials

Example rules:
```json
{
  "rules": {
    "materials": {
      ".read": "auth != null",
      "$materialId": {
        ".write": "auth != null && (
          root.child('users').child(auth.uid).child('role').val() === 'ADMIN' ||
          root.child('users').child(auth.uid).child('role').val() === 'TEACHER'
        )"
      }
    }
  }
}
```

