# Course Material Management - Navigation Flow

## Admin Flow

```
Admin Dashboard
    ↓
Manage Courses (click from dashboard)
    ↓
Courses List Screen
    ↓
Click "Manage Materials" on any course
    ↓
Course Materials Screen
    ↓
Click "+" FAB button
    ↓
Add Material Dialog
    ├─→ Upload File Mode
    │   ├─ Select file from device
    │   ├─ File auto-detected type
    │   ├─ Upload to Cloudinary
    │   └─ Save to Firebase
    │
    └─→ Enter URL Mode
        ├─ Enter external URL
        └─ Save to Firebase
```

## Teacher Flow (Existing + Enhanced)

```
Teacher Dashboard
    ↓
My Courses
    ↓
Select a Course
    ↓
View Materials
    ↓
Click "+" FAB button
    ↓
Add Material Dialog (Now with Cloudinary upload!)
    ├─→ Upload File Mode (NEW!)
    │   └─ Upload via Cloudinary
    │
    └─→ Enter URL Mode (Existing)
        └─ Direct URL entry
```

## Student Flow

```
Student Dashboard
    ↓
My Courses
    ↓
Select a Course
    ↓
View Materials (Read-only)
    ↓
Click "Open" on any material
    ↓
Material opens in external app/browser
```

## Screen Components

### 1. Admin Courses Screen (`AdminCoursesScreen.kt`)
```
┌─────────────────────────────────────┐
│ ← Manage Courses              [+]   │
├─────────────────────────────────────┤
│                                     │
│  ┌───────────────────────────────┐ │
│  │ 📚 Mathematics                │ │
│  │ Teacher: John Doe             │ │
│  │ Class: Grade 10A              │ │
│  │ ┌───────────────────────────┐ │ │
│  │ │ 📤 Manage Materials       │ │ │ ← New button
│  │ └───────────────────────────┘ │ │
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ 📚 Physics                    │ │
│  │ Teacher: Jane Smith           │ │
│  │ Class: Grade 10A              │ │
│  │ ┌───────────────────────────┐ │ │
│  │ │ 📤 Manage Materials       │ │ │
│  │ └───────────────────────────┘ │ │
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### 2. Course Materials Screen (`AdminCourseMaterialsScreen.kt`)
```
┌─────────────────────────────────────┐
│ ← Course Materials            [+]   │
│   Mathematics                       │
├─────────────────────────────────────┤
│ 3 Material(s)                       │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ 📄 Chapter 1 Notes      [↗][🗑]│ │
│  │ Introduction to Algebra       │ │
│  │ By: Admin User                │ │
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ 🖼️ Diagram              [↗][🗑]│ │
│  │ Algebraic expressions         │ │
│  │ By: Admin User                │ │
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ ▶️ Video Tutorial       [↗][🗑]│ │
│  │ Solving equations             │ │
│  │ By: Teacher Name              │ │
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### 3. Add Material Dialog
```
┌─────────────────────────────────────┐
│ Add Study Material             [×]  │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────┐ ┌───────────────┐ │
│  │ Upload File │ │  Enter URL    │ │ ← Mode toggle
│  │   (Active)  │ │  (Inactive)   │ │
│  └─────────────┘ └───────────────┘ │
│                                     │
│  Title:                             │
│  ┌─────────────────────────────────┐│
│  │ Chapter 1 Notes               ││ │
│  └─────────────────────────────────┘│
│                                     │
│  Description (optional):            │
│  ┌─────────────────────────────────┐│
│  │ Introduction to Algebra       ││ │
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 📎 Select File                 ││ │ ← File picker
│  └─────────────────────────────────┘│
│  File: chapter1.pdf ✓               │
│                                     │
│  File Type:                         │
│  ┌─────────────────────────────────┐│
│  │ PDF                        [▼] ││ │ ← Dropdown
│  └─────────────────────────────────┘│
│                                     │
│         [Cancel]     [Upload]       │
│                                     │
└─────────────────────────────────────┘
```

## Material Type Icons

| File Type | Icon | Color |
|-----------|------|-------|
| PDF       | 📄   | Red   |
| IMAGE     | 🖼️   | Purple|
| VIDEO     | ▶️   | Blue  |
| LINK      | 🔗   | Primary|
| OTHER     | 📋   | Primary|

## User Permissions

