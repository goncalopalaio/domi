package com.tri10.domi

import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.Dispatchers

interface Dependencies { // Empty for Now
    val deviceScanner: DeviceScanner
}
class AndroidDependencies(private val applicationContext: Context): Dependencies {
    override val deviceScanner = BluetoothScanner(applicationContext, Dispatchers.IO)
}