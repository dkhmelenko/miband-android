package com.khmelenko.lab.mibanddemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.khmelenko.lab.miband.MiBand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.reactivex.functions.Consumer;
import timber.log.Timber;

/**
 * Scanner activity
 *
 * @author Dmytro Khmelenko
 */
public class ScanActivity extends Activity {

    private MiBand mMiBand;

    private HashMap<String, BluetoothDevice> mDevices = new HashMap<>();
    private ArrayAdapter<String> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        mMiBand = new MiBand(this);

        mAdapter = new ArrayAdapter<>(this, R.layout.item, new ArrayList<>());

        Button startScanButton = (Button) findViewById(R.id.starScanButton);
        startScanButton.setOnClickListener(v -> {
            Timber.d("Scanning started...");
            mMiBand.startScan()
                    .subscribe(handleScanResult(),
                            Throwable::printStackTrace);
        });

        findViewById(R.id.stopScanButton).setOnClickListener(v -> {
            Timber.d("Stop scanning...");
            mMiBand.stopScan()
                    .subscribe(handleScanResult());
        });


        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String item = ((TextView) view).getText().toString();
            if (mDevices.containsKey(item)) {

                mMiBand.stopScan()
                        .subscribe(handleScanResult());

                BluetoothDevice device = mDevices.get(item);
                Intent intent = new Intent();
                intent.putExtra("device", device);
                intent.setClass(ScanActivity.this, MainActivity.class);
                ScanActivity.this.startActivity(intent);
                ScanActivity.this.finish();
            }
        });
    }

    /**
     * Handles the result of scanning
     *
     * @return Action handler
     */
    private Consumer<ScanResult> handleScanResult() {
        return result -> {
            BluetoothDevice device = result.getDevice();
            Timber.d("Scan results: name:" + device.getName() + ",uuid:"
                    + Arrays.toString(device.getUuids()) + ",add:"
                    + device.getAddress() + ",type:"
                    + device.getType() + ",bondState:"
                    + device.getBondState() + ",rssi:" + result.getRssi());

            String item = device.getName() + "|" + device.getAddress();
            if (!mDevices.containsKey(item)) {
                mDevices.put(item, device);
                mAdapter.add(item);
            }
        };
    }
}
