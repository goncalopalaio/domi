package com.tri10.domi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.estimateAnimationDurationMillis
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat
import com.tri10.domi.msc.debug
import com.tri10.domi.msc.err
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.Exception

interface DeviceScanner {
    val data: SharedFlow<ScannerResult>
    suspend fun init()
    suspend fun startScan()
    suspend fun stopScan()
    suspend fun connect(macAddress: String)
    suspend fun disconnectAll()
}

sealed class ScannerResult {
    data class ScannedDevice(val macAddress: String, val name: String, val rssi: Int) :
        ScannerResult()

    data class ScanningError(val text: String) : ScannerResult()
}

class BluetoothScanner(
    private val context: Context,
    coroutineDispatcher: CoroutineDispatcher,
) : DeviceScanner {
    private val scope = CoroutineScope(coroutineDispatcher)

    private val _data = MutableSharedFlow<ScannerResult>()
    override val data = _data.asSharedFlow()

    private val adapterState = MutableStateFlow<Pair<Boolean, BluetoothAdapter?>>(Pair(false, null))

    // TODO multithreading
    private val devices = mutableMapOf<String, BluetoothDevice>()
    private val gattCallbacks = mutableMapOf<String, BluetoothGattCallback>()
    private val gatts = mutableMapOf<String, BluetoothGatt>()
    //

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission") // TODO
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            val macAddress = device?.address ?: ""
            val deviceName = device?.name ?: ""
            val rssi = result?.rssi ?: 0 // TODO simply null check, move value to constant

            debug("onScanResult deviceName:${deviceName}, callbackType:$callbackType, result:$result")
            if (deviceName.isEmpty()) return


            if (device != null && macAddress.isNotEmpty()) {
                devices[macAddress] = device
            }

            scope.launch {
                _data.emit(
                    ScannerResult.ScannedDevice(
                        macAddress,
                        deviceName,
                        rssi
                    )
                )
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            debug("results:$results")

        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorName = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
                else -> "UNKNOWN($errorCode)"
            }
            debug("errorCode=$errorCode, errorName=$errorName")

            scope.launch { _data.emit(ScannerResult.ScanningError(errorName)) }
        }
    }

    override suspend fun init() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                val action = intent?.action
                val state =
                    intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                debug("onReceive action=$action state=${bluetoothAdapterState(state)}")

                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        val adapter = bluetoothAdapter() ?: return
                        val isEnabled = adapter.isEnabled
                        debug("adapter=$adapter, isEnabled=$isEnabled")
                        scope.launch { adapterState.emit(Pair(true, adapter)) }
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        val adapter = bluetoothAdapter() ?: return

                        scope.launch { adapterState.emit(Pair(false, adapter)) }
                    }
                    else -> {}
                }

            }

        }, filter)
    }

    @SuppressLint("MissingPermission") // TODO
    override suspend fun startScan() {
        debug("startScan start")

        // stopScan()

        val bluetoothAdapter = try {
            bluetoothAdapter() ?: return // TODO return result
        } catch (e: Throwable) {
            err("startScan e=$e")
            return
        }

        debug("startScan bluetoothAdapter=$bluetoothAdapter")

        //bluetoothAdapter.disable()
        bluetoothAdapter.enable()
        if (!bluetoothAdapter.isEnabled) {

        }

        adapterState.collect {
            val (enabled, adapter) = it
            debug("adapterState.collect enabled=$enabled, adapter=$adapter")
            if (adapter == null) {
                return@collect
            }

            val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner ?: return@collect
            debug("adapterState.collect scanner=$scanner, scanCallback=$scanCallback")

            if (!enabled) {
                scanner.stopScan(scanCallback)
                return@collect
            }

            scanner.startScan(
                emptyList(),
                ScanSettings
                    .Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build(),
                scanCallback,
            )

            debug("startScan finished")
        }

    }

    @SuppressLint("MissingPermission") // TODO
    override suspend fun stopScan() {
        debug("stopScan")
        val adapter = bluetoothAdapter() ?: return
        adapter.disable()
        val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission") // TODO
    override suspend fun connect(macAddress: String) {
        debug("connect macAddress=$macAddress")
        val device = devices[macAddress] ?: return // TODO Return failed result

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val deviceName = gatt?.device?.name
                debug("onConnectionStateChange gatt=$gatt, deviceName=${gatt.deviceName()}, status=${bluetoothGattStatus(status)}, newState=${bluetoothProfileState(newState)})")
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                debug("onServicesDiscovered(gatt=$gatt, deviceName=${gatt.deviceName()}, status=${bluetoothGattStatus(status)})")
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                debug("onCharacteristicRead gatt=$gatt, deviceName=${gatt.deviceName()}, characteristic=$characteristic, status=$status")
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                debug("onCharacteristicWrite gatt=$gatt, deviceName=${gatt.deviceName()}, characteristic=$characteristic, status=$status")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                debug("onCharacteristicChanged gatt=$gatt, deviceName=${gatt.deviceName()}, characteristic=$characteristic")
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorWrite(gatt, descriptor, status)
                debug("onDescriptorWrite gatt=$gatt, deviceName=${gatt.deviceName()}, descriptor=$descriptor, status=$status")
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
                debug("onReadRemoteRssi gatt=$gatt, deviceName=${gatt.deviceName()}, rssi=$rssi, status=$status")
            }
        }

        val gattConnection =
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        gattCallbacks[macAddress] = gattCallback
        gatts[macAddress] = gattConnection
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnectAll() {
        gattCallbacks.clear()
        for ((macAddress, gatt) in gatts) {
            debug("disconnectAll macAddress=$macAddress, gatt=$gatt")
            gatt.disconnect()
        }
    }


    private fun bluetoothAdapter(): BluetoothAdapter? =
        try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        } catch (e: Throwable) {
            err("bluetoothAdapter e=$e")
            null
        }


    private fun bluetoothAdapterState(state: Int?): String? {
        if (state == null) return "UNKNOWN($state)"

        return when (state) {
            BluetoothAdapter.STATE_CONNECTED -> "STATE_CONNECTED"
            BluetoothAdapter.STATE_CONNECTING -> "STATE_CONNECTING"
            BluetoothAdapter.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
            BluetoothAdapter.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
            BluetoothAdapter.STATE_OFF -> "STATE_OFF"
            BluetoothAdapter.STATE_ON -> "STATE_ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "STATE_TURNING_OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "STATE_TURNING_ON"
            else -> "UNKNOWN($state)"
        }
    }

    private fun bluetoothGattStatus(status: Int): String = when (status) {
        BluetoothGatt.GATT_CONNECTION_CONGESTED -> "GATT_CONNECTION_CONGESTED"
        BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
        BluetoothGatt.GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET"
        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED"
        BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED"
        else -> "UNKNOWN($status)"
    }

    private fun bluetoothProfileState(state: Int) = when (state) {
        BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
        BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
        else -> "UNKNOWN($state)"
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothGatt?.deviceName() = this?.device?.name
}