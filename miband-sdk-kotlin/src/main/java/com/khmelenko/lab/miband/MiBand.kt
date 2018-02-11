package com.khmelenko.lab.miband

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.khmelenko.lab.miband.listeners.HeartRateNotifyListener
import com.khmelenko.lab.miband.listeners.RealtimeStepsNotifyListener
import com.khmelenko.lab.miband.model.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.*

/**
 * Main class for interacting with MiBand

 * @author Dmytro Khmelenko
 */
class MiBand(private val context: Context) : BluetoothListener {

    private val bluetoothIo: BluetoothIO = BluetoothIO(this)

    private var connectionSubject: PublishSubject<Boolean> = PublishSubject.create()
    private var rssiSubject: PublishSubject<Int> = PublishSubject.create()
    private var batteryInfoSubject: PublishSubject<BatteryInfo> = PublishSubject.create()
    private var pairSubject: PublishSubject<Void> = PublishSubject.create()
    private var pairRequested: Boolean = false
    private var startVibrationSubject: PublishSubject<Void> = PublishSubject.create()
    private var stopVibrationSubject: PublishSubject<Void> = PublishSubject.create()
    private var sensorNotificationSubject: PublishSubject<Boolean> = PublishSubject.create()
    private var realtimeNotificationSubject: PublishSubject<Boolean> = PublishSubject.create()
    private var ledColorSubject: PublishSubject<LedColor> = PublishSubject.create()
    private var userInfoSubject: PublishSubject<Void> = PublishSubject.create()
    private var heartRateSubject: PublishSubject<Void> = PublishSubject.create()

    val device: BluetoothDevice?
        get() = bluetoothIo.getConnectedDevice()

