package com.example.studiora.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object CloudinaryHelper {
    private var isInitialized = false

    // Replace these with your Cloudinary credentials
    private const val CLOUD_NAME = "dlvgrrxd5"
    private const val API_KEY = "141658916226837"
    private const val API_SECRET = "Vtgt7CNYzzD0Epymz7AyTqsxLrE"

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                val config = mapOf(
                    "cloud_name" to CLOUD_NAME,
                    "api_key" to API_KEY,
                    "api_secret" to API_SECRET,
                    "secure" to true
                )
                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        resourceType: String = "auto" // auto, image, video, raw
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val file = getFileFromUri(context, uri)

            MediaManager.get().upload(file.absolutePath)
                .option("resource_type", resourceType)
                .option("folder", "studiora")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Upload progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            continuation.resume(Result.success(url))
                        } else {
                            continuation.resume(Result.failure(Exception("Failed to get URL from upload result")))
                        }
                        // Clean up temporary file
                        file.delete()
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                        // Clean up temporary file
                        file.delete()
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception("Upload rescheduled: ${error.description}")))
                        // Clean up temporary file
                        file.delete()
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "upload_${System.currentTimeMillis()}"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}


