package com.example.johannuniversalcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import android.os.SystemClock

val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

var bluetoothAdapter: BluetoothAdapter? = null
var bluetoothGatt: BluetoothGatt? = null
var bleCharacteristic: BluetoothGattCharacteristic? = null

var B: BluetoothDevice? = null


val main = R.layout.main_layout
const val BLname: String = "Johann 1.0"
var isButtonActive: Boolean = false
var initializing: Boolean = false
var altitudeValue: Float = 0.0f
var millis1: Long = 0L
var millis2: Long = 0L
var breakout1: Long = 0L
var switchsJob1: Job? = null
var switchsJob2: Job? = null
var switchsJob3: Job? = null
var BLE: Job? = null
var heartbeat: Job? = null
var connected: Boolean = false
var timelimitBL: Job? = null
var showem: Boolean = false
var johannDevice: android.bluetooth.BluetoothDevice? = null

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        Log.d("OHOHOH", "SWEET")
        cekDanMintaIzinBLE()
        initializing = true
        controlling()
        altitude()

    }

    private fun altitude() {
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)
        val calc: TextView = findViewById(R.id.percentage)
        var forcalc: Float = 0f
        altitudeSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser && !isButtonActive) {
                    altitudeValue = progress.toFloat()
                    kirimPerintah((altitudeValue/100).toString())
                    calc.setText("$altitudeValue%")
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
        val RecVi: Switch = findViewById(R.id.RecVideo)
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)
        val customToast: TextView = findViewById(R.id.newtoast)
        var customBle: Boolean = false
        customToast.alpha = 0f

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

                switchsJob1 = lifecycleScope.launch(Dispatchers.IO) {
                    while(isActive){
                        Log.d(BLname, "Hover Button is Active")
                        kirimPerintah("HOVER")
                        delay(100)
                    }
                }
            } else {
                switchsJob1?.cancel()
                kirimPerintah("STOP_HOVER")
                if (!ascendBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                }
            }
        }

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
                        kirimPerintah("ASCEND")
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
                        kirimPerintah("DESCEND")
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

        ConnectToBle.setOnCheckedChangeListener { _, isChecked ->

            // 1. KONDISI: MAU MENYAMBUNGKAN (AWAL)
            if (isChecked && !connected) {
                // Tampilkan Animasi Mencari
                tembakLangsungKeJohann()
                customToast.text = "Searching Johann's Bluetooth"
                customToast.animate().alpha(1f).setDuration(1500)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(1500)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()

                BLE = lifecycleScope.launch(Dispatchers.IO) {
                    // A. Tembak pencarian SATU KALI SAJA (Jangan ditaruh di dalam while)
                    Log.d(BLname, "Menyalakan Radar / Menembak MAC Address...")
                    mulaiScanBLE() // Memulai pencarian

                    val waktuMulai = SystemClock.elapsedRealtime()

                    // B. Loop ini HANYA untuk menunggu hasil (Koneksi atau Timeout)
                    while (isActive && !connected) {
                        delay(1000) // Santai, cek setiap 1 detik
                        val waktuSekarang = SystemClock.elapsedRealtime()

                        // Jika sudah 30 detik (30.000 ms) dan tetap gagal
                        if (waktuSekarang - waktuMulai >= 30000) {
                            Log.d("SWEET", "CONNECTION NOT FOUND!! Timeout 30 detik.")
                            removeBond(johannDevice)

                            // Gunakan withContext(Main) karena mengubah UI (Toast/Switch) harus dari depan
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                customToast.text = "Connection Timeout!"
                                customToast.animate().alpha(1f).setDuration(1500).withEndAction {
                                    customToast.animate().alpha(0f).setDuration(1500).start()
                                }.start()

                                ConnectToBle.isChecked = false // Matikan Switch otomatis
                            }
                            break // Keluar dari loop pencarian
                        }
                    }

                    // C. Jika loop berhenti karena berhasil nyambung (connected == true)
                    if (connected) {
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            customToast.text = "Connected to Johann!"
                            customToast.animate().alpha(1f).setDuration(700).withEndAction {
                                customToast.animate().alpha(0f).setDuration(700).start()
                            }.start()
                            connected = true
                            // Mulai Heartbeat otomatis saat berhasil connect
                            mulaiHeartbeat(hoverBut, ascendBut, descendBut)
                        }
                    }
                }
            }

            else if (isChecked && connected) {
                ConnectToBle.isEnabled = false // Bekukan tombol sementara
                customToast.text = "Already Connected to Johann"
                connected = true
                customToast.animate().alpha(1f).setDuration(700).withEndAction {
                    customToast.animate().alpha(0f).setDuration(700).start()
                }.start()
            }

            else if (!isChecked && !connected) {
                BLE?.cancel()
                connected = false
                Log.d(BLname, "Pencarian Dibatalkan.")
            }

            // (Logika pengecekan heartbeat untuk perubahan tombol dipindah ke fungsi terpisah agar bersih)
            mulaiHeartbeat(hoverBut, ascendBut, descendBut)
        }
        RecVi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                kirimPerintah("STOPBLE")
                var m: Long = 0L
                m = System.currentTimeMillis()
                if (m > 1300){
                    ConnectToBle.isChecked = false
                    ConnectToBle.isEnabled = false
                    disconnect()
                    BLE?.cancel()
                    heartbeat?.cancel() // Matikan detak jantung
                    connected = false
                    removeBond(johannDevice)
                    customToast.text = "Disconnected from Johann"
                    customToast.animate().alpha(1f).setDuration(700).withEndAction {
                        customToast.animate().alpha(0f).setDuration(700).start()
                    }.start()
                    Log.d(BLname, "Disconnected Manual")
                }

            }
        }

    }

    private fun mulaiHeartbeat(hover: Switch, ascend: Switch, descend: Switch) {
        // 1. Matikan paksa detak jantung lama agar tidak ada sinyal ganda (Spam)
        heartbeat?.cancel()

        // 2. Cek Syarat Mutlak: Harus sudah Connect DAN semua tombol terbang harus OFF
        if (connected && !hover.isChecked && !ascend.isChecked && !descend.isChecked) {

            // 3. Mulai detak jantung baru di jalur latar belakang
            heartbeat = lifecycleScope.launch(Dispatchers.IO) {
                while (isActive) {
                    kirimPerintah("A") // Kirim sinyal "A" ke ESP32 sebagai tanda kehidupan
                    Log.d(BLname, "Heartbeat: Sinyal 'A' terkirim")

                    delay(2000) // Jeda 2 detik antar detak (2000 ms)
                }
            }

        } else {
            // Jika sedang tidak connect atau sedang terbang, pastikan heartbeat mati
            heartbeat = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun tembakLangsungKeJohann() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val macAddressJohann = "E8:3D:C1:95:76:02"

        johannDevice = bluetoothAdapter!!.getRemoteDevice(macAddressJohann)

        Log.d(BLname, "Langsung menyambung ke MAC Address...")

        // Langsung panggil fungsi sambung tanpa perlu Scan lagi!
        sambungkanKeGATT(johannDevice!!)
    }

    @SuppressLint("MissingPermission")
    private fun mulaiScanBLE() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        val scanner = bluetoothAdapter?.bluetoothLeScanner

        Log.d(BLname, "Menyalakan Radar BLE...")

        // Ini adalah "Mata" radar untuk melihat perangkat di sekitar
        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                val device = result?.device

                // Jika nama yang terdeteksi cocok dengan ESP32-mu
                if (device?.name == "Johann 1.0") {
                    Log.d(BLname, "Ketemu Johann! Mematikan radar & mulai menyambung...")
                    scanner?.stopScan(this) // Matikan radar biar hemat baterai
                    sambungkanKeGATT(device)

                }
            }
        }
        scanner?.startScan(scanCallback)
    }

    private fun disconnect() {
        kirimPerintah("STOPBLE")
        bluetoothGatt?.close()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    private fun sambungkanKeGATT(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            // 1. Mengecek apakah jembatan koneksi berhasil terbentuk
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(BLname, "Jembatan Terhubung! Mencari laci data (Service)...")
                    connected = true

                    connected = true
                    gatt?.discoverServices() // Cari Service UUID
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(BLname, "Jembatan Terputus!")
                    connected = false
                }
            }

            // 2. Mengecek apakah Laci Data (UUID) yang dicari ketemu
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(SERVICE_UUID)
                    bleCharacteristic = service?.getCharacteristic(CHAR_UUID)
                    Log.d(BLname, "Johann SIAP DITERBANGKAN!")
                    connected = true
                }else if (status == BluetoothGatt.GATT_FAILURE){
                    connected = false
                }
            }
        })
    }



    // Fungsi ini yang nanti akan kamu panggil dari dalam tombol-tombolmu
    @SuppressLint("MissingPermission")
    private fun kirimPerintah(perintah: String) {
        if (connected && bleCharacteristic != null && bluetoothGatt != null) {
            bleCharacteristic?.value = perintah.toByteArray()
            bluetoothGatt?.writeCharacteristic(bleCharacteristic)
        }
    }
    override fun onPause() {
        super.onPause()

        initializing = false
    }

    private fun cekDanMintaIzinBLE() {
        // Bedakan izin untuk HP Android baru (12+) dan HP lama
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        // Cek izin mana saja yang belum disetujui pengguna
        val belumDiizinkan = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Jika ada yang belum diizinkan, munculkan pop-up!
        if (belumDiizinkan.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, belumDiizinkan.toTypedArray(), 1)
        }
    }

    private fun removeBond(device: BluetoothDevice?) {
        try {
            // Gunakan reflection aman, hanya jika device tidak null
            device?.let {
                val method = it.javaClass.getMethod("removeBond")
                method.invoke(it)
                Log.d(BLname, "Bond/Pairing berhasil dihapus dari sistem.")
            }
        } catch (e: Exception) {
            Log.e(BLname, "Gagal unpair: ${e.message}")
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        initializing = false
    }
}