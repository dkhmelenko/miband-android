package com.khmelenko.lab.miband

import android.bluetooth.*
import android.content.Context
import com.khmelenko.lab.miband.model.Profile
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

const val ERROR_CONNECTION_FAILED = 1
const val ERROR_READ_RSSI_FAILED = 2

/**
 * Defines Bluetooth communication
 *
 * @author Dmytro Khmelenko
 */
internal class BluetoothIO(private val listener: BluetoothListener?) : BluetoothGattCallback() {

    private var bluetoothGatt: BluetoothGatt? = null

    private var notifyListeners: HashMap<UUID, (ByteArray) -> Unit> = HashMap()

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
        return bluetoothGatt?.device
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

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                characteristic.value = value
                val writeResult = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
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

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                val readResult = bluetoothGatt?.readCharacteristic(characteristic) ?: false
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
        val readResult = bluetoothGatt?.readRemoteRssi() ?: false
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

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                bluetoothGatt?.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt?.writeDescriptor(descriptor)
                notifyListeners.put(characteristicId, listener)
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

        val service = bluetoothGatt?.getService(serviceUUID)
        if (service != null) {
            val characteristic = service.getCharacteristic(characteristicId)
            if (characteristic != null) {
                bluetoothGatt?.setCharacteristicNotification(characteristic, false)
                val descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION)
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                bluetoothGatt?.writeDescriptor(descriptor)
                notifyListeners.remove(characteristicId)
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
            listener?.onDisconnected()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bluetoothGatt = gatt
            checkAvailableServices()
            listener?.onConnectionEstablished()
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
        if (notifyListeners.containsKey(characteristic.uuid)) {
            notifyListeners[characteristic.uuid]?.invoke(characteristic.value)
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            Timber.d("onReadRemoteRssi: ${rssi}")
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
        if (bluetoothGatt == null) {
            Timber.e("Connect device first")
            throw IllegalStateException("Device is not connected")
        }
    }

    /**
     * Checks available services, characteristics and descriptors
     */
    private fun checkAvailableServices() {
        for (service in bluetoothGatt?.services.orEmpty()) {
            Timber.d("onServicesDiscovered: ${service.uuid}")

            for (characteristic in service.characteristics) {
                Timber.d("  char: ${characteristic.uuid}")

                for (descriptor in characteristic.descriptors) {
                    Timber.d("    descriptor: ${descriptor.uuid}")
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
            listener?.onResult(data)
        }
    }

    /**
     * Notifies with success result

     * @param data Result data
     */
    private fun notifyWithResult(data: Int) {
        listener?.onResultRssi(data)
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
        listener?.onFail(serviceUUID, characteristicId, msg)
    }

    /**
     * Notifies with failed result

     * @param errorCode Error code
     * *
     * @param msg       Message
     */
    private fun notifyWithFail(errorCode: Int, msg: String) {
        listener?.onFail(errorCode, msg)
    }

}
