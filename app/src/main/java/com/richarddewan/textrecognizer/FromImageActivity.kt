package com.richarddewan.textrecognizer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.activity_from_image.*
import kotlinx.android.synthetic.main.activity_from_image.btnFilePicker
import kotlinx.android.synthetic.main.activity_from_image.lbText
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.info


class FromImageActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FromImageActivity"
        private const val RC_PICK_FILE = 1
    }

    var image: FirebaseVisionImage? = null
    private lateinit var textRecognizer: FirebaseVisionTextRecognizer
    private lateinit var languageIdentifier: FirebaseLanguageIdentification
    private val logger = AnkoLogger(TAG)
    private var language: String = ""
    private var confidence: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_from_image)

        //initialize FirebaseVision
        textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        //initialize FirebaseNaturalLanguage
        languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification


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

        sw_image.setOnClickListener {
            ivFromFile.visibility = if (sw_image.isChecked) View.VISIBLE else View.GONE
        }
    }

    /*
   Recognize and extract text from images
    */
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

        if (requestCode == RC_PICK_FILE){
            if (resultCode == Activity.RESULT_OK){
                ivFromFile.setImageURI(data?.data)
                getTextFromImage(data?.data)
            }
        }

    }

}
