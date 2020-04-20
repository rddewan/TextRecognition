package com.richarddewan.textrecognizer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.info
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Executors.newSingleThreadExecutor


class MainActivity : AppCompatActivity(), LifecycleOwner {

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_PICK_FILE = 1
        private const val RC_IMAGE_CAPTURE = 2
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
    // This is an array of all the permission specified in the manifest.
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private val logger = AnkoLogger("MainActivity")
    private var language: String = ""
    private var confidence: Float = 0f

    var image: FirebaseVisionImage? = null
    private lateinit var textRecognizer: FirebaseVisionTextRecognizer
    private lateinit var languageIdentifier: FirebaseLanguageIdentification
    private lateinit var viewFinder: TextureView
    private val executor = newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification

        viewFinder = view_finder

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        /*
        select image
         */
        btnFilePicker.setOnClickListener {

            Intent(Intent.ACTION_GET_CONTENT).run {
                type ="image/*"
                putExtra(Intent.EXTRA_LOCAL_ONLY,true)
                startActivityForResult(this, RC_PICK_FILE)
            }
        }

        /*
        capture image
         */
       /* btnCaptureImage.setOnClickListener {
            /*
            Notice that the startActivityForResult() method is protected by a condition that calls resolveActivity(),
             which returns the first activity component that can handle the intent.
             Performing this check is important because if you call startActivityForResult()
             using an intent that no app can handle, your app will crash.
             So as long as the result is not null, it's safe to use the intent.
             */
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.resolveActivity(packageManager)?.also {
                    startActivityForResult(intent, RC_IMAGE_CAPTURE)
                }
            }
        }*/
    }



    private fun getTextFromImage(fileURI: Uri?){
        image = fileURI?.let {
            FirebaseVisionImage.fromFilePath(this, it)
        }

        image?.let { it ->
            textRecognizer.processImage(it)
                .addOnSuccessListener {result->
                    lbText.text = result.text
                    //Extract text from blocks of recognized text
                    for (block in result.textBlocks){
                        val blockText  = block.text
                        val blockLine = block.lines
                        val blockConfidence = block.confidence
                        val cornerPoint = block.cornerPoints
                        val blockFrame  = block.boundingBox
                        val blockRecognizedLanguages = block.recognizedLanguages

                    }
                    /*
                    identify language
                     */
                    languageIdentifier(result.text)

                }
                .addOnFailureListener {
                    logger.info { it.localizedMessage }
                    alert{
                        title = "error"
                        message = it.message.toString()
                    }.show()
                }
        }

    }

    private fun getTextFromImageBitmap(bitmap: Bitmap){
        image = FirebaseVisionImage.fromBitmap(bitmap)

        image?.let { it ->
            textRecognizer.processImage(it)
                .addOnSuccessListener {result->
                    lbText.text = result.text
                    //Extract text from blocks of recognized text
                    for (block in result.textBlocks){
                        val blockText  = block.text
                        val blockLine = block.lines
                        val blockConfidence = block.confidence
                        val cornerPoint = block.cornerPoints
                        val blockFrame  = block.boundingBox
                        val blockRecognizedLanguages = block.recognizedLanguages

                    }
                    /*
                    identify language
                     */
                    languageIdentifier(result.text)

                }
                .addOnFailureListener {
                    logger.info { it.localizedMessage }
                    alert{
                        title = "error"
                        message = it.message.toString()
                    }.show()
                }
        }

    }

    private fun languageIdentifier(text: String){
        languageIdentifier.identifyPossibleLanguages(text)
            .addOnSuccessListener {
                for (identifiedLanguage  in it){
                    language = identifiedLanguage.languageCode
                    confidence = identifiedLanguage.confidence

                    txtLanguageCode.text = language
                    txtConfidence.text = confidence.toString()
                }
            }
            .addOnFailureListener {
                logger.info { it.localizedMessage }
                alert{
                    title = "error"
                    message = it.message.toString()
                }.show()

            }

    }

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        btnCaptureImage.setOnClickListener {
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Log.e("CameraXApp", msg, exc)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onImageSaved(file: File) {
                        //runOnUiThread { ivFile.setImageURI(Uri.fromFile(file)) }
                        getTextFromImage(Uri.fromFile(file))
                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Log.d("CameraXApp", msg)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }

        // Setup image analysis pipeline
        val rotationImageAnalyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(rotationImageAnalyzerConfig).apply {
            setAnalyzer(executor, RotationImageAnalyzer())
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview,imageCapture)

    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)

    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,it
        ) == PackageManager.PERMISSION_GRANTED
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_PICK_FILE){
            if (resultCode == Activity.RESULT_OK){
                //ivFile.setImageURI(data?.data)
                getTextFromImage(data?.data)
            }
        }
        else if (requestCode == RC_IMAGE_CAPTURE){
            if (resultCode == Activity.RESULT_OK){
                val imageBitmap = data?.extras?.get("data") as Bitmap
                //val image = data?.data
                //ivFile.setImageBitmap(imageBitmap)
                //getTextFromImage(image)

            }
        }
    }

    //calculate the rotation value for you, so you just need to convert the rotation to one of
    // ML Kit's ROTATION_ constants before calling FirebaseVisionImage.fromMediaImage():
    inner class RotationImageAnalyzer : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
            val mediaImage = imageProxy?.image
            val imageRotation = degreesToFirebaseRotation(degrees)
            if (mediaImage != null) {
                val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                // Pass image to an ML Kit Vision API
                textRecognizer.processImage(image)
                    .addOnSuccessListener {result->
                        lbText.text = result.text
                        //Extract text from blocks of recognized text
                        for (block in result.textBlocks){
                            val blockText  = block.text
                            val blockLine = block.lines
                            val blockConfidence = block.confidence
                            val cornerPoint = block.cornerPoints
                            val blockFrame  = block.boundingBox
                            val blockRecognizedLanguages = block.recognizedLanguages

                        }
                        /*
                        identify language
                         */
                        languageIdentifier(result.text)

                    }
                    .addOnFailureListener {
                        logger.info { it.localizedMessage }
                        alert{
                            title = "error"
                            message = it.message.toString()
                        }.show()
                    }
            }
        }
    }
}
