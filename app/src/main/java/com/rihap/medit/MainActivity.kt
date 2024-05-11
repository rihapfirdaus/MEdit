package com.rihap.medit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rihap.medit.databinding.ActivityMainBinding

@Suppress("DEPRECATION", "DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {
    private var currentImageUri: Uri? = null
    private var currentEditor: String? = null

    private val PICK_AUDIO_REQUEST = 1

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            if (it != null) {
                currentImageUri = it
                showImage()
            } else {
                showToast(getString(R.string.toast_failed_pick_image))
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            tvImageResize.setOnClickListener {
                currentEditor = EditImageActivity.RESIZE
                startGallery()
            }
            tvImageFlip.setOnClickListener {
                currentEditor = EditImageActivity.FLIP
                startGallery()
            }
            tvImageRotate.setOnClickListener {
                currentEditor = EditImageActivity.ROTATE
                startGallery()
            }
            tvAudioCompress.setOnClickListener {
                startAudioPicker()
            }
        }
    }

    private fun startGallery() {
        showToast(getString(R.string.toast_pick_image_first))
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showImage() {
        Intent(this@MainActivity, EditImageActivity::class.java).apply {
            putExtra(EditImageActivity.EXTRA_IMAGE, currentImageUri)
            putExtra(EditImageActivity.EXTRA_EDITOR, currentEditor)
        }.also {
            startActivity(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startAudioPicker() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                STORAGE_PERMISSION_CODE
            )
        } else {
            showToast(getString(R.string.toast_pick_audio_first))
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
            }.also {
                startActivityForResult(it, PICK_AUDIO_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return
            } else {
                showToast(getString(R.string.toast_ask_for_permission))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode === PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let {
                Intent(this@MainActivity, EditAudioActivity::class.java).apply {
                    putExtra(EditAudioActivity.EXTRA_AUDIO, it)
                }.also {
                    startActivity(it)
                }
            }
        } else if (requestCode === PICK_AUDIO_REQUEST) {
            showToast(getString(R.string.toast_failed_pick_audio))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
    }
}