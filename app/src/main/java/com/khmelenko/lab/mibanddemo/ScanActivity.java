package com.khmelenko.lab.mibanddemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.khmelenko.lab.miband.MiBand;

import java.util.ArrayList;
import java.util.HashMap;

import rx.functions.Action1;
import timber.log.Timber;

public class ScanActivity extends Activity {
    private MiBand miband;

    HashMap<String, BluetoothDevice> devices = new HashMap<>();
    private ArrayAdapter<String> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        miband = new MiBand(this);

        mAdapter = new ArrayAdapter<>(this, R.layout.item, new ArrayList<>());

        Button startScanButton = (Button) findViewById(R.id.starScanButton);
        startScanButton.setOnClickListener(v -> {
            Timber.d("Scanning started...");
            miband.startScan()
                    .subscribe(handleScanResult(),
                            Throwable::printStackTrace);
        });

        findViewById(R.id.stopScanButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("Stop scanning...");
                miband.stopScan().subscribe(handleScanResult());
            }
        });


        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String item = ((TextView) view).getText().toString();
            if (devices.containsKey(item)) {

                miband.stopScan().subscribe(handleScanResult());

                BluetoothDevice device = devices.get(item);
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
    private Action1<ScanResult> handleScanResult() {
        return result -> {
            BluetoothDevice device = result.getDevice();
            Timber.d("Scan results: name:" + device.getName() + ",uuid:"
                    + device.getUuids() + ",add:"
                    + device.getAddress() + ",type:"
                    + device.getType() + ",bondState:"
                    + device.getBondState() + ",rssi:" + result.getRssi());

            String item = device.getName() + "|" + device.getAddress();
            if (!devices.containsKey(item)) {
                devices.put(item, device);
                mAdapter.add(item);
            }
        };
    }
}
