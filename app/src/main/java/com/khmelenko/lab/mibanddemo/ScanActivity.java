package com.khmelenko.lab.mibanddemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.khmelenko.lab.miband.MiBand;

import java.util.ArrayList;
import java.util.HashMap;

public class ScanActivity extends Activity {
    private static final String TAG = "==[mibandtest]==";
    private MiBand miband;

    HashMap<String, BluetoothDevice> devices = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);

        miband = new MiBand(this);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item, new ArrayList<>());

        final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {


            }
        };

        Button startScanButton = (Button) findViewById(R.id.starScanButton);
        startScanButton.setOnClickListener(v -> {
            Log.d(TAG, "Scanning started...");
            miband.startScan()
                    .subscribe(result -> {
                                BluetoothDevice device = result.getDevice();
                                Log.d(TAG, "Scan results: name:" + device.getName() + ",uuid:"
                                        + device.getUuids() + ",add:"
                                        + device.getAddress() + ",type:"
                                        + device.getType() + ",bondState:"
                                        + device.getBondState() + ",rssi:" + result.getRssi());

                                String item = device.getName() + "|" + device.getAddress();
                                if (!devices.containsKey(item)) {
                                    devices.put(item, device);
                                    adapter.add(item);
                                }
                            },
                            throwable -> {
                                throwable.printStackTrace();
                            });
        });

        findViewById(R.id.stopScanButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Stop scanning...");
                MiBand.stopScan(scanCallback);
            }
        });


        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString();
                if (devices.containsKey(item)) {

                    MiBand.stopScan(scanCallback);

                    BluetoothDevice device = devices.get(item);
                    Intent intent = new Intent();
                    intent.putExtra("device", device);
                    intent.setClass(ScanActivity.this, MainActivity.class);
                    ScanActivity.this.startActivity(intent);
                    ScanActivity.this.finish();
                }
            }
        });

    }
}
