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
import android.widget.Toast

val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

var bluetoothAdapter: BluetoothAdapter? = null
var bluetoothGatt: BluetoothGatt? = null
var bleCharacteristic: BluetoothGattCharacteristic? = null

var B: BluetoothDevice? = null

val main = R.layout.main_layout_landscape
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
var joyX: Int = 0
var joyY: Int = 0
var timelimitBL: Job? = null
var showem: Boolean = false
var johannDevice: android.bluetooth.BluetoothDevice? = null
var send: Job? = null
var allowRecvi: Boolean = false
// JJob dihapus — joystick tidak lagi spawn coroutine sendiri


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(main)
        actionBar?.hide()
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
        joystick()
    }

    private fun warning(
        context: Context,
        title: String,
        message: String,
        messageToast: String,
        onAksiOk: () -> Unit,
        onAksiBatal: () -> Unit
    ) {
        val customToast: TextView = findViewById(R.id.newtoast)
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(title)
            .setMessage(message.trimIndent())
            .setCancelable(false)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                onAksiOk()
                customToast.text = "Johann Disconnected"
                customToast.animate().alpha(1f).setDuration(1500)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(1500)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                onAksiBatal()
            }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            .setTextColor(android.graphics.Color.WHITE)
        alertDialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.WHITE)
    }

    private fun joystick() {
        val base = findViewById<androidx.cardview.widget.CardView>(R.id.joystickBase)
        val hat = findViewById<androidx.cardview.widget.CardView>(R.id.joystickHat)

        var centerX = 0f
        var centerY = 0f
        var maxRadius = 0f

        base.viewTreeObserver.addOnGlobalLayoutListener {
            centerX = base.width / 2f
            centerY = base.height / 2f
            maxRadius = (base.width / 2f) - (hat.width / 2f)
        }

        base.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> {
                    var dx = event.x - centerX
                    var dy = event.y - centerY
                    val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    if (distance > maxRadius) {
                        val ratio = maxRadius / distance
                        dx *= ratio
                        dy *= ratio
                    }

                    hat.translationX = dx
                    hat.translationY = dy

                    joyX = ((dx / maxRadius) * 100).toInt()
                    joyY = ((-dy / maxRadius) * 100).toInt()

                    // ✅ FIX: Tidak spawn coroutine baru di sini.
                    // Hanya update nilai joyX/joyY. Loop send yang sudah berjalan
                    // akan otomatis membaca nilai terbaru ini.
                    // Panggil mulaiKirimAltitude() SEKALI hanya jika send belum jalan.
                    if (send == null || send?.isActive == false) {
                        mulaiKirimAltitude()
                    }

                    Log.d(BLname, "Joystick X: $joyX | Y: $joyY")
                }

                android.view.MotionEvent.ACTION_UP -> {
                    hat.translationX = 0f
                    hat.translationY = 0f
                    joyX = 0
                    joyY = 0
                    // Restart send loop — sekarang joyX/Y = 0, akan kirim altitude saja
                    mulaiKirimAltitude()
                    Log.d(BLname, "Joystick Released! X: 0 | Y: 0")
                }
            }
            true
        }
    }

    private fun altitude() {
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)
        val calc: TextView = findViewById(R.id.percentage)

        altitudeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isButtonActive) {
                    altitudeValue = progress.toFloat()
                    calc.setText("$altitudeValue%")
                    Log.d("SeekBar $BLname", "Altitude: $altitudeValue")
                    // Tidak perlu kirimPerintah di sini — loop send akan mengirimnya
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun musnahkanKoneksi() {
        BLE?.cancel()
        heartbeat?.cancel()

        lifecycleScope.launch(Dispatchers.IO) {
            if (connected) {
                kirimPerintah("STOPBLE")
                delay(150)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                bluetoothGatt?.disconnect()
                delay(50)
                bluetoothGatt?.close()
                bluetoothGatt = null
                bleCharacteristic = null
                removeBond(johannDevice)
                connected = false
                Log.d(BLname, "KONEKSI TELAH DIMUSNAHKAN SEPERTI TIDAK PERNAH ADA. 💥")
            }
        }
    }

    private fun controlling() {
        val ascendBut: Switch = findViewById(R.id.Ascend)
        val hoverBut: Switch = findViewById(R.id.Hover)
        val descendBut: Switch = findViewById(R.id.Descend)
        val ConnectToBle: Switch = findViewById(R.id.ConnectBLE)
        val RecVi: Switch = findViewById(R.id.RecVideo)
        val altitudeSeek: SeekBar = findViewById(R.id.altitude)
        val customToast: TextView = findViewById(R.id.newtoast)
        customToast.alpha = 0f

        // ==========================================
        // 1. TOMBOL HOVER
        // ==========================================
        hoverBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                breakout1 = System.currentTimeMillis()
                ascendBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false
                send?.cancel()

                customToast.text = "Hovering!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(700)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()

                switchsJob1 = lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        Log.d(BLname, "Hover Button is Active")
                        kirimPerintah("HOVER")
                        delay(100)
                    }
                }
            } else {
                switchsJob1?.cancel()
                if (!ascendBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                    mulaiKirimAltitude()
                }
            }
        }

        // ==========================================
        // 2. TOMBOL ASCEND
        // ==========================================
        ascendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                descendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false
                send?.cancel()

                customToast.text = "Ascending!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(700)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()

                switchsJob2 = lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        Log.d(BLname, "Ascend Button is Active")
                        kirimPerintah("ASCEND")
                        delay(100)
                    }
                }
            } else {
                switchsJob2?.cancel()
                if (!hoverBut.isChecked && !descendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                    mulaiKirimAltitude()
                }
            }
        }

        // ==========================================
        // 3. TOMBOL DESCEND
        // ==========================================
        descendBut.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                hoverBut.isChecked = false
                ascendBut.isChecked = false
                isButtonActive = true
                altitudeSeek.isEnabled = false
                send?.cancel()

                customToast.text = "Descending!"
                customToast.animate().alpha(1f).setDuration(700)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(700)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()

                switchsJob3 = lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        Log.d(BLname, "Descend Button is Active")
                        kirimPerintah("DESCEND")
                        delay(100)
                    }
                }
            } else {
                switchsJob3?.cancel()
                kirimPerintah("STOP_DESCEND")
                if (!hoverBut.isChecked && !ascendBut.isChecked) {
                    isButtonActive = false
                    altitudeSeek.isEnabled = true
                    mulaiKirimAltitude()
                }
            }
        }

        // ==========================================
        // 4. TOMBOL CONNECT BLE
        // ==========================================
        ConnectToBle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !connected) {
                tembakLangsungKeJohann()
                customToast.text = "Searching Johann's Bluetooth"
                customToast.animate().alpha(1f).setDuration(1500)
                    .setInterpolator(DecelerateInterpolator()).withEndAction {
                        customToast.animate().alpha(0f).setDuration(1500)
                            .setInterpolator(AccelerateInterpolator()).start()
                    }.start()

                BLE = lifecycleScope.launch(Dispatchers.IO) {
                    Log.d(BLname, "Menyalakan Radar / Menembak MAC Address...")
                    mulaiScanBLE()

                    val waktuMulai = SystemClock.elapsedRealtime()
                    while (isActive && !connected) {
                        delay(1000)
                        val waktuSekarang = SystemClock.elapsedRealtime()
                        if (waktuSekarang - waktuMulai >= 30000) {
                            Log.d("SWEET", "CONNECTION NOT FOUND!! Timeout 30 detik.")
                            removeBond(johannDevice)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                customToast.text = "Connection Timeout!"
                                customToast.animate().alpha(1f).setDuration(1500).withEndAction {
                                    customToast.animate().alpha(0f).setDuration(1500).start()
                                }.start()
                                ConnectToBle.isChecked = false
                            }
                            break
                        }
                    }

                    if (connected) {
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            customToast.text = "Connected to Johann!"
                            customToast.animate().alpha(1f).setDuration(700).withEndAction {
                                customToast.animate().alpha(0f).setDuration(700).start()
                            }.start()
                            mulaiKirimAltitude()
                        }
                    }
                }
            } else if (!isChecked && connected) {
                musnahkanKoneksi()
                customToast.text = "Disconnected from Johann"
                customToast.animate().alpha(1f).setDuration(700).withEndAction {
                    customToast.animate().alpha(0f).setDuration(700).start()
                }.start()
            } else if (isChecked && connected) {
                ConnectToBle.isEnabled = false
                customToast.text = "Already Connected to Johann"
                customToast.animate().alpha(1f).setDuration(700).withEndAction {
                    customToast.animate().alpha(0f).setDuration(700).start()
                    ConnectToBle.isEnabled = true
                }.start()
                mulaiKirimAltitude()
            } else if (!isChecked && !connected) {
                BLE?.cancel()
                connected = false
                Log.d(BLname, "Pencarian Dibatalkan.")
            }
        }

        // ==========================================
        // 5. TOMBOL REC VIDEO (NUKLIR)
        // ==========================================
        RecVi.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                warning(
                    context = this@MainActivity,
                    title = "WARNING",
                    message = "Disconnecting Johann will automatically remove its Bluetooth from the phone. If disconnected while flying, Johann will descend automatically.",
                    messageToast = "BLE Connection Destroyed.",
                    onAksiOk = {
                        ConnectToBle.isChecked = false
                        ConnectToBle.isEnabled = false
                        musnahkanKoneksi()
                        kirimPerintah("STOP")
                        Log.d("YOOOOO", "CONNECTION DESTROYED")
                    },
                    onAksiBatal = {
                        buttonView.isChecked = false
                    }
                )
            } else {
                ConnectToBle.isEnabled = true
                Log.d(BLname, "Enable to Connect")
            }
        }
    }

    private fun mulaiKirimAltitude() {
        // Bunuh job lama agar tidak ada pengiriman ganda
        kirimPerintah("STOP")
        Log.d(BLname, "Kirim perintah STOP")
        send?.cancel()

        if (!connected) {
            send = null
            return
        }

        val joystickAktif = (joyX != 0 || joyY != 0)

        send = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Baca nilai terkini SETIAP iterasi — nilai bisa berubah sewaktu loop berjalan
                val currentAltitude = (altitudeValue / 100.0f).toString()
                val currentJoyX = joyX
                val currentJoyY = joyY
                val currentJoystickAktif = (currentJoyX != 0 || currentJoyY != 0)

                when {
                    // ✅ MODE 1: Joystick digerakkan + tidak ada tombol mode terbang
                    currentJoystickAktif && !isButtonActive -> {
                        // Kirim data XY dan Altitude secara BERGANTIAN
                        kirimPerintah("X${currentJoyX}Y${currentJoyY}")
                        Log.d(BLname, "→ Joystick: X=$currentJoyX | Y=$currentJoyY")
                        delay(150)

                        kirimPerintah(currentAltitude)
                        Log.d("SENDING", "→ Altitude: $currentAltitude")
                        delay(150)
                    }

                    // ✅ MODE 2: Joystick digerakkan + tombol mode terbang aktif (Hover/Ascend/Descend)
                    // Switch job sudah mengirim perintah mode-nya sendiri, kita hanya kirim arah XY
                    currentJoystickAktif && isButtonActive -> {
                        kirimPerintah("X${currentJoyX}Y${currentJoyY}")
                        Log.d(BLname, "→ [Mode Aktif] Joystick: X=$currentJoyX | Y=$currentJoyY")
                        delay(150)
                    }

                    // ✅ MODE 3: Joystick di tengah, tidak ada tombol mode — kirim Altitude sebagai Heartbeat
                    !currentJoystickAktif && !isButtonActive -> {
                        kirimPerintah(currentAltitude)
                        Log.d("SENDING", "→ Heartbeat Altitude: $currentAltitude")
                        delay(150)
                    }
                    else -> {
                        delay(150)
                    }
                }
            }
        }
    }

    private fun mulaiHeartbeat(hover: Switch, ascend: Switch, descend: Switch) {
        heartbeat?.cancel()
        if (connected && !hover.isChecked && !ascend.isChecked && !descend.isChecked) {
            heartbeat = lifecycleScope.launch(Dispatchers.IO) {
                while (isActive) {
                    kirimPerintah("A")
                    Log.d(BLname, "Heartbeat: Sinyal 'A' terkirim")
                    delay(2000)
                }
            }
        } else {
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
        sambungkanKeGATT(johannDevice!!)
    }

    @SuppressLint("MissingPermission")
    private fun mulaiScanBLE() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        Log.d(BLname, "Menyalakan Radar BLE...")

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                val device = result?.device
                if (device?.name == "Johann 1.0") {
                    Log.d(BLname, "Ketemu Johann! Mematikan radar & mulai menyambung...")
                    scanner?.stopScan(this)
                    sambungkanKeGATT(device)
                }
            }
        }
        scanner?.startScan(scanCallback)
    }

    private fun disconnect() {
        kirimPerintah("STOPBLE")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    private fun sambungkanKeGATT(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(BLname, "Jembatan Terhubung! Mencari laci data (Service)...")
                    connected = true
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(BLname, "Jembatan Terputus!")
                    connected = false
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(SERVICE_UUID)
                    bleCharacteristic = service?.getCharacteristic(CHAR_UUID)
                    Log.d(BLname, "Johann SIAP DITERBANGKAN!")
                    connected = true
                } else if (status == BluetoothGatt.GATT_FAILURE) {
                    connected = false
                }
            }
        })
    }

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

        val belumDiizinkan = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (belumDiizinkan.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, belumDiizinkan.toTypedArray(), 1)
        }
    }

    private fun removeBond(device: BluetoothDevice?) {
        try {
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
