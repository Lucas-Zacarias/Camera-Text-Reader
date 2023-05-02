package com.cameratextreader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cameratextreader.databinding.ActivityScannerBinding
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private lateinit var photo: ImageView
    private lateinit var textRecognized: TextView
    private lateinit var reScanBtn: MaterialButton
    private lateinit var listenTextBtn: MaterialButton
    private var imageUri: Uri? = null

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    private lateinit var textRecognizer: TextRecognizer
    private lateinit var textToSpeech: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        //To handle when user do back gesture
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initArraysPermissions()
        checkImageInput(intent.extras)
        getViews()
        setBtnReTakePhotoListener()
        setTextRecognizer()
        setTextToSpeech()
        setBtnReadTextRecognizedListener()
    }

    private fun checkImageInput(extras: Bundle?) {
        if(extras != null){
            if(extras.getInt("InputImage") == 1){
                verifyCameraPermissions()
            }else{
                verifyStoragePermissions()
            }
        }
    }

    private fun getViews() {
        photo = binding.ivPhoto
        textRecognized = binding.tvTextRecognized
        reScanBtn = binding.btnReScan
        listenTextBtn = binding.btnReadText
    }

    private fun initArraysPermissions(){
        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun verifyCameraPermissions(){
        if(checkCameraPermissions()){
            takePhoto()
        }else{
            requestCameraPermissions()
        }
    }

    private fun verifyStoragePermissions(){
        if(checkStoragePermissions()){
            pickImageFromGallery()
        }else{
            requestStoragePermissions()
        }
    }

    private fun setBtnReTakePhotoListener(){
        reScanBtn.setOnClickListener {
            verifyCameraPermissions()
            cleanPreviousRecognizedText()
        }
    }

    private fun cleanPreviousRecognizedText() {
        textRecognized.text = ""
    }

    private fun setTextRecognizer(){
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private fun recognizeTextFromImage(){
        if(imageUri != null){
            try {
                val inputImage = InputImage.fromFilePath(this, imageUri!!)

                val textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        val recognizedText = text.text
                        textRecognized.text = recognizedText
                    }
                    .addOnFailureListener{
                        //no se pudo reconocer texto en la foto
                        textRecognized.text = "No hay texto en la foto mamaguevo"
                    }
            }catch (e: Exception){
                textRecognized.text = "No hay texto en la foto mamaguevo"
                //no se pudo cargar la imagen
            }
        }
    }

    private fun setTextToSpeech(){
        val language = Locale("es", "US")
        textToSpeech = TextToSpeech(this) {
            if (it != TextToSpeech.ERROR) {
                textToSpeech.language = language
            }
        }
    }

    private fun setBtnReadTextRecognizedListener(){
        listenTextBtn.setOnClickListener {
            if(textRecognized.text.isNotEmpty()){
                textToSpeech.speak(textRecognized.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun takePhoto(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Lexi Photo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                photo.setImageURI(imageUri)
                recognizeTextFromImage()
            }else{
                //When i close de camera, finish the activity
                finish()
            }
        }

    private fun checkCameraPermissions(): Boolean{
        val cameraResult = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult
    }

    private fun requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data
                photo.setImageURI(imageUri)
                recognizeTextFromImage()
            }else{
                //No one image picked from gallery
                finish()
            }
        }

    private fun checkStoragePermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //handle permission(s) results
        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted) {
                        takePhoto()
                    } else {
                        //No camera and storage permissions granted
                        finish()
                    }

                }
            }
            STORAGE_REQUEST_CODE -> {
                val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if(storageAccepted){
                    //Open gallery
                    pickImageFromGallery()
                }else{
                    //No camera and storage permissions granted
                    finish()
                }
            }
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback = object: OnBackPressedCallback(true){
        override fun handleOnBackPressed() {
            finish()
        }

    }

    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 200
    }
}