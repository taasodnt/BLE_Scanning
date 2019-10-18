package com.example.btle_scanner03;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.btle_scanner03.BLE_ScanningService.SERVICE_STATE_ON;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
//    private static final int MY_REQUEST_CODE = 3;

    private boolean serviceState;
    private Intent serviceIntent;
    private MyReceiver myReceiver;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> arrayAdapter;

    private LocationManager locationManager;


    private BluetoothAdapter bluetoothAdapter;
    private WifiManager wifiManager;

    private ListView deviceListView;
    private Button startAndStopBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission();
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,REQUEST_CODE);
            }
        }else{
            finish();
        }
        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager == null){
            Toast.makeText(this, "This device doesn't support wifi service", Toast.LENGTH_SHORT).show();
            finish();
        }

        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        if(locationManager == null){
            Toast.makeText(this, "This device doesn't support location service", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            boolean gpsProvider,networkProvider;
            gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(!gpsProvider && !networkProvider){
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(" Turn on Location Service")
                        .setMessage("The app needs location service")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(locationIntent);
                            }
                        })
                        .show();
            }
        }
//        Intent enableWifiIntent = new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);
//        startActivity(enableWifiIntent);

        serviceIntent = new Intent(this,BLE_ScanningService.class);

        serviceState = false;
        startAndStopBtn = (Button) findViewById(R.id.startAndStop);
        deviceListView = (ListView) findViewById(R.id.list_item);

        myReceiver = new MyReceiver();
        deviceList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceList);
        deviceListView.setAdapter(arrayAdapter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_ScanningService.UPDATE_LIST_ACTION);
        registerReceiver(myReceiver,intentFilter);
    }

    private void requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("These permission is needed for scan wifi and BLE signal")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_FINE_LOCATION);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_FINE_LOCATION);
        }
    }



    @Override
    protected void onDestroy() {
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }


    public void startAndStop(View view) {
        if(serviceState == false){
            serviceState = true;
            Log.d(TAG,"Button Ok1");
            startService(serviceIntent);

        }else{
            serviceState = false;

            stopService(serviceIntent);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CODE:
                Log.d("ouob",Integer.toString(resultCode));
                if(resultCode == RESULT_CANCELED) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ouob","coarse location permission granted");
                }else{
                    finish();
                }
                return;
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> arrayList = intent.getStringArrayListExtra(BLE_ScanningService.DEVICE_LIST);
            deviceList.clear();
            deviceList.addAll(arrayList);
            arrayAdapter.notifyDataSetChanged();
        }
    }
}