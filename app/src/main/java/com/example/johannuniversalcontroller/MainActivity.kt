package com.example.johannuniversalcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.johannuniversalcontroller.ui.theme.JohannUniversalControllerTheme
val main = R.layout.main_layout
val BLname: String = "Johann 1.0"
val ascendBut = R.id.Ascend
val hoverBut = R.id.Hover
val descendBut = R.id.Descend
val AltitudeSeek = R.id.altitude
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(main)
        controlling()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun controlling(){
        if (hoverBut == 1){
            print("HOVERING")
        }else if (ascendBut == 1){
            print("ASCENDING")
        }else if (descendBut == 1){
            print("DECENCING")
        }

        }
    }


