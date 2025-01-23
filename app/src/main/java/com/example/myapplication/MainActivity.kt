package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var connectedDevicesListView: ListView
    private lateinit var pairedDevicesListView: ListView
    private lateinit var availableDevicesListView: ListView

    private val connectedDevices = mutableListOf<String>()
    private val pairedDevices = mutableListOf<String>()
    private val availableDevices = mutableListOf<String>()

    private lateinit var connectedDevicesAdapter: ArrayAdapter<String>
    private lateinit var pairedDevicesAdapter: ArrayAdapter<String>
    private lateinit var availableDevicesAdapter: ArrayAdapter<String>

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ListViews
        connectedDevicesListView = findViewById(R.id.connectedDevicesListView)
        pairedDevicesListView = findViewById(R.id.pairedDevicesListView)
        availableDevicesListView = findViewById(R.id.availableDevicesListView)

        // Set up adapters
        connectedDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, connectedDevices)
        pairedDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevices)
        availableDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, availableDevices)

        connectedDevicesListView.adapter = connectedDevicesAdapter
        pairedDevicesListView.adapter = pairedDevicesAdapter
        availableDevicesListView.adapter = availableDevicesAdapter

        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            setupBluetooth()
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    private fun setupBluetooth() {
        fetchConnectedDevices()
        fetchPairedDevices()
        discoverAvailableDevices()
    }

    private fun fetchConnectedDevices() {
        bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxy.connectedDevices.forEach { device ->
                    connectedDevices.add("${device.name}\n${device.address}")
                }
                connectedDevicesAdapter.notifyDataSetChanged()
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HEADSET)
    }

    private fun fetchPairedDevices() {
        val devices = bluetoothAdapter.bondedDevices
        devices.forEach {
            pairedDevices.add("${it.name}\n${it.address}")
        }
        pairedDevicesAdapter.notifyDataSetChanged()
    }

    private fun discoverAvailableDevices() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        bluetoothAdapter.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val deviceInfo = "${device.name ?: "Unknown"}\n${device.address}"
                    if (!availableDevices.contains(deviceInfo)) {
                        availableDevices.add(deviceInfo)
                        availableDevicesAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
