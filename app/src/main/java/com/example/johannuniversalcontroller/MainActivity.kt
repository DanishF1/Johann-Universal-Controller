package com.example.johannuniversalcontroller

import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val main = R.layout.main_layout
const val BLname: String = "Johann 1.0"
var isButtonActive: Boolean = false
var initializing: Boolean = false
var altitudeValue: Float = 0.0f
var millis1: Long = 0L
var breakout1: Long = 0L
var switchsJob1: Job? = null
var switchsJob2: Job? = null
var switchsJob3: Job? = null
var BLE: Job? = null
var connected: Boolean = false

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        initializing = true
        controlling()
        altitude()
    }

    private fun altitude() {
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)
        val calc: TextView = findViewById(R.id.percentage)
        var forcalc: Int = 0
        altitudeSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // HANYA MENGAMBIL NILAI. Jangan pernah taruh lifecycleScope.launch(while..) di sini!
                if(fromUser && !isButtonActive) {
                    altitudeValue = (progress + 1).toFloat()
                    forcalc = progress
                    forcalc.toInt()
                    calc.setText("$forcalc%")
                    Log.d("SeekBar $BLname", "Altitude: $altitudeValue")
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun controlling() {
        val ascendBut: Switch = findViewById(R.id.Ascend)
        val hoverBut: Switch = findViewById(R.id.Hover)
        val descendBut: Switch = findViewById(R.id.Descend)
        val ConnectToBle: Switch = findViewById(R.id.ConnectBLE)
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)

        // Panggil Custom Toast mu di sini agar siap dipakai
        val customToast: TextView = findViewById(R.id.newtoast)
        customToast.alpha = 0f
        fun customToast(){
            customToast.animate().alpha(1f).setDuration(500).start()
            customToast.animate().alpha(1f).setDuration(500).cancel()


        }

        // ==========================================
        // LOGIKA HOVER
        // ==========================================
        hoverBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                breakout1 = System.currentTimeMillis()
                ascendBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false

                customToast.text = "Hovering!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(700)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()

                // LOGIKA PENGIRIMAN DATA (Di dalam IO Thread)
                switchsJob1 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Hover Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob1?.cancel()
                if (!ascendBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                }
            }
        }

        // ==========================================
        // LOGIKA ASCEND
        // ==========================================
        ascendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false

                customToast.text = "Ascending!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(700)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()

                switchsJob2 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Ascend Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob2?.cancel()
                if (!hoverBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                }
            }
        }

        // ==========================================
        // LOGIKA DESCEND
        // ==========================================
        descendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                ascendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false

                customToast.text = "Descending!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(700)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()

                switchsJob3 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Descend Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob3?.cancel()
                if (!hoverBut.isChecked && !ascendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                }
            }
        }

        // ==========================================
        // LOGIKA BLE
        // ==========================================
        ConnectToBle.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked && !connected){
                customToast.text = "Searching Johann's Bluetooth"
                customToast.animate().alpha(1f).setDuration(1500)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(1500)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()

                BLE = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive && !connected){
                        Log.d(BLname, "Searching BLE Connection...")
                        delay(500)
                    }
                }
            } else if (!isChecked && connected) {
                BLE?.cancel()
                customToast.text = "Already Connected to Johann"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(700)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()
                Log.d(BLname, "Connected")
            }else if(isChecked && connected){
                customToast.text = "Connected to Johann"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction(object : Runnable {
                        override fun run() {
                            customToast.animate().alpha(0f).setDuration(700)
                                .setInterpolator(AccelerateInterpolator()).start()
                        }
                    }).start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        initializing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        initializing = false
    }
}