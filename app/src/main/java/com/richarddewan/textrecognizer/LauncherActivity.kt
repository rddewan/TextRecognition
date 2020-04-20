package com.richarddewan.textrecognizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_launcher.*
import org.jetbrains.anko.startActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        btnFromFilePicker.setOnClickListener {
            startActivity<FromImageActivity>()
        }

        btnFromCamera.setOnClickListener {
            startActivity<FromCameraActivity>()
        }

        btnLiveCamera.setOnClickListener {
            startActivity<CameraxAnalysisActivity>()
        }
    }
}
