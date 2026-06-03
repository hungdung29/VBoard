package com.vboard.aac.ui.edit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vboard.aac.databinding.ActivityCropBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private var sourceUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriStr = intent.getStringExtra("image_uri")
        if (uriStr == null) {
            com.vboard.aac.ui.utils.CustomToast.show(this, "Không tìm thấy ảnh")
            finish()
            return
        }
        sourceUri = Uri.parse(uriStr)

        setupListeners()
        loadImage()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.btnCrop.setOnClickListener {
            cropAndSave()
        }
    }

    private fun loadImage() {
        val uri = sourceUri ?: return
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmapFromUri(this@CropActivity, uri)
            }
            if (bitmap != null) {
                binding.cropImageView.setBitmap(bitmap)
            } else {
                com.vboard.aac.ui.utils.CustomToast.show(this@CropActivity, "Không thể tải ảnh")
                finish()
            }
        }
    }

    private fun cropAndSave() {
        lifecycleScope.launch {
            val croppedBitmap = binding.cropImageView.getCroppedBitmap(512)
            if (croppedBitmap == null) {
                com.vboard.aac.ui.utils.CustomToast.show(this@CropActivity, "Không thể cắt ảnh")
                return@launch
            }

            val savedFile = withContext(Dispatchers.IO) {
                saveBitmapToInternalStorage(this@CropActivity, croppedBitmap)
            }

            if (savedFile != null) {
                val resultIntent = Intent().apply {
                    putExtra("cropped_uri", Uri.fromFile(savedFile).toString())
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                com.vboard.aac.ui.utils.CustomToast.show(this@CropActivity, "Không thể lưu ảnh")
            }
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var sampleSize = 1
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (max(srcWidth, srcHeight) > maxSide) {
                val longerSide = max(srcWidth, srcHeight)
                while (longerSide / sampleSize > maxSide) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val nextStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(nextStream, null, decodeOptions)
            nextStream.close()

            if (bitmap != null) {
                bitmap = rotateBitmapIfRequired(context, bitmap, uri)
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmapIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): File? {
        return try {
            val dir = File(context.filesDir, "vocab_images").apply { mkdirs() }
            val file = File(dir, "card_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
