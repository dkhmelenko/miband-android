package com.khmelenko.lab.mibanddemo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.khmelenko.lab.miband.MiBand;
import com.khmelenko.lab.miband.model.UserInfo;
import com.khmelenko.lab.miband.model.VibrationMode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Main application activity
 *
 * @author Dmytro Khmelenko
 */
public class MainActivity extends AppCompatActivity {

    private MiBand mMiBand;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mMiBand = new MiBand(this);
    }

    @OnClick(R.id.action_connect)
    public void actionConnect() {
        Intent intent = getIntent();
        final BluetoothDevice device = intent.getParcelableExtra("device");

        final ProgressDialog pd = ProgressDialog.show(MainActivity.this, "", "Connecting...");
        mMiBand.connect(device)
                .subscribe(result -> {
                    pd.dismiss();
                    Timber.d("Connect onNext: " + String.valueOf(result));
                }, throwable -> {
                    pd.dismiss();
                    throwable.printStackTrace();
                    Timber.e(throwable);
                });
    }

    @OnClick(R.id.action_pair)
    public void actionPair() {
        mMiBand.pair().subscribe(aVoid -> {
            Timber.d("Pairing successful");
        }, throwable -> {
            Timber.e(throwable, "Pairing failed");
        });
    }

    @OnClick(R.id.action_show_services)
    public void actionShowServices() {
        // TODO miband.showServicesAndCharacteristics();
    }

    @OnClick(R.id.action_read_rssi)
    public void actionReadRssi() {
        mMiBand.readRssi()
                .subscribe(rssi -> Timber.d("rssi:" + String.valueOf(rssi)),
                        throwable -> Timber.e(throwable, "readRssi fail"),
                        () -> Timber.d("Rssi onCompleted"));
    }

    @OnClick(R.id.action_battery_info)
    public void actionBatteryInfo() {
        mMiBand.getBatteryInfo()
                .subscribe(batteryInfo -> {
                    Timber.d(batteryInfo.toString());
                }, throwable -> {
                    Timber.e(throwable, "getBatteryInfo fail");
                });
    }

    @OnClick(R.id.action_set_user_info)
    public void actionSetUserInfo() {
        UserInfo userInfo = new UserInfo(20271234, 1, 32, 160, 40, "alias", 0);
        Timber.d("setUserInfo:" + userInfo.toString() + ",data:" + Arrays.toString(userInfo.getBytes(mMiBand.getDevice().getAddress())));
        mMiBand.setUserInfo(userInfo)
                .subscribe(aVoid -> {
                    Timber.d("setUserInfo success");
                }, throwable -> {
                    Timber.e(throwable, "setUserInfo failed");
                });
    }

    @OnClick(R.id.action_set_heart_rate_notify_listener)
    public void actionSetHeartRateNotifyListener() {
        mMiBand.setHeartRateScanListener(heartRate -> Timber.d("heart rate: " + heartRate));
    }

    @OnClick(R.id.action_start_heart_rate_scan)
    public void actionStartHeartRateScan() {
        mMiBand.startHeartRateScan();
    }

    @OnClick(R.id.action_start_vibro_with_led)
    public void actionStartVibroWithLed() {
        mMiBand.startVibration(VibrationMode.VIBRATION_WITH_LED)
                .subscribe(Void -> {
                    Timber.d("Vibration started");
                });
    }

    @OnClick(R.id.action_start_vibro)
    public void actionStartVibro() {
        mMiBand.startVibration(VibrationMode.VIBRATION_WITHOUT_LED)
                .subscribe(Void -> {
                    Timber.d("Vibration started");
                });
    }

    @OnClick(R.id.action_start_vibro_with_led_time)
    public void actionStartVibroWithLedAndTime() {
        mMiBand.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED)
                .subscribe(Void -> {
                    Timber.d("Vibration started");
                });
    }

    @OnClick(R.id.action_stop_vibro)
    public void actionStopVibration() {
        mMiBand.stopVibration()
                .subscribe(Void -> {
                    Timber.d("Vibration stopped");
                });
    }

    @OnClick(R.id.action_set_notify_listener)
    public void actionSetNotifyListener() {
        mMiBand.setNormalNotifyListener(data -> Timber.d("NormalNotifyListener:" + Arrays.toString(data)));
    }

    @OnClick(R.id.action_set_realtime_notify_listener)
    public void actionSetRealtimeNotifyListener() {
        mMiBand.setRealtimeStepsNotifyListener(steps -> Timber.d("RealtimeStepsNotifyListener:" + steps));
    }

    @OnClick(R.id.action_enable_realtime_steps_notify)
    public void actionEnableRealtimeStepsNotify() {
        mMiBand.enableRealtimeStepsNotify();
    }

    @OnClick(R.id.action_disable_realtime_steps_notify)
    public void actionDisableRealtimeStepsNotify() {
        mMiBand.disableRealtimeStepsNotify();
    }

    @OnClick(R.id.action_set_sensor_data_notify_listener)
    public void actionSetSensorDataNotifyListener() {
        mMiBand.setSensorDataNotifyListener(data -> {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int i = 0;

            int index = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
            int d1 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
            int d2 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
            int d3 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;

            String logMsg = index + "," + d1 + "," + d2 + "," + d3;
            Timber.d(logMsg);
        });
    }

    @OnClick(R.id.action_enable_sensor_data_notify)
    public void actionEnableSensorDataNotify() {
        mMiBand.enableSensorDataNotify();
    }

    @OnClick(R.id.action_disable_sensor_data_notify)
    public void actionDisableSensorDataNotify() {
        mMiBand.disableSensorDataNotify();
    }
}
