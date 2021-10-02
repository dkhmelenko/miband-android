package com.khmelenko.lab.mibanddemo

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import butterknife.ButterKnife
import butterknife.OnClick
import com.khmelenko.lab.miband.MiBand
import com.khmelenko.lab.miband.listeners.HeartRateNotifyListener
import com.khmelenko.lab.miband.listeners.RealtimeStepsNotifyListener
import com.khmelenko.lab.miband.model.UserInfo
import com.khmelenko.lab.miband.model.VibrationMode
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Main application activity
 *
 * @author Dmytro Khmelenko
 */
class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var miBand: MiBand
    private lateinit var locationManager: LocationManager
    private lateinit var provider: String

    private val disposables = CompositeDisposable()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        miBand = MiBand(this)
        provider = LocationManager.GPS_PROVIDER
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkLocationPermission()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    @OnClick(R.id.action_connect)
    fun actionConnect() {
        val device = intent.getParcelableExtra<BluetoothDevice>("device")

        val d = miBand.connect(device!!)
                .subscribe({ result ->
                    Timber.d("Connect onNext: $result")
                }, { throwable ->
                    throwable.printStackTrace()
                    Timber.e(throwable)
                })
        disposables.add(d)
    }

    @OnClick(R.id.action_pair)
    fun actionPair() {
        val d = miBand.pair().subscribe(
                { Timber.d("Pairing successful") },
                { throwable -> Timber.e(throwable, "Pairing failed") })
        disposables.add(d)
    }

    @OnClick(R.id.action_show_services)
    fun actionShowServices() {
        // TODO miband.showServicesAndCharacteristics();
    }

    @OnClick(R.id.action_read_rssi)
    fun actionReadRssi() {
        val d = miBand.readRssi()
                .subscribe({ rssi -> Timber.d("rssi:$rssi") },
                        { throwable -> Timber.e(throwable, "readRssi fail") },
                        { Timber.d("Rssi onCompleted") })
        disposables.add(d)
    }

    @OnClick(R.id.action_battery_info)
    fun actionBatteryInfo() {
        val d = miBand.batteryInfo
                .subscribe({ batteryInfo -> Timber.d(batteryInfo.toString()) },
                        { throwable -> Timber.e(throwable, "getBatteryInfo fail") })
        disposables.add(d)
    }

    @OnClick(R.id.action_set_user_info)
    fun actionSetUserInfo() {
        val userInfo = UserInfo(20271234, 1.toByte(), 32.toByte(), 160.toByte(), 40.toByte(), "alias", 0.toByte())
        Timber.d("setUserInfo: $userInfo data: ${userInfo.getBytes(miBand.device!!.address).contentToString()}")

        val d = miBand.setUserInfo(userInfo)
                .subscribe({ Timber.d("setUserInfo success") },
                        { throwable -> Timber.e(throwable, "setUserInfo failed") })
        disposables.add(d)
    }

    @OnClick(R.id.action_set_heart_rate_notify_listener)
    fun actionSetHeartRateNotifyListener() {
        miBand.setHeartRateScanListener(object : HeartRateNotifyListener {
            override fun onNotify(heartRate: Int) {
                Timber.d("heart rate: $heartRate")
            }
        })
    }

    @OnClick(R.id.action_start_heart_rate_scan)
    fun actionStartHeartRateScan() {
        val d = miBand.startHeartRateScan().subscribe()
        disposables.add(d)
    }

    @OnClick(R.id.action_start_vibro_with_led)
    fun actionStartVibroWithLed() {
        val d = miBand.startVibration(VibrationMode.VIBRATION_WITH_LED)
                .subscribe { Timber.d("Vibration started") }
        disposables.add(d)
    }

    @OnClick(R.id.action_start_vibro)
    fun actionStartVibro() {
        val d = miBand.startVibration(VibrationMode.VIBRATION_WITHOUT_LED)
                .subscribe { Timber.d("Vibration started") }
        disposables.add(d)
    }

    @OnClick(R.id.action_start_vibro_with_led_time)
    fun actionStartVibroWithLedAndTime() {
        val d = miBand.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED)
                .subscribe { Timber.d("Vibration started") }
        disposables.add(d)
    }

    @OnClick(R.id.action_stop_vibro)
    fun actionStopVibration() {
        val d = miBand.stopVibration()
                .subscribe { Timber.d("Vibration stopped") }
        disposables.add(d)
    }

    @OnClick(R.id.action_set_notify_listener)
    fun actionSetNotifyListener() {
        miBand.setNormalNotifyListener { data ->
            Timber.d("NormalNotifyListener: ${data.contentToString()}")
        }
    }

    @OnClick(R.id.action_set_realtime_notify_listener)
    fun actionSetRealtimeNotifyListener() {
        miBand.setRealtimeStepsNotifyListener(object : RealtimeStepsNotifyListener {
            override fun onNotify(steps: Int) {
                Timber.d("RealtimeStepsNotifyListener:$steps")
            }
        })
    }

    @OnClick(R.id.action_enable_realtime_steps_notify)
    fun actionEnableRealtimeStepsNotify() {
        val d = miBand.enableRealtimeStepsNotify().subscribe()
        disposables.add(d)
    }

    @OnClick(R.id.action_disable_realtime_steps_notify)
    fun actionDisableRealtimeStepsNotify() {
        val d = miBand.disableRealtimeStepsNotify().subscribe()
        disposables.add(d)
    }

    @OnClick(R.id.action_set_sensor_data_notify_listener)
    fun actionSetSensorDataNotifyListener() {
        miBand.setSensorDataNotifyListener { data ->
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            var i = 0

            val index = data[i++].toInt() and 0xFF or (data[i++].toInt() and 0xFF shl 8)
            val d1 = data[i++].toInt() and 0xFF or (data[i++].toInt() and 0xFF shl 8)
            val d2 = data[i++].toInt() and 0xFF or (data[i++].toInt() and 0xFF shl 8)
            val d3 = data[i++].toInt() and 0xFF or (data[i++].toInt() and 0xFF shl 8)

            val logMsg = "$index , $d1 , $d2 , $d3"
            Timber.d(logMsg)
        }
    }

    @OnClick(R.id.action_enable_sensor_data_notify)
    fun actionEnableSensorDataNotify() {
        val d = miBand.enableSensorDataNotify().subscribe()
        disposables.add(d)
    }

    @OnClick(R.id.action_disable_sensor_data_notify)
    fun actionDisableSensorDataNotify() {
        val d = miBand.disableSensorDataNotify().subscribe()
        disposables.add(d)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                        .setTitle("Location permission")
                        .setMessage("Mi Band needs to access your location in order to continue working.")
                        .setPositiveButton("OK") { _, i ->
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(this@MainActivity,
                                    arrayOf(ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
                        }
                        .create()
                        .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                    locationManager.requestLocationUpdates(provider, 400, 1f, this)
                } else {
                    Toast.makeText(this, "Location permission denied. Please grant location permission.",
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 400, 1f, this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this)
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lng = location.longitude

        Timber.i("Location info: Lat $lat")
        Timber.i("Location info: Lng $lng")
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // do nothing
    }

    override fun onProviderEnabled(provider: String) {
        // do nothing
    }

    override fun onProviderDisabled(provider: String) {
        // do nothing
    }

    companion object {
        const val PERMISSIONS_REQUEST_LOCATION = 99
    }
}
