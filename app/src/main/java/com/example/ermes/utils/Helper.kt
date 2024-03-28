package com.example.ermes.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.sunmi.facelib.SunmiFaceFeature
import com.sunmi.facelib.SunmiFaceImage
import com.sunmi.facelib.SunmiFaceImageFeatures
import com.sunmi.facelib.SunmiFaceLib
import com.sunmi.facelib.SunmiFaceSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

object Helper {


    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE
    )

    fun Context.checkPermissions(listener: PermissionManagerListener, permissions:Array<String>) {
        val permissionManager = PermissionsHandler(this,listener)
        permissionManager.setMultiplePermission(permissions)
    }


    fun createImageFile(context: Activity,fileName:String): File? {

        val IMAGE_DATE_FORMAT = "yyyyMMddHHmmssSSS"
        var picFile: File?=null

        try {
            val fileName1 = "$fileName.jpg"

            val tmpDir: File =
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())

            if (!tmpDir.exists()) {
                tmpDir.mkdirs()
            }

            picFile = File(tmpDir, fileName1)
            // picFile.delete()


            return picFile
        } catch (e: Exception) {
            e.printStackTrace()

        }
        return picFile
    }

    fun getFileProvider(activity: Activity, file: File): Uri {
        return FileProvider.getUriForFile(
            activity,
            "com.example.ermes.provider", //(use your app signature + ".provider" )
            file
        )
    }

    fun getDate(): String {
        return  SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    fun getTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }


    fun getTodayUniqueNumber(): String {
        return SimpleDateFormat("MMddyyyy", Locale.getDefault()).format(Date())
    }

    fun getResizedBitmap(image: Bitmap, newWidth:Int, newHeight:Int): Bitmap? {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = newWidth
            height = (width / bitmapRatio).toInt()
        } else {
            height = newHeight
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
    }

    fun getPixelsBGR(image: Bitmap): ByteArray {
        // Calculate how many bytes in the image
        val bytes = image.byteCount
        val buffer = ByteBuffer.allocate(bytes) // Create a new buffer
        image.copyPixelsToBuffer(buffer) // Move byte data to buffer
        val temp = buffer.array() // Get underlying array containing data
        val pixels = ByteArray(temp.size / 4 * 3) // Assign space to BGR

        // Recomposition of pixels
        for (i in 0 until temp.size / 4) {
            pixels[i * 3] = temp[i * 4 + 2] //B
            pixels[i * 3 + 1] = temp[i * 4 + 1] //G
            pixels[i * 3 + 2] = temp[i * 4] //R
        }
        return pixels
    }


    fun ByteArray.toBase64(): String =
        Base64.getEncoder().encodeToString(this).toString()


    fun String.toByteArray(): ByteArray =
        this.encodeToByteArray()


    fun isLateForCheckIn(startTime: String): Boolean {
        val currentTime = LocalTime.now()
        val start = LocalTime.parse(startTime.padStart(5, '0')) // Add leading zero if needed
        val lateThreshold = start.plusMinutes(15)

        // Late for check-in if current time is after the lateThreshold.
        return currentTime.isAfter(lateThreshold)
    }

    fun isCheckIn(startTime: String): Boolean {
        val currentTime = LocalTime.now()
        val start = LocalTime.parse(startTime.padStart(5, '0')) // Add leading zero if needed
        val lateThreshold = start.plusHours(1)

        // Late for check-in if current time is after the lateThreshold.
        return currentTime.isBefore(lateThreshold)
    }

    fun isEarlyForCheckOut(endTime: String): Boolean {
        val currentTime = LocalTime.now()
        val end = LocalTime.parse(endTime.padStart(5, '0')) // Add leading zero if needed
        val earlyThreshold = end.minusMinutes(5)

        // Early for check-out if current time is before the earlyThreshold.
        return currentTime.isBefore(earlyThreshold)
    }

    fun processImage(file: String): Deferred<SunmiFaceFeature?> {

        return CoroutineScope(Dispatchers.Default).async {
            var bitmap = BitmapFactory.decodeFile(file)
            if(bitmap!=null) {
                // bitmap = FixingImageRotation.rotateImageIfRequired(bitmap, file.path)
                val srcData: ByteArray = getPixelsBGR(bitmap)
                val image = SunmiFaceImage(srcData, bitmap.getHeight(), bitmap.getWidth(), 1)
                val features = SunmiFaceImageFeatures()
                SunmiFaceSDK.getImageFeatures(image, features)
                val feature_ary = features.features
                SunmiFaceSDK.releaseImageFeatures(features)
                return@async SunmiFaceLib.SunmiFaceFeatureArrayGetItem(feature_ary, 0)
            }else{
                return@async null
            }
        }


    }


    fun isCurrentDayBetween(startDay: String, endDay: String): Boolean {
        val currentDayOfWeek = LocalDate.now().dayOfWeek
        val startDayOfWeek = DayOfWeek.valueOf(startDay.uppercase())
        val endDayOfWeek = DayOfWeek.valueOf(endDay.uppercase())

        val currentDayIndex = currentDayOfWeek.value
        val startDayIndex = startDayOfWeek.value
        val endDayIndex = endDayOfWeek.value

        return if (startDayIndex <= endDayIndex) {
            currentDayIndex in startDayIndex..endDayIndex
        } else {
            currentDayIndex >= startDayIndex || currentDayIndex <= endDayIndex
        }
    }
}