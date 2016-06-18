package com.khmelenko.lab.miband.listeners;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Action callback
 *
 * @author Dmytro Khmelenko
 */
public interface ActionCallback {

    /**
     * Called on successful completion
     *
     * @param data Fetched data
     */
    void onSuccess(Object data);

    /**
     * Called on fail
     *
     * @param errorCode Error code
     * @param msg       Error message
     */
    void onFail(int errorCode, String msg);
}
