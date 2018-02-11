package com.khmelenko.lab.mibanddemo

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.khmelenko.lab.miband.MiBand
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import timber.log.Timber
import java.util.*

/**
 * Scanner activity
 *
 * @author Dmytro Khmelenko
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var miBand: MiBand

    private val devices = HashMap<String, BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activity)

        miBand = MiBand(this)
        adapter = ArrayAdapter(this, R.layout.item, ArrayList())

        val startScanButton = findViewById<Button>(R.id.starScanButton)
        startScanButton.setOnClickListener { _ ->
            Timber.d("Scanning started...")
            val disposable = miBand.startScan()
                    .subscribe(handleScanResult(), handleScanError())
            disposables.add(disposable)
        }

        findViewById<Button>(R.id.stopScanButton).setOnClickListener { _ ->
            Timber.d("Stop scanning...")
            val disposable = miBand.stopScan().subscribe(handleScanResult(), handleScanError())
            disposables.add(disposable)
        }

        val lv = findViewById<ListView>(R.id.listView)
        lv.adapter = adapter
        lv.setOnItemClickListener { parent, view, position, id ->
            val item = (view as TextView).text.toString()
            if (devices.containsKey(item)) {

                val disposable = miBand.stopScan().subscribe(handleScanResult(), handleScanError())
                disposables.add(disposable)

                val intent = Intent(this@ScanActivity, MainActivity::class.java)
                intent.putExtra("device", devices[item])
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun handleScanResult(): Consumer<ScanResult> {
        return Consumer { result ->
            val device = result.getDevice()
            Timber.d("Scan results: name: ${device.name} uuid: ${Arrays.toString(device.uuids)}, add:"
                    + " ${device.address}, type: ${device.type} bondState: ${device.bondState}, rssi: ${result.rssi}")

            val item = device.name + "|" + device.address
            if (!devices.containsKey(item)) {
                devices.put(item, device)
                adapter.add(item)
            }
        }
    }

    private fun handleScanError(): Consumer<Throwable> {
        return Consumer { throwable ->
            Timber.e(throwable)
        }
    }
}
