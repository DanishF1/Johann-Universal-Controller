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

val main = R.layout.main_layout
val BLname: String = "Johann 1.0"
var isButtonActive: Boolean = false
var initializing: Boolean = false
var altitudeValue: Float = 0.0f

class MainActivity : ComponentActivity() {

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
        val ascendBut: SwitchCompat = findViewById(R.id.Ascend)
        val hoverBut: SwitchCompat = findViewById(R.id.Hover)
        val descendBut: SwitchCompat = findViewById(R.id.Descend)
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)

        // Fungsi untuk mengatur logika saling silang (Toggle) pada tombol
        fun controlling() {
            hoverBut.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    ascendBut.isChecked = false
                    descendBut.isChecked = false
                    isButtonActive = true
                }
            }

            ascendBut.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    hoverBut.isChecked = false
                    descendBut.isChecked = false
                    isButtonActive = true
                }
            }

            descendBut.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    hoverBut.isChecked = false
                    ascendBut.isChecked = false
                    isButtonActive = true
                }
            }
        }

        // Fungsi untuk membaca pergeseran Slider Ketinggian
        fun altitude() {
            altitudeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Update nilai hanya jika tombol teks tidak sedang aktif
                    if (!isButtonActive) {
                        altitudeValue = (progress + 1).toFloat()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Dikosongkan karena belum ada logika saat mulai disentuh
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Dikosongkan karena belum ada logika saat dilepas
                }
            })
        }

        // 3. JALANKAN SEMUA FUNGSI (Langsung di Main Thread)
        initializing = true
        controlling()
        altitude()
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