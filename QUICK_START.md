# 🚀 Quick Setup Guide - Course Material Management

## ⚡ 4 Steps to Get Started

### Step 1️⃣: Get Cloudinary Credentials (2 minutes)
1. Go to https://cloudinary.com
2. Sign up for FREE account
3. From Dashboard, copy:
   - Cloud Name
   - API Key
   - API Secret

### Step 2️⃣: Configure App (1 minute)
1. Open: `app/src/main/java/com/example/studiora/util/CloudinaryHelper.kt`
2. Replace these lines (around line 18-20):
   ```kotlin
   private const val CLOUD_NAME = "YOUR_CLOUD_NAME"
   private const val API_KEY = "YOUR_API_KEY"
   private const val API_SECRET = "YOUR_API_SECRET"
   ```
3. Paste your actual credentials
4. Save file

### Step 3️⃣: Sync Project (2 minutes)
1. In Android Studio, click **"Sync Now"** banner
2. Or: File → Sync Project with Gradle Files
3. Wait for download to complete
4. You'll see "BUILD SUCCESSFUL"

### Step 4️⃣: Update Firebase Rules (1 minute)
1. Go to Firebase Console → Your Project
2. Realtime Database → Rules tab
3. Add this:
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
4. Click **Publish**

---

## ✅ Done! Now Test It:

1. **Run the app** (Shift+F10)
2. **Login as admin**
3. Go to **"Manage Courses"**
4. Click **"Manage Materials"** on any course
5. Click the **"+"** button
6. **Upload a file** or **add a link**
7. See it appear instantly! 🎉

---

## 📱 Quick Navigation Map

```
Admin Dashboard
    ↓
Manage Courses
    ↓
[Manage Materials] button on any course
    ↓
Course Materials Screen
    ↓
[+] button
    ↓
Upload Dialog
```

---

## 🎯 File Types You Can Upload

- 📄 **PDF** - Notes, assignments, documents
- 🖼️ **Images** - Photos, diagrams, screenshots
- ▶️ **Videos** - Tutorials, lectures, demos
- 🔗 **Links** - YouTube, websites, resources

---

## 🆘 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| "Unresolved reference" errors | Sync Gradle (Step 3) |
| Upload fails | Check credentials (Step 2) |
| Can't save materials | Update Firebase rules (Step 4) |
| Red errors in code | Sync Gradle & Rebuild |

---

## 💡 Tips

✅ **Free Tier Limits:**
- 25 GB storage
- 25 GB bandwidth/month
- Perfect for educational use!

✅ **File Size:**
- Max 10 MB per file (free tier)
- For larger files, upgrade Cloudinary plan

✅ **Supported Formats:**
- PDF, JPG, PNG, GIF, MP4, MOV, AVI
- Most common formats work!

---

## 🔒 Security Reminder

⚠️ **NEVER** commit credentials to Git!

For production apps:
1. Use environment variables
2. Store in `local.properties`
3. Add to `.gitignore`

---

## 📚 Full Documentation

For detailed info, check these files:
- `CLOUDINARY_SETUP.md` - Detailed setup
- `README_COURSE_MANAGEMENT.md` - Complete guide
- `IMPLEMENTATION_SUMMARY.md` - Technical details
- `NAVIGATION_FLOW.md` - Visual guide

---

## ✨ That's It!

**Total setup time: ~6 minutes**

You now have a fully functional course material management system with cloud storage! 🎉

**Start uploading materials now!** 📚🚀

