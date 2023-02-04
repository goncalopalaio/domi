import android.net.MacAddress
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tri10.domi.DeviceScanner
import com.tri10.domi.ScannerResult
import com.tri10.domi.msc.ApplicationLogger
import com.tri10.domi.msc.debug
import com.tri10.domi.msc.profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StateViewModel(private val deviceScanner: DeviceScanner) : ViewModel() {

    private val _scannedDevices = mutableStateMapOf<String, Device>()
    val scannedDevices: Map<String, Device> = _scannedDevices

    fun init() {
        profile("StateViewModel", "init", true)
        viewModelScope.launch {
            profile("StateViewModel", "deviceScanner.init", true)
            deviceScanner.init()
            profile("StateViewModel", "deviceScanner.init", false)
        }
        viewModelScope.launch {
            profile("StateViewModel", "deviceScanner.data.collectIndexed", true)
            deviceScanner.data.collectIndexed { index, value ->
                debug("init.deviceScanner.data.collectIndexed index=$index, value=$value")
                when (value) {
                    is ScannerResult.ScannedDevice -> {
                        if (value.macAddress.isNotBlank() && !_scannedDevices.containsKey(value.macAddress)) {
                            _scannedDevices[value.macAddress] =
                                Device(value.macAddress, value.name, value.rssi)
                        }
                    }
                    is ScannerResult.ScanningError -> {}
                }
            }
        }
        profile("StateViewModel", "init", false)
    }

    fun onStopScanRequested() {
        debug("onStopScanRequested")

        viewModelScope.launch { deviceScanner.stopScan() }
    }

    fun onScanRequested() {
        debug("onScanRequested")


        viewModelScope.launch {

            deviceScanner.startScan()
        }
    }

    fun onConnectRequested(macAddress: String) {
        debug("onConnectRequested")

        viewModelScope.launch {
            deviceScanner.connect(macAddress)
        }
    }

    fun onDisconnectAll() {
        debug("onDisconnectAll")

        viewModelScope.launch {
            deviceScanner.disconnectAll()
        }
    }
}

sealed interface State
data class EmptyState(val text: String, val devices: List<String>) : State
data class ScanningState(val devices: Map<String, Device>) : State

data class Device(val macAddress: String, val deviceName: String, val rssi: Int)