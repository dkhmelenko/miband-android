package com.khmelenko.lab.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.khmelenko.lab.miband.listeners.ActionCallback;
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

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

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
    private PublishSubject<BluetoothGattCharacteristic> mReadWriteSubject;

    public MiBand(Context context) {
        mContext = context;
        mBluetoothIO = new BluetoothIO(this);

        mConnectionSubject = PublishSubject.create();
        mRssiSubject = PublishSubject.create();
        mReadWriteSubject = PublishSubject.create();
    }

    /**
     * Starts scanning for devices
     *
     * @param callback Callback
     */
    public static void startScan(@NonNull ScanCallback callback) {
        // TODO Change to Rx
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.startScan(callback);
            } else {
                Log.e(TAG, "BluetoothLeScanner is null");
            }
        } else {
            Log.e(TAG, "BluetoothAdapter is null");
        }
    }

    /**
     * Stops scanning for devices
     *
     * @param callback Callback
     */
    public static void stopScan(ScanCallback callback) {
        // TODO Change to Rx
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(callback);
            } else {
                Log.e(TAG, "BluetoothLeScanner is null");
            }
        } else {
            Log.e(TAG, "BluetoothAdapter is null");
        }
    }

    /**
     * Starts connection process to the device
     *
     * @param device Device to connect
     */
    public Observable<Boolean> connect(final BluetoothDevice device) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                mConnectionSubject.subscribe(subscriber);
                mBluetoothIO.connect(mContext, device);
            }
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
     * 和手环配对, 实际用途未知, 不配对也可以做其他的操作
     *
     * @return data = null
     */
    public void pair(final ActionCallback callback) {
        ActionCallback ioCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) data;
                Log.d(TAG, "pair result " + Arrays.toString(characteristic.getValue()));
                if (characteristic.getValue().length == 1 && characteristic.getValue()[0] == 2) {
                    callback.onSuccess(null);
                } else {
                    callback.onFail(-1, "respone values no succ!");
                }
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        // TODO mBluetoothIO.writeAndRead(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_PAIR, Protocol.PAIR, ioCallback);
    }

    /**
     * Reads Received Signal Strength Indication (RSSI)
     */
    public Observable<Integer> readRssi() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                mRssiSubject.subscribe(subscriber);
                mBluetoothIO.readRssi();
            }
        });

    }

    /**
     * 读取手环电池信息
     *
     * @return {@link BatteryInfo}
     */
    public void getBatteryInfo(final ActionCallback callback) {
        ActionCallback ioCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) data;
                Log.d(TAG, "getBatteryInfo result " + Arrays.toString(characteristic.getValue()));
                if (characteristic.getValue().length == 10) {
                    BatteryInfo info = BatteryInfo.fromByteData(characteristic.getValue());
                    callback.onSuccess(info);
                } else {
                    callback.onFail(-1, "result format wrong!");
                }
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        mBluetoothIO.readCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_BATTERY);
    }

    /**
     * 让手环震动
     */
    public void startVibration(VibrationMode mode) {
        byte[] protocal;
        switch (mode) {
            case VIBRATION_WITH_LED:
                protocal = Protocol.VIBRATION_WITH_LED;
                break;
            case VIBRATION_10_TIMES_WITH_LED:
                protocal = Protocol.VIBRATION_10_TIMES_WITH_LED;
                break;
            case VIBRATION_WITHOUT_LED:
                protocal = Protocol.VIBRATION_WITHOUT_LED;
                break;
            default:
                return;
        }
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocal);
    }

    /**
     * 停止以模式Protocol.VIBRATION_10_TIMES_WITH_LED 开始的震动
     */
    public void stopVibration() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION);
    }

    public void setNormalNotifyListener(NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_NOTIFICATION, listener);
    }

    /**
     * 重力感应器数据通知监听, 设置完之后需要另外使用 {@link MiBand#enableRealtimeStepsNotify} 开启 和
     * {@link MiBand##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public void setSensorDataNotifyListener(final NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_SENSOR_DATA, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                listener.onNotify(data);
            }
        });
    }

    /**
     * 开启重力感应器数据通知
     */
    public void enableSensorDataNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_SENSOR_DATA_NOTIFY);
    }

    /**
     * 关闭重力感应器数据通知
     */
    public void disableSensorDataNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_SENSOR_DATA_NOTIFY);
    }

    /**
     * 实时步数通知监听器, 设置完之后需要另外使用 {@link MiBand#enableRealtimeStepsNotify} 开启 和
     * {@link MiBand##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public void setRealtimeStepsNotifyListener(final RealtimeStepsNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_REALTIME_STEPS, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 4) {
                    int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                    listener.onNotify(steps);
                }
            }
        });
    }

    /**
     * 开启实时步数通知
     */
    public void enableRealtimeStepsNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_REALTIME_STEPS_NOTIFY);
    }

    /**
     * 关闭实时步数通知
     */
    public void disableRealtimeStepsNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_REALTIME_STEPS_NOTIFY);
    }

    /**
     * 设置led灯颜色
     */
    public void setLedColor(LedColor color) {
        byte[] protocal;
        switch (color) {
            case RED:
                protocal = Protocol.SET_COLOR_RED;
                break;
            case BLUE:
                protocal = Protocol.SET_COLOR_BLUE;
                break;
            case GREEN:
                protocal = Protocol.SET_COLOR_GREEN;
                break;
            case ORANGE:
                protocal = Protocol.SET_COLOR_ORANGE;
                break;
            default:
                return;
        }
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_CONTROL_POINT, protocal);
    }

    /**
     * 设置用户信息
     *
     * @param userInfo
     */
    public void setUserInfo(UserInfo userInfo) {
        BluetoothDevice device = mBluetoothIO.getConnectedDevice();
        byte[] data = userInfo.getBytes(device.getAddress());
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_USER_INFO, data);
    }


    public void setHeartRateScanListener(final HeartRateNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE, new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 2 && data[0] == 6) {
                    int heartRate = data[1] & 0xFF;
                    listener.onNotify(heartRate);
                }
            }
        });
    }

    public void startHeartRateScan() {

        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_CHAR_HEARTRATE, Protocol.START_HEART_RATE_SCAN);
    }

    /**
     * Notify for connection results
     *
     * @param result True, if connected. False if disconnected
     */
    private void notifyConnectionResult(boolean result) {
        mConnectionSubject.onNext(true);
        mConnectionSubject.onCompleted();

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
        // TODO
    }

    @Override
    public void onResultRssi(int rssi) {
        mRssiSubject.onNext(rssi);
        mRssiSubject.onCompleted();

        mRssiSubject = PublishSubject.create();
    }

    @Override
    public void onFail(int errorCode, String msg) {
        // TODO
    }
}
