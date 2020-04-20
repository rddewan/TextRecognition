package com.richarddewan.textrecognizer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.activity_from_camera.*
import kotlinx.android.synthetic.main.activity_from_camera.btnCaptureImage
import kotlinx.android.synthetic.main.activity_from_camera.lbText
import kotlinx.android.synthetic.main.activity_from_camera.sw_image
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.info


class FromCameraActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FromCameraActivity"
        private const val RC_IMAGE_CAPTURE = 2
    }

    var image: FirebaseVisionImage? = null
    private lateinit var textRecognizer: FirebaseVisionTextRecognizer
    private lateinit var languageIdentifier: FirebaseLanguageIdentification
    private val logger = AnkoLogger(TAG)
    private var language: String = ""
    private var confidence: Float = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_from_camera)

        //initialize FirebaseVision
        textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        //initialize FirebaseNaturalLanguage
        languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification

        sw_image.setOnClickListener {
            ivFromCamera.visibility = if (sw_image.isChecked) View.VISIBLE else View.GONE
        }

        /*
       capture image
        */
       btnCaptureImage.setOnClickListener {
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
         }
    }

    /*
    Recognize and extract text from images
     */
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

    /*
    Detect the language of tex
     */
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_IMAGE_CAPTURE){
            if (resultCode == Activity.RESULT_OK){
                val imageBitmap = data?.extras?.get("data") as Bitmap
                //set image to image view
                ivFromCamera.setImageBitmap(imageBitmap)
                //Detect the language of text
                getTextFromImageBitmap(imageBitmap)

            }
        }
    }
}
