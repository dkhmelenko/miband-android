package com.khmelenko.lab.miband

import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

/**
 * Bluetooth listener
 *
 * @author Dmytro Khmelenko
 */
interface BluetoothListener {

    /**
     * Called on established connection
     */
    fun onConnectionEstablished()

    /**
     * Called on disconnection
     */
    fun onDisconnected()

    /**
     * Called on getting successful result
     *
     * @param data Characteristic data
     */
    fun onResult(data: BluetoothGattCharacteristic)

    /**
     * Called on getting successful result of RSSI strength
     *
     * @param rssi RSSI strength
     */
    fun onResultRssi(rssi: Int)

    /**
     * Called on fail from service
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic ID
     * @param msg              Error message
     */
    fun onFail(serviceUUID: UUID, characteristicId: UUID, msg: String)

    /**
     * Called on fail from Bluetooth IO
     *
     * @param errorCode Error code
     * @param msg       Error message
     */
    fun onFail(errorCode: Int, msg: String)
}
