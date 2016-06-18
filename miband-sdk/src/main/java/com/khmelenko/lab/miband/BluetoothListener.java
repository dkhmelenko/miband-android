package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothGattCharacteristic;

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
     * Called on successful completion
     *
     * @param data Characteristic data
     */
    void onSuccess(BluetoothGattCharacteristic data);

    /**
     * Called on fail
     *
     * @param errorCode Error code
     * @param msg       Error message
     */
    void onFail(int errorCode, String msg);
}
