package com.rihap.medit

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.rihap.medit.databinding.ActivityEditAudioBinding
import java.io.File
import java.io.IOException

class EditAudioActivity : AppCompatActivity() {
    private var currentAudioUri: Uri? = null
    private lateinit var currentAudioPath: String
    private lateinit var currentCachePath: String

    private val bitrateOptions = arrayOf("32k", "64k", "128k", "192k", "256k", "320k")
    private lateinit var choosedBitrate: String

    private val binding: ActivityEditAudioBinding by lazy {
        ActivityEditAudioBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        currentAudioUri = intent.getParcelableExtra(EXTRA_AUDIO)
        currentAudioPath = Util.getPathFromUri(this@EditAudioActivity, currentAudioUri!!).toString()

        showAudioInputInfo(currentAudioPath)
        showInput()

        with(binding) {
            btnCompressAudio.setOnClickListener {
                if (choosedBitrate.isEmpty()) showToast(getString(R.string.toast_pick_bitrate_first)) else compressAudio(
                    currentAudioPath,
                    choosedBitrate
                )
            }

            btnSaveAudio.setOnClickListener {
                saveAudio(currentCachePath)
            }

            btnBack.setOnClickListener {
                finish()
            }
        }
    }

    private fun showInput() {
        val spinnerAdapter = ArrayAdapter(
            this@EditAudioActivity,
            android.R.layout.simple_spinner_item,
            bitrateOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_item)
        }

        with(binding) {
            llAudioCompressorInput.visibility = View.VISIBLE
            btnCompressAudio.visibility = View.VISIBLE
            btnSaveAudio.visibility = View.GONE

            llOutputInfo.visibility = View.GONE
            spnrAudioCompressor.adapter = spinnerAdapter
            spnrAudioCompressor.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        choosedBitrate = bitrateOptions[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        choosedBitrate = bitrateOptions[2]
                    }

                }
        }
    }

    private fun showAudioInputInfo(inputAudioPath: String) {
        val inputPath = File(inputAudioPath)
        val name = inputPath.name
        val size = inputPath.length().toString() + " byte"

        binding.tvInputInfo.text = getString(R.string.tv_input_audio_info, name, size)
    }

    private fun showAudioOutputInfo(outputPath: File) {
        val size = outputPath.length().toString() + " byte"
        val bitrate = choosedBitrate

        binding.tvOutputInfo.text = getString(R.string.tv_output_audio_info, size, bitrate)
    }

    private fun hideInput() {
        with(binding) {
            llAudioCompressorInput.visibility = View.GONE
            btnCompressAudio.visibility = View.GONE
            btnSaveAudio.visibility = View.VISIBLE
            llOutputInfo.visibility = View.VISIBLE
        }
    }

    @Throws(IOException::class)
    private fun compressAudio(inputPath: String, bitrate: String) {
        val cacheDir = cacheDir
        val tempFile = File.createTempFile("temp_audio", ".mp3", cacheDir)

        if (tempFile.exists()) {
            tempFile.delete()
        }

        val ffmpegCommand = "-i $inputPath -c:a libmp3lame -b:a $bitrate ${tempFile.absolutePath}"

        showToast(getString(R.string.toast_begin_compression))
        try {
            FFmpegKit.execute(ffmpegCommand)
            hideInput()
            showAudioOutputInfo(tempFile)
            currentCachePath = tempFile.absolutePath
            showToast(getString(R.string.toast_compression_succes))
        } catch (e: Exception) {
            showToast(getString(R.string.toast_compression_failed))
        }
    }

    @SuppressLint("Recycle")
    private fun saveAudio(cachePath: String) {
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val folderPath = File(storageDir, "MEdit")
        folderPath.mkdirs()

        val file = File(cachePath)
        val outputPath = File(folderPath, "AUDIO-COMPRESSED-${System.currentTimeMillis()}.mp3")

        try {
            file.copyTo(outputPath)
            showToast(getString(R.string.toast_audio_save_success))
        } catch (e: Exception) {
            showToast(getString(R.string.toast_audio_save_failed))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@EditAudioActivity, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_AUDIO = "extra_audio"
    }
}