    /**
     * Starts scanning for devices

     * @return An Observable which emits ScanResult
     */
    fun startScan(): Observable<ScanResult> {
        return Observable.create<ScanResult> { subscriber ->
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                val scanner = adapter.bluetoothLeScanner
                if (scanner != null) {
                    scanner.startScan(getScanCallback(subscriber))
                } else {
                    Timber.d("BluetoothLeScanner is null")
                    subscriber.onError(NullPointerException("BluetoothLeScanner is null"))
                }
            } else {
                Timber.d("BluetoothAdapter is null")
                subscriber.onError(NullPointerException("BluetoothLeScanner is null"))
            }
        }
    }

    /**
     * Stops scanning for devices

     * @return An Observable which emits ScanResult
     */
    fun stopScan(): Observable<ScanResult> {
        return Observable.create<ScanResult> { subscriber ->
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                val scanner = adapter.bluetoothLeScanner
                if (scanner != null) {
                    scanner.stopScan(getScanCallback(subscriber))
                } else {
                    Timber.d("BluetoothLeScanner is null")
                    subscriber.onError(NullPointerException("BluetoothLeScanner is null"))
                }
            } else {
                Timber.d("BluetoothAdapter is null")
                subscriber.onError(NullPointerException("BluetoothLeScanner is null"))
            }
        }
    }

    /**
     * Creates [ScanCallback] instance

     * @param subscriber Subscriber
     * *
     * @return ScanCallback instance
     */
    private fun getScanCallback(subscriber: ObservableEmitter<in ScanResult>): ScanCallback {
        return object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                subscriber.onError(Exception("Scan failed, error code " + errorCode))
            }

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                subscriber.onNext(result)
                subscriber.onComplete()
            }
        }
    }

    /**
     * Starts connection process to the device

     * @param device Device to connect
     */
    fun connect(device: BluetoothDevice): Observable<Boolean> {
        return Observable.create<Boolean> { subscriber ->
            connectionSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.connect(context, device)
        }
    }

    /**
     * Executes device pairing
     */
    fun pair(): Observable<Void> {
        return Observable.create<Void> { subscriber ->
            pairRequested = true
            pairSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR, Protocol.PAIR)
        }
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     */
    fun readRssi(): Observable<Int> {
        return Observable.create<Int> { subscriber ->
            rssiSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.readRssi()
        }
    }

    /**
     * Requests battery info

     * @return Battery info instance
     */
    val batteryInfo: Observable<BatteryInfo>
        get() = Observable.create<BatteryInfo> { subscriber ->
            batteryInfoSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_BATTERY)
        }

    /**
     * Requests starting vibration
     */
    fun startVibration(mode: VibrationMode): Observable<Void> {
        return Observable.create<Void> { subscriber ->
            val protocol = when (mode) {
                VibrationMode.VIBRATION_WITH_LED -> Protocol.VIBRATION_WITH_LED
                VibrationMode.VIBRATION_10_TIMES_WITH_LED -> Protocol.VIBRATION_10_TIMES_WITH_LED
                VibrationMode.VIBRATION_WITHOUT_LED -> Protocol.VIBRATION_WITHOUT_LED
            }
            startVibrationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocol)
        }
    }

    /**
     * Requests stopping vibration
     */
    fun stopVibration(): Observable<Void> {
        return Observable.create<Void> { subscriber ->
            stopVibrationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION,
                    Protocol.STOP_VIBRATION)
        }
    }

    /**
     * Enables sensor notifications
     */
    fun enableSensorDataNotify(): Observable<Boolean> {
        return Observable.create<Boolean> { subscriber ->
            sensorNotificationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.ENABLE_SENSOR_DATA_NOTIFY)
        }
    }

    /**
     * Disables sensor notifications
     */
    fun disableSensorDataNotify(): Observable<Boolean> {
        return Observable.create<Boolean> { subscriber ->
            sensorNotificationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.DISABLE_SENSOR_DATA_NOTIFY)
        }
    }

    /**
     * Sets sensor data notification listener

     * @param listener Notification listener
     */
    fun setSensorDataNotifyListener(listener: (ByteArray) -> Unit) {
        bluetoothIo.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_SENSOR_DATA, listener)
    }

    /**
     * Enables realtime steps notification
     */
    fun enableRealtimeStepsNotify(): Observable<Boolean> {
        return Observable.create<Boolean> { subscriber ->
            realtimeNotificationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.ENABLE_REALTIME_STEPS_NOTIFY)
        }
    }

    /**
     * Disables realtime steps notification
     */
    fun disableRealtimeStepsNotify(): Observable<Boolean> {
        return Observable.create<Boolean> { subscriber ->
            realtimeNotificationSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.DISABLE_REALTIME_STEPS_NOTIFY)
        }
    }

    /**
     * Sets realtime steps notification listener

     * @param listener Notification listener
     */
    fun setRealtimeStepsNotifyListener(listener: RealtimeStepsNotifyListener) {
        bluetoothIo.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_REALTIME_STEPS, { data: ByteArray ->
            Timber.d(Arrays.toString(data))
            if (data.size == 4) {
                val steps = data[3].toInt() shl 24 or (data[2].toInt() and 0xFF shl 16) or
                        (data[1].toInt() and 0xFF shl 8) or (data[0].toInt() and 0xFF)
                listener.onNotify(steps)
            }
        })
    }

    /**
     * Sets notification listener

     * @param listener Listener
     */
    fun setNormalNotifyListener(listener: (ByteArray) -> Unit) {
        bluetoothIo.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_NOTIFICATION, listener)
    }

    /**
     * Sets LED color

     * @param color Color
     */
    fun setLedColor(color: LedColor): Observable<LedColor> {
        return Observable.create<LedColor> { subscriber ->
            val protocol: ByteArray
            when (color) {
                LedColor.RED -> protocol = Protocol.SET_COLOR_RED
                LedColor.BLUE -> protocol = Protocol.SET_COLOR_BLUE
                LedColor.GREEN -> protocol = Protocol.SET_COLOR_GREEN
                LedColor.ORANGE -> protocol = Protocol.SET_COLOR_ORANGE
            }
            ledColorSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, protocol)
        }

    }

    /**
     * Sets user info

     * @param userInfo User info
     */
    fun setUserInfo(userInfo: UserInfo): Observable<Void> {
        return Observable.create<Void> { subscriber ->
            userInfoSubject.subscribe(ObserverWrapper(subscriber))

            val data = userInfo.getBytes(device?.address ?: "")
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_USER_INFO, data)
        }
    }

    /**
     * Starts heart rate scanner
     */
    fun startHeartRateScan(): Observable<Void> {
        return Observable.create<Void> { subscriber ->
            heartRateSubject.subscribe(ObserverWrapper(subscriber))
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_CHAR_HEARTRATE,
                    Protocol.START_HEART_RATE_SCAN)
        }
    }

    /**
     * Sets heart rate scanner listener
     *
     * @param listener Listener
     */
    fun setHeartRateScanListener(listener: HeartRateNotifyListener) {
        bluetoothIo.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, { data ->
            Timber.d(Arrays.toString(data))
            if (data.size == 2 && data[0].toInt() == 6) {
                val heartRate = data[1].toInt() and 0xFF
                listener.onNotify(heartRate)
            }
        })
    }

    /**
     * Sets heart rate scanner listener for Xiaomi MiBand 2
     *
     * @param listener Listener
     */
    fun setHeartRateScanListenerMiBand2(listener: HeartRateNotifyListener) {
        bluetoothIo.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, { data ->
            Timber.d(Arrays.toString(data))
            if (data.size == 2 && data[0].toInt() == 0) {
                val heartRate = data[1].toInt() and 0xFF
                listener.onNotify(heartRate)
            }
        })
    }

    /**
     * Notify for connection results

     * @param result True, if connected. False if disconnected
     */
    private fun notifyConnectionResult(result: Boolean) {
        connectionSubject.onNext(result)
        connectionSubject.onComplete()

        // create new connection subject
        connectionSubject = PublishSubject.create<Boolean>()
    }

    override fun onConnectionEstablished() {
        notifyConnectionResult(true)
    }

    override fun onDisconnected() {
        notifyConnectionResult(false)
    }

    override fun onResult(data: BluetoothGattCharacteristic) {
        val serviceId = data.service.uuid
        val characteristicId = data.uuid
        if (serviceId == Profile.UUID_SERVICE_MILI) {

            // pair
            if (characteristicId == Profile.UUID_CHAR_PAIR) {
                Timber.d("pair requested $pairRequested")
                if (pairRequested) {
                    bluetoothIo.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR)
                    pairRequested = false
                } else {
                    pairSubject.onComplete()
                }
                pairSubject = PublishSubject.create()
            }

            // Battery info
            if (characteristicId == Profile.UUID_CHAR_BATTERY) {
                Timber.d("getBatteryInfo result ${Arrays.toString(data.value)}")
                if (data.value.size == 10) {
                    val info = BatteryInfo.fromByteData(data.value)

                    batteryInfoSubject.onNext(info)
                    batteryInfoSubject.onComplete()
                } else {
                    batteryInfoSubject.onError(Exception("Wrong data format for battery info"))
                }
                batteryInfoSubject = PublishSubject.create()
            }

            // Pair
            if (characteristicId == Profile.UUID_CHAR_PAIR) {
                Timber.d("Pair result ${Arrays.toString(data.value)}")
                if (data.value.size == 1 && data.value[0].toInt() == 2) {
                    pairSubject.onComplete()
                } else {
                    pairSubject.onError(Exception("Pairing failed"))
                }
                pairSubject = PublishSubject.create()
            }

            // sensor notify
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                val changedValue = data.value
                if (Arrays.equals(changedValue, Protocol.ENABLE_SENSOR_DATA_NOTIFY)) {
                    sensorNotificationSubject.onNext(true)
                } else {
                    sensorNotificationSubject.onNext(false)
                }
                sensorNotificationSubject.onComplete()
                sensorNotificationSubject = PublishSubject.create()
            }

            // realtime notify
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                val changedValue = data.value
                if (Arrays.equals(changedValue, Protocol.ENABLE_REALTIME_STEPS_NOTIFY)) {
                    realtimeNotificationSubject.onNext(true)
                } else {
                    realtimeNotificationSubject.onNext(false)
                }
                realtimeNotificationSubject.onComplete()
                realtimeNotificationSubject = PublishSubject.create()
            }

            // led color
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                val changedValue = data.value
                var ledColor = LedColor.BLUE
                if (Arrays.equals(changedValue, Protocol.SET_COLOR_RED)) {
                    ledColor = LedColor.RED
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_BLUE)) {
                    ledColor = LedColor.BLUE
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_GREEN)) {
                    ledColor = LedColor.GREEN
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_ORANGE)) {
                    ledColor = LedColor.ORANGE
                }
                ledColorSubject.onNext(ledColor)
                ledColorSubject.onComplete()
                ledColorSubject = PublishSubject.create()
            }

            // user info
            if (characteristicId == Profile.UUID_CHAR_USER_INFO) {
                userInfoSubject.onComplete()

                userInfoSubject = PublishSubject.create()
            }
        }

        // vibration service
        if (serviceId == Profile.UUID_SERVICE_VIBRATION) {
            if (characteristicId == Profile.UUID_CHAR_VIBRATION) {
                val changedValue = data.value
                if (Arrays.equals(changedValue, Protocol.STOP_VIBRATION)) {
                    stopVibrationSubject.onComplete()
                    stopVibrationSubject = PublishSubject.create()
                } else {
                    startVibrationSubject.onComplete()
                    startVibrationSubject = PublishSubject.create()
                }
            }
        }

        // heart rate
        if (serviceId == Profile.UUID_SERVICE_HEARTRATE) {
            if (characteristicId == Profile.UUID_CHAR_HEARTRATE) {
                val changedValue = data.value
                if (Arrays.equals(changedValue, Protocol.START_HEART_RATE_SCAN)) {
                    heartRateSubject.onComplete()
                    heartRateSubject = PublishSubject.create()
                }
            }
        }
    }

    override fun onResultRssi(rssi: Int) {
        rssiSubject.onNext(rssi)
        rssiSubject.onComplete()

        rssiSubject = PublishSubject.create()
    }

    override fun onFail(serviceUUID: UUID, characteristicId: UUID, msg: String) {
        if (serviceUUID == Profile.UUID_SERVICE_MILI) {

            // Battery info
            if (characteristicId == Profile.UUID_CHAR_BATTERY) {
                Timber.d("getBatteryInfo failed: $msg")
                batteryInfoSubject.onError(Exception("Wrong data format for battery info"))
                batteryInfoSubject = PublishSubject.create()
            }

            // Pair
            if (characteristicId == Profile.UUID_CHAR_PAIR) {
                Timber.d("Pair failed $msg")
                pairSubject.onError(Exception("Pairing failed"))
                pairSubject = PublishSubject.create()
            }

            // sensor notify
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                Timber.d("Sensor notify failed $msg")
                sensorNotificationSubject.onError(Exception("Sensor notify failed"))
                sensorNotificationSubject = PublishSubject.create()
            }

            // realtime notify
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                Timber.d("Realtime notify failed $msg")
                realtimeNotificationSubject.onError(Exception("Realtime notify failed"))
                realtimeNotificationSubject = PublishSubject.create()
            }

            // led color
            if (characteristicId == Profile.UUID_CHAR_CONTROL_POINT) {
                Timber.d("Led color failed")
                ledColorSubject.onError(Exception("Changing LED color failed"))
                ledColorSubject = PublishSubject.create()
            }

            // user info
            if (characteristicId == Profile.UUID_CHAR_USER_INFO) {
                Timber.d("User info failed")
                userInfoSubject.onError(Exception("Setting User info failed"))
                userInfoSubject = PublishSubject.create()
            }
        }

        // vibration service
        if (serviceUUID == Profile.UUID_SERVICE_VIBRATION) {
            if (characteristicId == Profile.UUID_CHAR_VIBRATION) {
                Timber.d("Enable/disable vibration failed")
                stopVibrationSubject.onError(Exception("Enable/disable vibration failed"))
                stopVibrationSubject = PublishSubject.create()
            }
        }

        // heart rate
        if (serviceUUID == Profile.UUID_SERVICE_HEARTRATE) {
            if (characteristicId == Profile.UUID_CHAR_HEARTRATE) {
                Timber.d("Reading heartrate failed")
                heartRateSubject.onError(Exception("Reading heartrate failed"))
                heartRateSubject = PublishSubject.create()
            }
        }
    }

    override fun onFail(errorCode: Int, msg: String) {
        Timber.d(String.format("onFail: errorCode %d, message %s", errorCode, msg))
        when (errorCode) {
            ERROR_CONNECTION_FAILED -> {
                connectionSubject.onError(Exception("Establishing connection failed"))
                connectionSubject = PublishSubject.create()
            }
            ERROR_READ_RSSI_FAILED -> {
                rssiSubject.onError(Exception("Reading RSSI failed"))
                rssiSubject = PublishSubject.create()
            }
        }
    }
}
