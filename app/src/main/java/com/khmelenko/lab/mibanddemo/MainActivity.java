package com.khmelenko.lab.mibanddemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.khmelenko.lab.miband.MiBand;
import com.khmelenko.lab.miband.model.BatteryInfo;
import com.khmelenko.lab.miband.model.LedColor;
import com.khmelenko.lab.miband.model.UserInfo;
import com.khmelenko.lab.miband.model.VibrationMode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import rx.Subscriber;
import rx.Subscription;

/**
 * Main application activity
 *
 * @author Dmytro Khmelenko
 */
public class MainActivity extends Activity {
    static final String[] BUTTONS = new String[]{
            "Connect",
            "showServicesAndCharacteristics",
            "read_rssi",
            "battery_info",
            "setUserInfo",
            "setHeartRateNotifyListener",
            "startHeartRateScan",
            "miband.startVibration(VibrationMode.VIBRATION_WITH_LED);",
            "miband.startVibration(VibrationMode.VIBRATION_WITHOUT_LED);",
            "miband.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED);",
            "stopVibration",
            "setNormalNotifyListener",
            "setRealtimeStepsNotifyListener",
            "enableRealtimeStepsNotify",
            "disableRealtimeStepsNotify",
            "miband.setLedColor(LedColor.ORANGE);",
            "miband.setLedColor(LedColor.BLUE);",
            "miband.setLedColor(LedColor.RED);",
            "miband.setLedColor(LedColor.GREEN);",
            "setSensorDataNotifyListener",
            "enableSensorDataNotify",
            "disableSensorDataNotify",
            "pair",
    };
    private static final String TAG = "==[mibandtest]==";
    private static final int Message_What_ShowLog = 1;
    private MiBand miband;
    private TextView logView;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message m) {
            switch (m.what) {
                case Message_What_ShowLog:
                    String text = (String) m.obj;
                    logView.setText(text);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        this.logView = (TextView) findViewById(R.id.textView);

        Intent intent = this.getIntent();
        final BluetoothDevice device = intent.getParcelableExtra("device");


        miband = new MiBand(this);
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<>(this, R.layout.item, BUTTONS));
        lv.setOnItemClickListener((parent, view, position, id) -> {
            int menuIndex = 0;
            if (position == menuIndex++) {
                final ProgressDialog pd = ProgressDialog.show(MainActivity.this, "", "Connecting...");
                rx.Observable<Boolean> observable = miband.connect(device);
                Subscription subscription = observable.subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {

                        Log.d(TAG, "Connect onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        pd.dismiss();
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Boolean result) {
                        pd.dismiss();
                        Log.d(TAG, "Connect onNext: " + String.valueOf(result));
                    }
                });
            } else if (position == menuIndex++) {
                // TODO miband.showServicesAndCharacteristics();
            } else if (position == menuIndex++) {
                rx.Observable<Integer> observable = miband.readRssi();
                observable.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Rssi onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "readRssi fail");
                    }

                    @Override
                    public void onNext(Integer rssi) {
                        Log.d(TAG, "rssi:" + String.valueOf(rssi));
                    }
                });
            } else if (position == menuIndex++) {
                rx.Observable<BatteryInfo> observable = miband.getBatteryInfo();
                observable.subscribe(batteryInfo -> {
                    Log.d(TAG, batteryInfo.toString());
                }, throwable -> {
                    Log.d(TAG, "getBatteryInfo fail");
                });
            } else if (position == menuIndex++) {
                UserInfo userInfo = new UserInfo(20271234, 1, 32, 160, 40, "alias", 0);
                Log.d(TAG, "setUserInfo:" + userInfo.toString() + ",data:" + Arrays.toString(userInfo.getBytes(miband.getDevice().getAddress())));
                miband.setUserInfo(userInfo);
            } else if (position == menuIndex++) {

                miband.setHeartRateScanListener(heartRate -> Log.d(TAG, "heart rate: " + heartRate));
            } else if (position == menuIndex++) {
                miband.startHeartRateScan();
            } else if (position == menuIndex++) {
                miband.startVibration(VibrationMode.VIBRATION_WITH_LED).subscribe(aVoid -> {
                    Log.d(TAG, "Vibration started");
                });
            } else if (position == menuIndex++) {
                miband.startVibration(VibrationMode.VIBRATION_WITHOUT_LED).subscribe(aVoid -> {
                    Log.d(TAG, "Vibration started");
                });

            } else if (position == menuIndex++) {
                miband.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED).subscribe(aVoid -> {
                    Log.d(TAG, "Vibration started");
                });

            } else if (position == menuIndex++) {
                miband.stopVibration().subscribe(aVoid -> {
                    Log.d(TAG, "Vibration stopped");
                });

            } else if (position == menuIndex++) {
                miband.setNormalNotifyListener(data -> Log.d(TAG, "NormalNotifyListener:" + Arrays.toString(data)));
            } else if (position == menuIndex++) {
                miband.setRealtimeStepsNotifyListener(steps -> Log.d(TAG, "RealtimeStepsNotifyListener:" + steps));
            } else if (position == menuIndex++) {
                miband.enableRealtimeStepsNotify();
            } else if (position == menuIndex++) {
                miband.disableRealtimeStepsNotify();
            } else if (position == menuIndex++) {
                miband.setLedColor(LedColor.ORANGE);
            } else if (position == menuIndex++) {
                miband.setLedColor(LedColor.BLUE);
            } else if (position == menuIndex++) {
                miband.setLedColor(LedColor.RED);
            } else if (position == menuIndex++) {
                miband.setLedColor(LedColor.GREEN);
            } else if (position == menuIndex++) {
                miband.setSensorDataNotifyListener(data -> {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                    int i = 0;

                    int index = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                    int d1 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                    int d2 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                    int d3 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;

                    Message m = new Message();
                    m.what = Message_What_ShowLog;
                    m.obj = index + "," + d1 + "," + d2 + "," + d3;

                    handler.sendMessage(m);
                });
            } else if (position == menuIndex++) {
                miband.enableSensorDataNotify();
            } else if (position == menuIndex++) {
                miband.disableSensorDataNotify();
            } else if (position == menuIndex++) {
                rx.Observable<Void> observable = miband.pair();
                observable.subscribe(aVoid -> {
                    Log.d(TAG, "Pairing successful");
                }, throwable -> {
                    Log.d(TAG, "Pairing failed");
                    throwable.printStackTrace();
                });
            }
        });

    }
}
