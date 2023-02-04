package com.tri10.domi

import Device
import StateViewModel
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tri10.domi.msc.ApplicationLogger
import com.tri10.domi.msc.debug
import com.tri10.domi.msc.profile
import com.tri10.domi.ui.theme.DomiTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MainActivity : ComponentActivity() {
    init {
        ApplicationLogger.inject(AndroidLogger)
    }

    private val viewModelFactory by lazy { ViewModelFactory(AndroidDependencies(application)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile("MainActivity", "OnCreate", true)

        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // TODO Handle permissions properly
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        requestPermissions(permissions, 1234)

        val viewModel = viewModelFactory.create(StateViewModel::class.java)
        viewModel.init()

        setContent {
            DomiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android", viewModel)
                }
            }
        }

        profile("MainActivity", "OnCreate", false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        debug("onRequestPermissionsResult requestCode=$requestCode, permissions=$permissions, grantResults=$grantResults")
    }
}

@Composable
fun Greeting(name: String, stateViewModel: StateViewModel) {
    val devices = stateViewModel.scannedDevices
    Column {
        Button(onClick = { stateViewModel.onScanRequested() }) { Text("Start Scan") }
        Button(onClick = { stateViewModel.onStopScanRequested() }) { Text("Stop Scan") }
        Button(onClick = { stateViewModel.onDisconnectAll() }) { Text("Disconnect All") }

        DeviceList(messages = devices.values.toList(), stateViewModel)
    }

}

@Composable
fun DeviceList(messages: List<Device>, stateViewModel: StateViewModel) {
    LazyColumn {
        items(messages) { device ->
            Button(onClick = { stateViewModel.onConnectRequested(device.macAddress) }) {
                Text("${device.deviceName} | ${device.macAddress} | ${device.rssi}")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val viewModel = StateViewModel(object : DeviceScanner {
        override val data: SharedFlow<ScannerResult>
            get() = MutableSharedFlow()

        override suspend fun init() {}

        override suspend fun startScan() {}
        override suspend fun stopScan() {}
        override suspend fun connect(macAddress: String) {}
        override suspend fun disconnectAll() {}
    })
    DomiTheme {
        Greeting("Android", viewModel)
    }
}