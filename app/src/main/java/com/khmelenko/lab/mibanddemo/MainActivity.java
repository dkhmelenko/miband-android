package com.khmelenko.lab.mibanddemo;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.khmelenko.lab.miband.MiBand;
import com.khmelenko.lab.miband.model.UserInfo;
import com.khmelenko.lab.miband.model.VibrationMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import kotlin.Unit;
import timber.log.Timber;

/**
 * Main application activity
 *
 * @author Dmytro Khmelenko
 */
public class MainActivity extends AppCompatActivity implements LocationListener {

    private MiBand mMiBand;
    public static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private LocationManager locationManager;
    private String provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mMiBand = new MiBand(this);
        checkLocationPermission();
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
        UserInfo userInfo = new UserInfo(20271234, (byte) 1, (byte) 32, (byte) 160, (byte) 40, "alias", (byte) 0);
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
        mMiBand.setNormalNotifyListener(data -> {
            Timber.d("NormalNotifyListener:" + Arrays.toString(data));
            return Unit.INSTANCE;
        });
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
            return Unit.INSTANCE;
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location permission")
                        .setMessage("Mi Band needs to access your location in order to continue working.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                    locationManager.requestLocationUpdates(provider, 400, 1, this);
                } else {
                    Toast.makeText(this, "Location permission denied. Please grant location permission.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();

        Log.i("Location info: Lat", lat.toString());
        Log.i("Location info: Lng", lng.toString());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // do nothing
    }
}
