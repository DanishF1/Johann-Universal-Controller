package com.example.johannuniversalcontroller

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers

val main = R.layout.main_layout
val BLname: String = "Johann 1.0"
var isButtonActive: Boolean = false
var initializing: Boolean = false
var altitudeValue: Float = 0.0f

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

        altitudeSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // HANYA MENGAMBIL NILAI. Jangan pernah taruh lifecycleScope.launch(while..) di sini!
                if(fromUser && !isButtonActive) {
                    altitudeValue = (progress + 1).toFloat()
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
                ascendBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false

                // MANIPULASI UI (Harus di luar IO Thread)
                customToast.text = "Hovering!"
                customToast.animate().alpha(1f).setDuration(300).start() // Fade-in animasi selama 0.3 detik

                // LOGIKA PENGIRIMAN DATA (Di dalam IO Thread)
                switchsJob1 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Hover Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob1?.cancel()

                // Hilangkan Toast secara perlahan
                customToast.animate().alpha(0f).setDuration(300).start()

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
                customToast.animate().alpha(1f).setDuration(300).start()

                switchsJob2 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Ascend Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob2?.cancel()
                customToast.animate().alpha(0f).setDuration(300).start()

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
                customToast.animate().alpha(1f).setDuration(300).start()

                switchsJob3 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Descend Button is Active")
                        delay(100)
                    }
                }
            } else {
                switchsJob3?.cancel()
                customToast.animate().alpha(0f).setDuration(300).start()

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
                customToast.text = "Searching BLE..."
                customToast.animate().alpha(1f).setDuration(300).start()

                BLE = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive && !connected){
                        Log.d(BLname, "Searching BLE Connection...")
                        delay(500)
                    }
                }
            } else if (!isChecked) {
                BLE?.cancel()
                customToast.animate().alpha(0f).setDuration(300).start()
                Log.d(BLname, "BLE Search Cancelled")
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