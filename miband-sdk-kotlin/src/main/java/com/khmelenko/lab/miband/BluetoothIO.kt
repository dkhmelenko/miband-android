package com.khmelenko.lab.miband

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.khmelenko.lab.miband.listeners.NotifyListener
import com.khmelenko.lab.miband.model.Profile
import java.util.*
import kotlin.collections.HashMap

/**
 * Defines Bluetooth communication
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
internal class BluetoothIO(private val mListener: BluetoothListener?) : BluetoothGattCallback() {

    private val TAG = "BluetoothIO"

    companion object {
        val ERROR_CONNECTION_FAILED = 1
        val ERROR_READ_RSSI_FAILED = 2
    }

    private var mBluetoothGatt: BluetoothGatt? = null

    private var mNotifyListeners: HashMap<UUID, (ByteArray) -> Unit> = HashMap<UUID, (ByteArray) -> Unit>()

    /**
     * Connects to the Bluetooth device

     * @param context Context
     * *
     * @param device  Device to connect
     */
    fun connect(context: Context, device: BluetoothDevice) {
        device.connectGatt(context, false, this)
    }

    /**
     * Gets remote connected device

     * @return Connected device or null
     */
    fun getConnectedDevice(): BluetoothDevice? {
        return mBluetoothGatt?.device
    }

    /**
     * Writes data to the service

     * @param serviceUUID      Service UUID
     * *
     * @param characteristicId Characteristic UUID
     * *
     * @param value            Value to write
     */
    fun writeCharacteristic(serviceUUID: UUID, characteristicId: UUID, value: ByteArray) {
        checkConnectionState()

        val service = mBluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                characteristic.value = value
                val writeResult = mBluetoothGatt?.writeCharacteristic(characteristic) ?: false
                if (!writeResult) {
                    notifyWithFail(serviceUUID, characteristicId, "BluetoothGatt write operation failed")
                }
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic $characteristicId does not exist")
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService $serviceUUID does not exist")
        }
    }

    /**
     * Reads data from the service

     * @param serviceUUID      Service UUID
     * *
     * @param characteristicId Characteristic UUID
     */
    fun readCharacteristic(serviceUUID: UUID, characteristicId: UUID) {
        checkConnectionState()

        val service = mBluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                val readResult = mBluetoothGatt?.readCharacteristic(characteristic) ?: false
                if (readResult) {
                    notifyWithFail(serviceUUID, characteristicId, "BluetoothGatt read operation failed")
                }
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic $characteristicId does not exist")
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService $serviceUUID does not exist")
        }
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     */
    fun readRssi() {
        checkConnectionState()
        val readResult = mBluetoothGatt?.readRemoteRssi() ?: false
        if (!readResult) {
            notifyWithFail(ERROR_READ_RSSI_FAILED, "Request RSSI value failed")
        }
    }

    /**
     * Sets notification listener for specific service and specific characteristic

     * @param serviceUUID      Service UUID
     * *
     * @param characteristicId Characteristic UUID
     * *
     * @param listener         New listener
     */
    fun setNotifyListener(serviceUUID: UUID, characteristicId: UUID, listener: (ByteArray) -> Unit) {
        checkConnectionState()

        val service = mBluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                mBluetoothGatt?.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                mBluetoothGatt?.writeDescriptor(descriptor)
                mNotifyListeners.put(characteristicId, listener)
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic $characteristicId does not exist")
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService $serviceUUID does not exist")
        }
    }

    /**
     * Removes notification listener for the service and characteristic

     * @param serviceUUID      Service UUID
     * *
     * @param characteristicId Characteristic UUID
     */
    fun removeNotifyListener(serviceUUID: UUID, characteristicId: UUID) {
        checkConnectionState()

        val service = mBluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                mBluetoothGatt?.setCharacteristicNotification(characteristic, false)
                val descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION)
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                mBluetoothGatt?.writeDescriptor(descriptor)
                mNotifyListeners.remove(characteristicId)
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic $characteristicId does not exist")
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService $serviceUUID does not exist")
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else {
            gatt.close()
            mListener?.onDisconnected()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mBluetoothGatt = gatt
            checkAvailableServices()
            mListener?.onConnectionEstablished()
        } else {
            notifyWithFail(ERROR_CONNECTION_FAILED, "onServicesDiscovered fail: " + status.toString())
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            notifyWithResult(characteristic)
        } else {
            val serviceId = characteristic.service.uuid
            val characteristicId = characteristic.uuid
            notifyWithFail(serviceId, characteristicId, "onCharacteristicRead fail")
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            notifyWithResult(characteristic)
        } else {
            val serviceId = characteristic.service.uuid
            val characteristicId = characteristic.uuid
            notifyWithFail(serviceId, characteristicId, "onCharacteristicWrite fail")
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        if (mNotifyListeners.containsKey(characteristic.uuid)) {
            mNotifyListeners[characteristic.uuid]?.invoke(characteristic.value)
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            Log.d(TAG, "onReadRemoteRssi:" + rssi)
            notifyWithResult(rssi)
        } else {
            notifyWithFail(ERROR_READ_RSSI_FAILED, "onCharacteristicRead fail: " + status.toString())
        }
    }

    /**
     * Checks connection state.

     * @throws IllegalStateException if device is not connected
     */
    @Throws(IllegalStateException::class)
    private fun checkConnectionState() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "Connect device first")
            throw IllegalStateException("Device is not connected")
        }
    }

    /**
     * Checks available services, characteristics and descriptors
     */
    private fun checkAvailableServices() {
        for (service in mBluetoothGatt?.services.orEmpty()) {
            Log.d(TAG, "onServicesDiscovered:" + service.uuid)

            for (characteristic in service.characteristics) {
                Log.d(TAG, "  char:" + characteristic.uuid)

                for (descriptor in characteristic.descriptors) {
                    Log.d(TAG, "    descriptor:" + descriptor.uuid)
                }
            }
        }
    }

    /**
     * Notifies with success result

     * @param data Result data
     */
    private fun notifyWithResult(data: BluetoothGattCharacteristic?) {
        if (data != null) {
            mListener?.onResult(data)
        }
    }

    /**
     * Notifies with success result

     * @param data Result data
     */
    private fun notifyWithResult(data: Int) {
        mListener?.onResultRssi(data)
    }

    /**
     * Notifies with failed result

     * @param serviceUUID      Service UUID
     * *
     * @param characteristicId Characteristic ID
     * *
     * @param msg              Message
     */
    private fun notifyWithFail(serviceUUID: UUID, characteristicId: UUID, msg: String) {
        mListener?.onFail(serviceUUID, characteristicId, msg)
    }

    /**
     * Notifies with failed result

     * @param errorCode Error code
     * *
     * @param msg       Message
     */
    private fun notifyWithFail(errorCode: Int, msg: String) {
        mListener?.onFail(errorCode, msg)
    }

}
