package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Bluetooth listener
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
public interface BluetoothListener {

    /**
     * Called on established connection
     */
    void onConnectionEstablished();

    /**
     * Called on disconnection
     */
    void onDisconnected();

    /**
     * Called on getting successful result
     *
     * @param data Characteristic data
     */
    void onResult(BluetoothGattCharacteristic data);

    /**
     * Called on getting successful result of RSSI strength
     *
     * @param rssi RSSI strength
     */
    void onResultRssi(int rssi);

    /**
     * Called on fail from service
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic ID
     * @param msg              Error message
     */
    void onFail(UUID serviceUUID, UUID characteristicId, String msg);

    /**
     * Called on fail from Bluetooth IO
     *
     * @param errorCode Error code
     * @param msg       Error message
     */
    void onFail(int errorCode, String msg);
}