| Action | Admin | Teacher | Student |
|--------|-------|---------|---------|
| View Materials | ✅ | ✅ | ✅ |
| Upload to Any Course | ✅ | ❌ | ❌ |
| Upload to Own Course | ✅ | ✅ | ❌ |
| Delete Any Material | ✅ | ❌ | ❌ |
| Delete Own Material | ✅ | ✅ | ❌ |
| Open/Download Material | ✅ | ✅ | ✅ |

## Data Flow

### Upload File Flow
```
User selects file
    ↓
File picker returns URI
    ↓
Auto-detect file type
    ↓
User fills title/description
    ↓
Click "Upload"
    ↓
Show progress indicator
    ↓
CloudinaryHelper.uploadFile(uri)
    ↓
[Upload to Cloudinary servers]
    ↓
Receive secure URL
    ↓
AdminViewModel.addStudyMaterial(url)
    ↓
[Save to Firebase Realtime Database]
    ↓
Refresh materials list
    ↓
Hide progress indicator
    ↓
Show success message
```

### Enter URL Flow
```
User enters URL
    ↓
User fills title/description
    ↓
User selects file type
    ↓
Click "Add"
    ↓
AdminViewModel.addStudyMaterial(url)
    ↓
[Save to Firebase Realtime Database]
    ↓
Refresh materials list
    ↓
Show success message
```

## Firebase Database Structure

```json
{
  "materials": {
    "materialId1": {
      "materialId": "materialId1",
      "title": "Chapter 1 Notes",
      "description": "Introduction to Algebra",
      "fileUrl": "https://res.cloudinary.com/...",
      "fileType": "PDF",
      "courseId": "courseId1",
      "uploadedBy": "adminId1",
      "uploaderName": "Admin User",
      "createdAt": 1234567890
    },
    "materialId2": {
      "materialId": "materialId2",
      "title": "Tutorial Video",
      "description": "Solving equations",
      "fileUrl": "https://res.cloudinary.com/...",
      "fileType": "VIDEO",
      "courseId": "courseId1",
      "uploadedBy": "teacherId1",
      "uploaderName": "Teacher Name",
      "createdAt": 1234567891
    }
  }
}
```

## Cloudinary Storage Structure

```
studiora/
  ├── upload_1234567890.pdf
  ├── upload_1234567891.jpg
  ├── upload_1234567892.mp4
  └── ...
```

## State Management

### AdminViewModel States

```kotlin
// Materials for current course
_materials: MutableStateFlow<List<StudyMaterial>> = emptyList()

// Operation state (Loading, Success, Error)
_operationState: MutableStateFlow<OperationState> = Idle
```

### Screen States

```kotlin
// UI state
showAddDialog: Boolean = false
materialToDelete: StudyMaterial? = null
isUploading: Boolean = false

// Upload mode
uploadMode: Boolean = true  // true = upload, false = URL
selectedUri: Uri? = null
```

## Error Handling

### Upload Errors
- No file selected → Show toast
- Upload failed → Show error message
- Network error → Show retry option
- File too large → Show size limit message

### Validation Errors
- Empty title → "Enter title"
- No file selected → "Select a file"
- Empty URL → "Enter URL"

### Permission Errors
- Not authenticated → Redirect to login
- Insufficient permissions → Show error message
- Database write denied → Check Firebase rules

## Testing Scenarios

1. **Upload PDF**
   - Select PDF file
   - Verify upload to Cloudinary
   - Verify saved to Firebase
   - Verify appears in list

2. **Upload Image**
   - Select image file
   - Verify type auto-detected
   - Verify upload successful
   - Verify thumbnail display

3. **Add External URL**
   - Switch to URL mode
   - Enter YouTube link
   - Verify saved correctly
   - Verify opens in browser

4. **Delete Material**
   - Click delete button
   - Confirm deletion
   - Verify removed from list
   - Verify removed from Firebase

5. **Open Material**
   - Click open button
   - Verify opens in appropriate app
   - PDF → PDF viewer
   - Image → Gallery
   - Video → Video player
   - Link → Browser

## Performance Considerations

- **Lazy Loading**: Materials loaded only when screen opened
- **Caching**: Cloudinary provides CDN caching
- **Async Upload**: Upload happens in background coroutine
- **Progress Indicator**: User sees upload progress
- **Error Recovery**: Failed uploads can be retried

## Accessibility

- All icons have content descriptions
- Buttons are labeled clearly
- Error messages are descriptive
- Touch targets are appropriately sized
- Color is not the only indicator (icons + text)

