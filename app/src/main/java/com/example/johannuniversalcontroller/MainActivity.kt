package com.example.johannuniversalcontroller

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx. core. view. WindowInsetsControllerCompat
import androidx. core. view. WindowInsetsCompat
import androidx. core. view. WindowCompat
import android. util. Log
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.lifecycle.lifecycleScope
import android.view.View
import android.widget.Switch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val main = R.layout.main_layout
val BLname: String = "Johann 1.0"
var isButtonActive: Boolean = false
var initializing: Boolean = false
var altitudeValue: Float = 0.0f
var switchsJob1: Job? = null
var switchsJob2: Job? = null
var switchsJob3: Job? = null
var seeksJob: Job? = null

class MainActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastTimeBut: Long = 0L
    private var lastTimeBar: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. PASANG LAYOUT KE LAYAR TERLEBIH DAHULU (Wajib di Paling Atas)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }


        // 2. HUBUNGKAN VARIABEL DENGAN ID DI XML

        // Fungsi untuk mengatur logika saling silang (Toggle) pada tombol


        // Fungsi untuk membaca pergeseran Slider Ketinggian


        initializing = true
        controlling()
        altitude()
    }
    private fun altitude() {
                val altitudeSeek: SeekBar = findViewById(R.id.altitude)
                if(!isButtonActive){
                    altitudeSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            // Update nilai hanya jika tombol teks tidak sedang aktif
                            if(!isButtonActive) {
                                altitudeValue = (progress + 1).toFloat()
                                seeksJob = lifecycleScope.launch(Dispatchers.IO) {
                                    while (altitudeValue > 1f) {
                                        Log.d(
                                            "SeekBar " + BLname,
                                            "Altitude: $altitudeValue"
                                        );delay(100)

                                    }
                                }
                            }else{
                                seeksJob?.cancel()

                            }

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                           //-
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            //-
                        }
                    })
                }
    }

    private fun controlling() {
        val ascendBut: Switch = findViewById(R.id.Ascend)
        val hoverBut: Switch = findViewById(R.id.Hover)
        val descendBut: Switch = findViewById(R.id.Descend)
        val altitudeSeek: SeekBar = findViewById(R.id.altitude) // Panggil SeekBar di sini

        hoverBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ascendBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true

                // INSTAN MATIKAN SEEKBAR SAAT TOMBOL NYALA
                altitudeSeek.isEnabled = false

                switchsJob1 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Hover Button is Active"); delay(100)
                    }
                }
            } else {
                switchsJob1?.cancel()

                // Cek apakah semua tombol benar-benar mati
                if (!ascendBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true // HIDUPKAN LAGI SEEKBAR
                }
                Log.d("DebugSwitch", "$isButtonActive")
            }
        }

        ascendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true

                // INSTAN MATIKAN SEEKBAR
                altitudeSeek.isEnabled = false

                switchsJob2 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Ascend Button is Active"); delay(100)
                    }
                }
            } else {
                switchsJob2?.cancel()

                if (!hoverBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true // HIDUPKAN LAGI SEEKBAR
                }
                Log.d("DebugSwitch", "$isButtonActive")
            }
        }

        descendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                ascendBut.isChecked = false
                isButtonActive = true

                // INSTAN MATIKAN SEEKBAR
                altitudeSeek.isEnabled = false

                switchsJob3 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Descend Button is Active"); delay(100)
                    }
                }
            } else {
                switchsJob3?.cancel()

                if (!hoverBut.isChecked && !ascendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true // HIDUPKAN LAGI SEEKBAR
                }
                Log.d("DebugSwitch", "$isButtonActive")
            }
        }
    }
    override fun onStart() {
        super.onStart()

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