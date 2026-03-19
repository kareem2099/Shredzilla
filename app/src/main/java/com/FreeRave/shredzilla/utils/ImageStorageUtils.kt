package com.FreeRave.shredzilla.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider // For getUriForFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageStorageUtils {

    private const val TAG = "ImageStorageUtils"
    private const val PERMANENT_IMAGE_SUBDIR = "images" // Subdirectory for saved profile images
    private const val TEMP_CAMERA_IMAGE_SUBDIR = "images" // Subdirectory in cache for temporary camera photos (matches file_paths.xml)


    // Creates a temporary file in the cache directory to store the image taken by the camera
    fun createImageFile(context: Context, fileNamePrefix: String = "temp_cam_"): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            // Use cacheDir for temporary files from camera, as defined in file_paths.xml
            val storageDir = File(context.cacheDir, TEMP_CAMERA_IMAGE_SUBDIR) 
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile(
                "${fileNamePrefix}${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            )
        } catch (ex: IOException) {
            Log.e(TAG, "Error creating temp image file", ex)
            null
        }
    }

    // Gets a content URI for a file, required by the TakePicture contract
    fun getUriForFile(context: Context, file: File): Uri {
        // Ensure your authority matches what's in AndroidManifest.xml
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // Saves an image from a source URI (gallery or camera temp file) to permanent app internal storage
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri, fileNamePrefix: String = "profile_img_"): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            
            val permanentImageDir = File(context.filesDir, PERMANENT_IMAGE_SUBDIR)
            if (!permanentImageDir.exists()) {
                permanentImageDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val destinationFile = File(permanentImageDir, "$fileNamePrefix${timeStamp}.jpg")

            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            Log.d(TAG, "Image saved to: ${destinationFile.absolutePath}")
            return destinationFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image to internal storage from URI: $sourceUri", e)
            return null
        }
    }

    fun loadImageFromPath(filePath: String): Bitmap? {
        return try {
            val imageFile = File(filePath)
            if (imageFile.exists()) {
                BitmapFactory.decodeFile(imageFile.absolutePath)
            } else {
                Log.w(TAG, "Image file not found at path: $filePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from path: $filePath", e)
            null
        }
    }

    fun deleteImageFromInternalStorage(filePath: String): Boolean {
        return try {
            val imageFile = File(filePath)
            if (imageFile.exists()) {
                val deleted = imageFile.delete()
                if (deleted) {
                    Log.d(TAG, "Image deleted: $filePath")
                } else {
                    Log.w(TAG, "Failed to delete image: $filePath")
                }
                deleted
            } else {
                Log.w(TAG, "Image not found for deletion: $filePath")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: $filePath", e)
            false
        }
    }
}
