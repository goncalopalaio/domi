import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.*
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

private const val TAG = "StateViewModel"
class StateViewModel : ViewModel() {

    private val _scannedDevices = mutableStateMapOf<String, Advertisement>()
    val scannedDevices: Map<String, Advertisement> = _scannedDevices

    private val _services = mutableStateMapOf<Int, DiscoveredService>()
    val services: Map<Int, DiscoveredService> = _services

    private val _state = mutableStateMapOf<Int, State>()
    val state: Map<Int, State> = _state

    fun start() = try {
        Log.d(TAG, "start")
        val scanner = Scanner {
            filters = null
            logging {
                engine = SystemLogEngine
                level = Logging.Level.Warnings
                format = Logging.Format.Multiline
            }
        }

        viewModelScope.launch {
            scanner.advertisements.collect {
                _scannedDevices[it.address] = it
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "e=$e")
    }

    fun test(advertisement: Advertisement) {
        val FTMS_UUID = UUID.fromString("00001826-0000-1000-8000-00805F9B34FB")

        Log.d(TAG, "advertisement=$advertisement")
        viewModelScope.launch {
            val p = peripheral(advertisement) {
                onServicesDiscovered {
                    Log.d(TAG, "onServicesDiscovered | this=$this")
                }
            }
            Log.d(TAG, "Connecting | p=$p")
            try {
                p.connect()
                Log.d(TAG, "Connected | p=$p")
                viewModelScope.launch {
                    p.state.collect {
                        Log.d(TAG, "State changed | it=$it")
                        _state[0] = it
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "While connecting | e=$e")
            }
        }
    }
}
