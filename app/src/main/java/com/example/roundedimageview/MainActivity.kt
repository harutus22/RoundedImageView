package com.example.roundedimageview

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.drawToBitmap
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        image.clipToOutline = true
        image.setOnClickListener {
            saveImage()
        }
        roundCorners()
    }

    private fun roundCorners() {
        val mBitmap = BitmapFactory.decodeResource(resources, R.drawable.econd)
        val cornerRadius = 100.0f

        val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
            resources,
            mBitmap
        )

        roundedBitmapDrawable.cornerRadius = cornerRadius
        roundedBitmapDrawable.setAntiAlias(true)
        image.setImageDrawable(roundedBitmapDrawable)
    }

    private fun saveImage() {
        val finalBitmap = image.drawToBitmap()
        runWithPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, UUID.randomUUID().toString())
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Thermal")
                }

                val uri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                resolver.openOutputStream(uri!!)?.use {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.flush()
                    it.close()
                }
            } else {
                saveImage(this, finalBitmap)
            }
            Toast.makeText(this, "Thermal Photo Saved", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun saveImage(context: Context, bitmap: Bitmap?): String {

        if (bitmap == null)
            return ""

        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)


        val qrImageDirectory =
            File(Environment.getExternalStorageDirectory().absolutePath + "/Thermal")
        if (!qrImageDirectory.exists()) {
            qrImageDirectory.mkdirs()
        }

        try {
            val f = File(
                qrImageDirectory, Calendar.getInstance()
                    .timeInMillis.toString() + ".png"
            )
            if (!f.exists()) {
                f.createNewFile()
            }

            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(
                context,
                arrayOf(f.path),
                arrayOf("image/png"), null
            )


            val contentUri = Uri.fromFile(File(f.absolutePath))
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = contentUri
            application.sendBroadcast(mediaScanIntent)

            fo.close()
            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        return ""
    }
}
