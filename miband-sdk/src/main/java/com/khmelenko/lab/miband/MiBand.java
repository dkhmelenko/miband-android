package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import com.khmelenko.lab.miband.listeners.HeartRateNotifyListener;
import com.khmelenko.lab.miband.listeners.NotifyListener;
import com.khmelenko.lab.miband.listeners.RealtimeStepsNotifyListener;
import com.khmelenko.lab.miband.model.BatteryInfo;
import com.khmelenko.lab.miband.model.LedColor;
import com.khmelenko.lab.miband.model.Profile;
import com.khmelenko.lab.miband.model.Protocol;
import com.khmelenko.lab.miband.model.UserInfo;
import com.khmelenko.lab.miband.model.VibrationMode;

import java.util.Arrays;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.subjects.PublishSubject;


/**
 * Main class for interacting with MiBand
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
public final class MiBand implements BluetoothListener {

    private static final String TAG = "miband-android";

    private final Context mContext;
    private final BluetoothIO mBluetoothIO;

    private PublishSubject<Boolean> mConnectionSubject;
    private PublishSubject<Integer> mRssiSubject;
    private PublishSubject<BatteryInfo> mBatteryInfoSubject;
    private PublishSubject<Void> mPairSubject;
    private boolean mPairRequested;
    private PublishSubject<Void> mStartVibrationSubject;
    private PublishSubject<Void> mStopVibrationSubject;
    private PublishSubject<Boolean> mSensorNotificationSubject;
    private PublishSubject<Boolean> mRealtimeNotificationSubject;
    private PublishSubject<LedColor> mLedColorSubject;
    private PublishSubject<Void> mUserInfoSubject;
    private PublishSubject<Void> mHeartRateSubject;

    public MiBand(Context context) {
        mContext = context;
        mBluetoothIO = new BluetoothIO(this);

        mConnectionSubject = PublishSubject.create();
        mRssiSubject = PublishSubject.create();
        mBatteryInfoSubject = PublishSubject.create();
        mPairSubject = PublishSubject.create();
        mStartVibrationSubject = PublishSubject.create();
        mStopVibrationSubject = PublishSubject.create();
        mSensorNotificationSubject = PublishSubject.create();
        mRealtimeNotificationSubject = PublishSubject.create();
        mLedColorSubject = PublishSubject.create();
        mUserInfoSubject = PublishSubject.create();
        mHeartRateSubject = PublishSubject.create();
    }

    /**
     * Starts scanning for devices
     *
     * @return An Observable which emits ScanResult
     */
    public Observable<ScanResult> startScan() {
        return Observable.create(subscriber -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.startScan(getScanCallback(subscriber));
                } else {
                    Log.e(TAG, "BluetoothLeScanner is null");
                    subscriber.onError(new NullPointerException("BluetoothLeScanner is null"));
                }
            } else {
                Log.e(TAG, "BluetoothAdapter is null");
                subscriber.onError(new NullPointerException("BluetoothLeScanner is null"));
            }
        });
    }

    /**
     * Stops scanning for devices
     *
     * @return An Observable which emits ScanResult
     */
    public Observable<ScanResult> stopScan() {
        return Observable.create(subscriber -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.stopScan(getScanCallback(subscriber));
                } else {
                    Log.e(TAG, "BluetoothLeScanner is null");
                    subscriber.onError(new NullPointerException("BluetoothLeScanner is null"));
                }
            } else {
                Log.e(TAG, "BluetoothAdapter is null");
                subscriber.onError(new NullPointerException("BluetoothLeScanner is null"));
            }
        });
    }

    /**
     * Creates {@link ScanCallback} instance
     *
     * @param subscriber Subscriber
     * @return ScanCallback instance
     */
    private ScanCallback getScanCallback(ObservableEmitter<? super ScanResult> subscriber) {
        return new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                subscriber.onError(new Exception("Scan failed, error code " + errorCode));
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                subscriber.onNext(result);
                subscriber.onComplete();
            }
        };
    }

    /**
     * Starts connection process to the device
     *
     * @param device Device to connect
     */
    public Observable<Boolean> connect(final BluetoothDevice device) {
        return Observable.create(subscriber -> {
            mConnectionSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.connect(mContext, device);
        });
    }

    /**
     * Gets connected device
     *
     * @return Connected device or null, if device is not connected
     */
    public BluetoothDevice getDevice() {
        return mBluetoothIO.getConnectedDevice();
    }

    /**
     * Executes device pairing
     */
    public Observable<Void> pair() {
        return Observable.create(subscriber -> {
            mPairRequested = true;
            mPairSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR, Protocol.PAIR);
        });
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     */
    public Observable<Integer> readRssi() {
        return Observable.create(subscriber -> {
            mRssiSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.readRssi();
        });
    }

    /**
     * Requests battery info
     *
     * @return Battery info instance
     */
    public Observable<BatteryInfo> getBatteryInfo() {
        return Observable.create(subscriber -> {
            mBatteryInfoSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_BATTERY);
        });
    }

    /**
     * Requests starting vibration
     */
    public Observable<Void> startVibration(final VibrationMode mode) {
        return Observable.create(subscriber -> {
            byte[] protocol;
            switch (mode) {
                case VIBRATION_WITH_LED:
                    protocol = Protocol.VIBRATION_WITH_LED;
                    break;
                case VIBRATION_10_TIMES_WITH_LED:
                    protocol = Protocol.VIBRATION_10_TIMES_WITH_LED;
                    break;
                case VIBRATION_WITHOUT_LED:
                    protocol = Protocol.VIBRATION_WITHOUT_LED;
                    break;
                default:
                    return;
            }
            mStartVibrationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocol);
        });
    }

    /**
     * Requests stopping vibration
     */
    public Observable<Void> stopVibration() {
        return Observable.create(subscriber -> {
            mStopVibrationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION,
                    Protocol.STOP_VIBRATION);
        });
    }

    /**
     * Enables sensor notifications
     */
    public Observable<Boolean> enableSensorDataNotify() {
        return Observable.create(subscriber -> {
            mSensorNotificationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.ENABLE_SENSOR_DATA_NOTIFY);
        });
    }

    /**
     * Disables sensor notifications
     */
    public Observable<Boolean> disableSensorDataNotify() {
        return Observable.create(subscriber -> {
            mSensorNotificationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.DISABLE_SENSOR_DATA_NOTIFY);
        });
    }

    /**
     * Sets sensor data notification listener
     *
     * @param listener Notification listener
     */
    public void setSensorDataNotifyListener(final NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_SENSOR_DATA, listener);
    }

    /**
     * Enables realtime steps notification
     */
    public Observable<Boolean> enableRealtimeStepsNotify() {
        return Observable.create(subscriber -> {
            mRealtimeNotificationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.ENABLE_REALTIME_STEPS_NOTIFY);
        });
    }

    /**
     * Disables realtime steps notification
     */
    public Observable<Boolean> disableRealtimeStepsNotify() {
        return Observable.create(subscriber -> {
            mRealtimeNotificationSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT,
                    Protocol.DISABLE_REALTIME_STEPS_NOTIFY);
        });
    }

    /**
     * Sets realtime steps notification listener
     *
     * @param listener Notification listener
     */
    public void setRealtimeStepsNotifyListener(final RealtimeStepsNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_REALTIME_STEPS, data -> {
            Log.d(TAG, Arrays.toString(data));
            if (data.length == 4) {
                int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                listener.onNotify(steps);
            }
        });
    }

    /**
     * Sets notification listener
     *
     * @param listener Listener
     */
    public void setNormalNotifyListener(NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_NOTIFICATION, listener);
    }

    /**
     * Sets LED color
     *
     * @param color Color
     */
    public Observable<LedColor> setLedColor(final LedColor color) {
        return Observable.create(subscriber -> {
            byte[] protocol;
            switch (color) {
                case RED:
                    protocol = Protocol.SET_COLOR_RED;
                    break;
                case BLUE:
                    protocol = Protocol.SET_COLOR_BLUE;
                    break;
                case GREEN:
                    protocol = Protocol.SET_COLOR_GREEN;
                    break;
                case ORANGE:
                    protocol = Protocol.SET_COLOR_ORANGE;
                    break;
                default:
                    return;
            }
            mLedColorSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, protocol);
        });

    }

    /**
     * Sets user info
     *
     * @param userInfo User info
     */
    public Observable<Void> setUserInfo(final UserInfo userInfo) {
        return Observable.create(subscriber -> {
            mUserInfoSubject.subscribe(new ObserverWrapper<>(subscriber));

            BluetoothDevice device = mBluetoothIO.getConnectedDevice();
            byte[] data = userInfo.getBytes(device.getAddress());
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_USER_INFO, data);
        });
    }

    /**
     * Starts heart rate scanner
     */
    public Observable<Void> startHeartRateScan() {
        return Observable.create(subscriber -> {
            mHeartRateSubject.subscribe(new ObserverWrapper<>(subscriber));
            mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_CHAR_HEARTRATE, Protocol.START_HEART_RATE_SCAN);
        });
    }

    /**
     * Sets heart rate scanner listener
     *
     * @param listener Listener
     */
    public void setHeartRateScanListener(final HeartRateNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, data -> {
            Log.d(TAG, Arrays.toString(data));
            if (data.length == 2 && data[0] == 6) {
                int heartRate = data[1] & 0xFF;
                listener.onNotify(heartRate);
            }
        });
    }

    /**
     * Sets heart rate scanner listener for Xiaomi MiBand 2
     *
     * @param listener Listener
     */
    public void setHeartRateScanListenerMiBand2(final HeartRateNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, data -> {
            Log.d(TAG, Arrays.toString(data));
            if (data.length == 2 && data[0] == 0) {
                int heartRate = data[1] & 0xFF;
                listener.onNotify(heartRate);
            }
        });
    }

    /**
     * Notify for connection results
     *
     * @param result True, if connected. False if disconnected
     */
    private void notifyConnectionResult(boolean result) {
        mConnectionSubject.onNext(result);
        mConnectionSubject.onComplete();

        // create new connection subject
        mConnectionSubject = PublishSubject.create();
    }

    @Override
    public void onConnectionEstablished() {
        notifyConnectionResult(true);
    }

    @Override
    public void onDisconnected() {
        notifyConnectionResult(false);
    }

    @Override
    public void onResult(BluetoothGattCharacteristic data) {
        UUID serviceId = data.getService().getUuid();
        UUID characteristicId = data.getUuid();
        if (serviceId.equals(Profile.UUID_SERVICE_MILI)) {

            // pair
            if (characteristicId.equals(Profile.UUID_CHAR_PAIR)) {
                Log.d(TAG, "pair requested " + String.valueOf(mPairRequested));
                if (mPairRequested) {
                    mBluetoothIO.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR);
                    mPairRequested = false;
                } else {
                    mPairSubject.onComplete();
                }
                mPairSubject = PublishSubject.create();
            }

            // Battery info
            if (characteristicId.equals(Profile.UUID_CHAR_BATTERY)) {
                Log.d(TAG, "getBatteryInfo result " + Arrays.toString(data.getValue()));
                if (data.getValue().length == 10) {
                    BatteryInfo info = BatteryInfo.fromByteData(data.getValue());

                    mBatteryInfoSubject.onNext(info);
                    mBatteryInfoSubject.onComplete();
                } else {
                    mBatteryInfoSubject.onError(new Exception("Wrong data format for battery info"));
                }
                mBatteryInfoSubject = PublishSubject.create();
            }

            // Pair
            if (characteristicId.equals(Profile.UUID_CHAR_PAIR)) {
                Log.d(TAG, "Pair result " + Arrays.toString(data.getValue()));
                if (data.getValue().length == 1 && data.getValue()[0] == 2) {
                    mPairSubject.onComplete();
                } else {
                    mPairSubject.onError(new Exception("Pairing failed"));
                }
                mPairSubject = PublishSubject.create();
            }

            // sensor notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                if (Arrays.equals(changedValue, Protocol.ENABLE_SENSOR_DATA_NOTIFY)) {
                    mSensorNotificationSubject.onNext(true);
                } else {
                    mSensorNotificationSubject.onNext(false);
                }
                mSensorNotificationSubject.onComplete();
                mSensorNotificationSubject = PublishSubject.create();
            }

            // realtime notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                if (Arrays.equals(changedValue, Protocol.ENABLE_REALTIME_STEPS_NOTIFY)) {
                    mRealtimeNotificationSubject.onNext(true);
                } else {
                    mRealtimeNotificationSubject.onNext(false);
                }
                mRealtimeNotificationSubject.onComplete();
                mRealtimeNotificationSubject = PublishSubject.create();
            }

            // led color
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                byte[] changedValue = data.getValue();
                LedColor ledColor = LedColor.BLUE;
                if (Arrays.equals(changedValue, Protocol.SET_COLOR_RED)) {
                    ledColor = LedColor.RED;
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_BLUE)) {
                    ledColor = LedColor.BLUE;
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_GREEN)) {
                    ledColor = LedColor.GREEN;
                } else if (Arrays.equals(changedValue, Protocol.SET_COLOR_ORANGE)) {
                    ledColor = LedColor.ORANGE;
                }
                mLedColorSubject.onNext(ledColor);
                mLedColorSubject.onComplete();
                mLedColorSubject = PublishSubject.create();
            }

            // user info
            if (characteristicId.equals(Profile.UUID_CHAR_USER_INFO)) {
                mUserInfoSubject.onComplete();

                mUserInfoSubject = PublishSubject.create();
            }
        }

        // vibration service
        if (serviceId.equals(Profile.UUID_SERVICE_VIBRATION)) {
            if (characteristicId.equals(Profile.UUID_CHAR_VIBRATION)) {
                byte[] changedValue = data.getValue();
                if (Arrays.equals(changedValue, Protocol.STOP_VIBRATION)) {
                    mStopVibrationSubject.onComplete();

                    mStopVibrationSubject = PublishSubject.create();
                } else {
                    mStartVibrationSubject.onComplete();

                    mStartVibrationSubject = PublishSubject.create();
                }
            }
        }

        // heart rate
        if (serviceId.equals(Profile.UUID_SERVICE_HEARTRATE)) {
            if (characteristicId.equals(Profile.UUID_CHAR_HEARTRATE)) {
                byte[] changedValue = data.getValue();
                if (Arrays.equals(changedValue, Protocol.START_HEART_RATE_SCAN)) {
                    mHeartRateSubject.onComplete();

                    mHeartRateSubject = PublishSubject.create();
                }
            }
        }
    }

    @Override
    public void onResultRssi(int rssi) {
        mRssiSubject.onNext(rssi);
        mRssiSubject.onComplete();

        mRssiSubject = PublishSubject.create();
    }

    @Override
    public void onFail(UUID serviceId, UUID characteristicId, String msg) {
        if (serviceId.equals(Profile.UUID_SERVICE_MILI)) {

            // Battery info
            if (characteristicId.equals(Profile.UUID_CHAR_BATTERY)) {
                Log.d(TAG, "getBatteryInfo failed: " + msg);
                mBatteryInfoSubject.onError(new Exception("Wrong data format for battery info"));
                mBatteryInfoSubject = PublishSubject.create();
            }

            // Pair
            if (characteristicId.equals(Profile.UUID_CHAR_PAIR)) {
                Log.d(TAG, "Pair failed " + msg);
                mPairSubject.onError(new Exception("Pairing failed"));
                mPairSubject = PublishSubject.create();
            }

            // sensor notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                Log.d(TAG, "Sensor notify failed " + msg);
                mSensorNotificationSubject.onError(new Exception("Sensor notify failed"));
                mSensorNotificationSubject = PublishSubject.create();
            }

            // realtime notify
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                Log.d(TAG, "Realtime notify failed " + msg);
                mRealtimeNotificationSubject.onError(new Exception("Realtime notify failed"));
                mRealtimeNotificationSubject = PublishSubject.create();
            }

            // led color
            if (characteristicId.equals(Profile.UUID_CHAR_CONTROL_POINT)) {
                Log.d(TAG, "Led color failed");
                mLedColorSubject.onError(new Exception("Changing LED color failed"));
                mLedColorSubject = PublishSubject.create();
            }

            // user info
            if (characteristicId.equals(Profile.UUID_CHAR_USER_INFO)) {
                Log.d(TAG, "User info failed");
                mUserInfoSubject.onError(new Exception("Setting User info failed"));
                mUserInfoSubject = PublishSubject.create();
            }
        }

        // vibration service
        if (serviceId.equals(Profile.UUID_SERVICE_VIBRATION)) {
            if (characteristicId.equals(Profile.UUID_CHAR_VIBRATION)) {
                Log.d(TAG, "Enable/disable vibration failed");
                mStopVibrationSubject.onError(new Exception("Enable/disable vibration failed"));
                mStopVibrationSubject = PublishSubject.create();
            }
        }

        // heart rate
        if (serviceId.equals(Profile.UUID_SERVICE_HEARTRATE)) {
            if (characteristicId.equals(Profile.UUID_CHAR_HEARTRATE)) {
                Log.d(TAG, "Reading heartrate failed");
                mHeartRateSubject.onError(new Exception("Reading heartrate failed"));
                mHeartRateSubject = PublishSubject.create();
            }
        }
    }

    @Override
    public void onFail(int errorCode, String msg) {
        Log.d(TAG, String.format("onFail: errorCode %d, message %s", errorCode, msg));
        switch (errorCode) {
            case BluetoothIO.ERROR_CONNECTION_FAILED:
                mConnectionSubject.onError(new Exception("Establishing connection failed"));
                mConnectionSubject = PublishSubject.create();
                break;
            case BluetoothIO.ERROR_READ_RSSI_FAILED:
                mRssiSubject.onError(new Exception("Reading RSSI failed"));
                mRssiSubject = PublishSubject.create();
                break;
        }
    }
}
