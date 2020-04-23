package com.richarddewan.textrecognizer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.richarddewan.textrecognizer.util.GeneralHelper
import kotlinx.android.synthetic.main.activity_credit_card.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.info
import java.util.regex.Pattern

class CreditCardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CreditCardActivity"
        private const val RC_IMAGE_CAPTURE = 2
        private const val NUMBER_REGEX = "[0-9]+"
    }

    var image: FirebaseVisionImage? = null
    private lateinit var textRecognizer: FirebaseVisionTextRecognizer
    private lateinit var languageIdentifier: FirebaseLanguageIdentification
    private val logger = AnkoLogger(TAG)
    private lateinit var pattern: Pattern

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_card)

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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTextFromImageBitmap(bitmap: Bitmap){
        image = FirebaseVisionImage.fromBitmap(bitmap)

        image?.let { it ->
            textRecognizer.processImage(it)
                .addOnSuccessListener {result->
                    var matcher = false

                    //Extract text from blocks of recognized text
                    for (block in result.textBlocks){
                        //remove space from block of text
                        val blockText  = block.text.replace(" ","")
                        //split block text if contain new line
                        val card = blockText.split("\n")
                        //loop card as it's a array after we split the text
                        for (data in card){
                            pattern = Pattern.compile(NUMBER_REGEX)

                            //we only want to match the card regex if data contains number only
                            if (pattern.matcher(data).matches()){
                                //match the card rex pattern
                                matcher = GeneralHelper.matchCreditCard(data)
                                if (matcher){
                                    //set card text
                                    txtCreditCard.setText(data)

                                    //get card vendor
                                    val vendor = GeneralHelper.getCreditCardVendor(data)
                                    txtCardType.text = vendor
                                    /*
                                    validate the card
                                     */
                                    val valid = GeneralHelper.checkValidCard(data)
                                    txtValidCard.text = valid.toString()
                                }
                            }
                            //extract expiry month and year from card
                            //not a perfect solution
                            else if (data.contains("/")){
                                val monthYear =  data.split("/")
                                if (pattern.matcher(monthYear[0]).matches()){
                                    txtExpiryMonth.setText(monthYear[0])
                                }
                                if (pattern.matcher(monthYear[1]).matches()){
                                    txtExpiryYear.setText(monthYear[1])
                                }
                            }
                            //break the loop
                            break

                        }
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

    }

    @RequiresApi(Build.VERSION_CODES.O)
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
