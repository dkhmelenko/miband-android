package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.khmelenko.lab.miband.listeners.ActionCallback;
import com.khmelenko.lab.miband.listeners.NotifyListener;
import com.khmelenko.lab.miband.model.Profile;

import java.util.HashMap;
import java.util.UUID;

/**
 * Defines Bluetooth communication
 *
 * @author Dmytro Khmelenko
 */
final class BluetoothIO extends BluetoothGattCallback {
    private static final String TAG = "BluetoothIO";

    private BluetoothGatt mBluetoothGatt;
    private ActionCallback mCallback;

    private HashMap<UUID, NotifyListener> mNotifyListeners = new HashMap<UUID, NotifyListener>();
    private NotifyListener mDisconnectedListener = null;

    /**
     * Connects to the Bluetooth device
     *
     * @param context  Context
     * @param device   Device to connect
     * @param callback Callback
     */
    public void connect(Context context, BluetoothDevice device, ActionCallback callback) {
        mCallback = callback;
        device.connectGatt(context, false, this);
    }

    /**
     * Sets a listener for disconnect state
     *
     * @param disconnectedListener Listener
     */
    public void setDisconnectedListener(NotifyListener disconnectedListener) {
        mDisconnectedListener = disconnectedListener;
    }

    /**
     * Gets remote connected device
     *
     * @return Connected device or null
     */
    public BluetoothDevice getDevice() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "connect to miband first");
            return null;
        }
        return mBluetoothGatt.getDevice();
    }

    public void writeAndRead(final UUID uuid, byte[] valueToWrite, final ActionCallback callback) {
        ActionCallback readCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object characteristic) {
                readCharacteristic(uuid, callback);
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        writeCharacteristic(uuid, valueToWrite, readCallback);
    }

    public void writeCharacteristic(UUID characteristicUUID, byte[] value, ActionCallback callback) {
        writeCharacteristic(Profile.UUID_SERVICE_MILI, characteristicUUID, value, callback);
    }

    public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] value, ActionCallback callback) {
        try {
            if (mBluetoothGatt == null) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            mCallback = callback;
            BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
            if (chara == null) {
                onFail(-1, "BluetoothGattCharacteristic " + characteristicUUID + " is not exsit");
                return;
            }
            chara.setValue(value);
            if (!mBluetoothGatt.writeCharacteristic(chara)) {
                onFail(-1, "gatt.writeCharacteristic() return false");
            }
        } catch (Throwable tr) {
            Log.e(TAG, "writeCharacteristic", tr);
            onFail(-1, tr.getMessage());
        }
    }

    public void readCharacteristic(UUID serviceUUID, UUID uuid, ActionCallback callback) {
        try {
            if (mBluetoothGatt == null) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            mCallback = callback;
            BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(uuid);
            if (chara == null) {
                onFail(-1, "BluetoothGattCharacteristic " + uuid + " is not exsit");
                return;
            }
            if (!mBluetoothGatt.readCharacteristic(chara)) {
                onFail(-1, "gatt.readCharacteristic() return false");
            }
        } catch (Throwable tr) {
            Log.e(TAG, "readCharacteristic", tr);
            onFail(-1, tr.getMessage());
        }
    }

    public void readCharacteristic(UUID uuid, ActionCallback callback) {
        readCharacteristic(Profile.UUID_SERVICE_MILI, uuid, callback);
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     *
     * @param callback Notification callback
     */
    public void readRssi(ActionCallback callback) {
        try {
            if (mBluetoothGatt == null) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            mCallback = callback;
            mBluetoothGatt.readRemoteRssi();
        } catch (Throwable tr) {
            Log.e(TAG, "readRssi", tr);
            onFail(-1, tr.getMessage());
        }

    }

    public void setNotifyListener(UUID serviceUUID, UUID characteristicId, NotifyListener listener) {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "connect to miband first");
            return;
        }

        BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicId);
        if (chara == null) {
            Log.e(TAG, "characteristicId " + characteristicId.toString() + " not found in service " + serviceUUID.toString());
            return;
        }


        mBluetoothGatt.setCharacteristicNotification(chara, true);
        BluetoothGattDescriptor descriptor = chara.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        mNotifyListeners.put(characteristicId, listener);
    }

    public BluetoothGatt getGatt() {
        return mBluetoothGatt;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close();
            if (mDisconnectedListener != null)
                mDisconnectedListener.onNotify(null);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            onSuccess(characteristic);
        } else {
            onFail(status, "onCharacteristicRead fail");
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            onSuccess(characteristic);
        } else {
            onFail(status, "onCharacteristicWrite fail");
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            onSuccess(rssi);
        } else {
            onFail(status, "onCharacteristicRead fail");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mBluetoothGatt = gatt;
            onSuccess(null);
        } else {
            onFail(status, "onServicesDiscovered fail");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (mNotifyListeners.containsKey(characteristic.getUuid())) {
            mNotifyListeners.get(characteristic.getUuid()).onNotify(characteristic.getValue());
        }
    }

    private void onSuccess(Object data) {
        if (mCallback != null) {
            ActionCallback callback = mCallback;
            mCallback = null;
            callback.onSuccess(data);
        }
    }

    private void onFail(int errorCode, String msg) {
        if (mCallback != null) {
            ActionCallback callback = mCallback;
            mCallback = null;
            callback.onFail(errorCode, msg);
        }
    }

}
