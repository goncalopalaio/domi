package com.tri10.domi

import StateViewModel
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.juul.kable.Advertisement
import com.juul.kable.DiscoveredService
import com.juul.kable.State
import com.tri10.domi.msc.ApplicationLogger
import com.tri10.domi.msc.debug
import com.tri10.domi.msc.profile
import com.tri10.domi.ui.theme.DomiTheme

class MainActivity : ComponentActivity() {
    init {
        ApplicationLogger.inject(AndroidLogger)
    }

    private val viewModelFactory by lazy { ViewModelFactory() }

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
    val services = stateViewModel.services
    val state = stateViewModel.state
    Column {
        Button(onClick = { stateViewModel.start() }) { Text("Start Scan") }

        Text("Device List")
        DeviceList(messages = devices.values.toList(), stateViewModel)
        Text("State")
        StateList(messages = state.values.toList(), stateViewModel)
        Text("Service List")
        ServiceList(messages = services.values.toList(), stateViewModel)
    }

}

@Composable
fun DeviceList(messages: List<Advertisement>, stateViewModel: StateViewModel) {
    LazyColumn {
        items(messages) { device ->
            Button(onClick = { stateViewModel.test(device) }) {
                Text("${device.address} | ${device.name} | ${device.peripheralName}")
            }
        }
    }
}

@Composable
fun StateList(messages: List<State>, stateViewModel: StateViewModel) {
    LazyColumn {
        items(messages) { state ->
            Text("$state")
        }
    }
}

@Composable
fun ServiceList(messages: List<DiscoveredService>, stateViewModel: StateViewModel) {
    LazyColumn {
        items(messages) { service ->
            Button(onClick = { }) {
                Text("${service.serviceUuid} | ${service.characteristics}")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val viewModel = StateViewModel()
    DomiTheme {
        Greeting("Android", viewModel)
    }
}