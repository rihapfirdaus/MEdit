package com.rihap.medit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rihap.medit.databinding.ActivityEditImageBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditImageActivity : AppCompatActivity() {
    private var currentImageUri: Uri? = null
    private var currentEditor: String? = null

    private lateinit var currentBitmap: Bitmap
    private lateinit var originalBitmap: Bitmap

    private val orientationOptions = arrayOf("horizontal", "vertical")
    private lateinit var flipOrientation: String

    private val binding: ActivityEditImageBinding by lazy {
        ActivityEditImageBinding.inflate(layoutInflater)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        currentImageUri = intent.getParcelableExtra(EXTRA_IMAGE)
        currentEditor = intent.getStringExtra(EXTRA_EDITOR)

        val inputStream = currentImageUri?.let { contentResolver.openInputStream(it) }
        originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        with(binding) {
            if (currentImageUri != null && currentEditor != null) {
                showImage()
                showInput()
            }

            btnEditPicture.setOnClickListener {
                when (currentEditor) {
                    RESIZE -> {
                        val width = etImageResizeWidth.text.toString()
                        val height = etImageResizeHeight.text.toString()

                        if (height.isNotEmpty() && width.isNotEmpty()) {
                            val resizedImage =
                                resizeImage(originalBitmap, width.toInt(), height.toInt())

                            hideInput()
                            showImage(resizedImage)
                        } else showToast(getString(R.string.toast_complete_form_first))
                    }

                    FLIP -> {
                        val orientation: String = flipOrientation
                        val flipedImage = flipImage(originalBitmap, orientation == "horizontal")

                        hideInput()
                        showImage(flipedImage)
                    }

                    ROTATE -> {
                        val degree = etImageRotateDegree.text.toString()

                        if (degree.isNotEmpty()) {
                            val rotatedImage = rotateImage(originalBitmap, degree.toFloat())

                            hideInput()
                            showImage(rotatedImage)
                        } else showToast(getString(R.string.toast_complete_form_first))
                    }
                }
            }

            btnBack.setOnClickListener {
                finish()
            }

            btnSavePicture.setOnClickListener {
                saveImage(currentBitmap)
            }
        }
    }

    private fun resizeImage(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun rotateImage(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipImage(bitmap: Bitmap, orientation: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale(if (orientation) -1f else 1f, if (!orientation) -1f else 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun showImage() {
        binding.ivImagePreview.setImageURI(currentImageUri)
    }

    private fun showImage(bitmap: Bitmap) {
        currentBitmap = bitmap

        binding.ivImagePreview.setImageBitmap(bitmap)
    }

    private fun hideInput() {
        with(binding) {
            when (currentEditor) {
                RESIZE -> inputResize.visibility = View.GONE
                ROTATE -> inputRotate.visibility = View.GONE
                FLIP -> inputFlip.visibility = View.GONE
            }
            btnEditPicture.visibility = View.GONE
            btnSavePicture.visibility = View.VISIBLE
        }
    }

    private fun showInput() {
        val spinnerAdapter =
            ArrayAdapter(this@EditImageActivity, android.R.layout.simple_spinner_item, orientationOptions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        with(binding) {
            when (currentEditor) {
                RESIZE -> inputResize.visibility = View.VISIBLE
                ROTATE -> inputRotate.visibility = View.VISIBLE
                FLIP -> {
                    inputFlip.visibility = View.VISIBLE
                    spnrImageFlip.adapter = spinnerAdapter
                    spnrImageFlip.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                flipOrientation = orientationOptions[position]
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                flipOrientation = orientationOptions[0]
                            }
                        }
                }
            }
            btnEditPicture.visibility = View.VISIBLE
            btnSavePicture.visibility = View.GONE
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folderPath = File(storageDir, "MEdit")
        folderPath.mkdirs()

        val imageFile = File(folderPath, "IMG-$currentEditor-${System.currentTimeMillis()}.jpg")

        try {
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            showToast(getString(R.string.toast_file_saved))

            MediaScannerConnection.scanFile(
                this@EditImageActivity,
                arrayOf(imageFile.absolutePath),
                null,
                null
            )
        } catch (e: IOException) {
            showToast(getString(R.string.toast_file_not_saved))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMAGE = "extra_image"
        const val EXTRA_EDITOR = "extra_editor"
        const val RESIZE = "resize"
        const val FLIP = "flip"
        const val ROTATE = "rotate"
    }
}