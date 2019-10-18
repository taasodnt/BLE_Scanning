package com.example.btle_scanner03;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

//無法再次啟動Service與關閉Notification
public class BLE_ScanningService extends Service {
    private String uriAPI = "http://163.18.53.144/F459/PHP/httpPostTest.php";
    private String[] DEVICE_ADDRESSES = {"20:91:48:21:79:2C","20:91:48:21:7E:65","20:91:48:21:7E:57","20:91:48:21:92:0E","20:91:48:21:88:84","20:91:48:21:47:66"};
    private static int SCANCALLBACK_COUNTER = 0;
    private static final String START_SCANCALLBACK = "START SCAN";
    private static final String STOP_SCANCALLBACK = "STOP SCAN";
//    private static final int SCAN_PERIOD = 4000;
    protected static final int REFRESH_DATA = 0x00000001;
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static boolean SERVICE_STATE_ON = false;

    private static final String TAG = BLE_ScanningService.class.getSimpleName();
    public static final String UPDATE_LIST_ACTION = "UPDATE_LIST_ACTION";
    public static final String DEVICE_LIST = "DEVICE_LIST";
    private static final int FOREGROUND_SERVICE_CHANNEL_ID = 1;
    private static int TEST_COUNTER = 0;

    private List<ScanFilter> bleScanFilter;

    private HashMap<String,BLE_Device> ble_deviceHashMap;
    private ArrayList<String> arrayList = new ArrayList<>();
    private PrintStream printStream;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback bleScanCallback;
    private long scanPeriod;
//    private HandlerThread scanThread;
//    private Handler scanHandler;
    private BroadcastReceiver receiver;
    private WifiManager wifiManager;

    private HashMap<String,BLE_Device> meanValueHashMap;


    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            TEST_COUNTER ++;
            Log.d("TestCounter: ",Integer.toString(TEST_COUNTER));
            String dataString = bluetoothDevice.getAddress()+" "+rssi;
            Log.d(TAG,dataString);
            update(dataString);
        }
    };

//    private void setArrayListAndDeviceListFile() {
//        arrayList.clear();
//
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()) {
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            String name  = tmpDevice.getName();
//            String address = tmpDevice.getAddress();
//            String rssi = Double.toString(tmpDevice.getRssi());
//            String tmpString = "Mac:"+address+"Name:"+name+"Rssi:"+rssi;
//           update(tmpString);
//            Log.d(TAG,tmpString);
//            Log.d(TAG,address+" Debug Counter: "+tmpDevice.getNumberOfDataOfTheDevice());
//            Log.d(TAG,"Sum of rssi: "+tmpDevice.getSumOfRssi());
//            printStream.println(tmpString);
//            arrayList.add(tmpString);
//        }
//        printStream.close();
//    }

    private void setBleScanFilter() {
        bleScanFilter = new ArrayList<>();
        for(String macAddress:DEVICE_ADDRESSES){
            bleScanFilter.add(new ScanFilter.Builder().setDeviceAddress(macAddress).build());
        }
    }

//    private void sendBroadCastToActivity () {
//        Intent broadCastIntent = new Intent();
//        broadCastIntent.setAction(UPDATE_LIST_ACTION);
//        broadCastIntent.putExtra(DEVICE_LIST,arrayList);
//        sendBroadcast(broadCastIntent);
//    }
//
//    private void showDetail() {
//        arrayList.clear();
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()) {
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            String name  = tmpDevice.getName();
//            String address = tmpDevice.getAddress();
//            String rssi = Double.toString(tmpDevice.getRssi());
//            String tmpString = "Mac:"+address+"Name:"+name+"Rssi:"+rssi;
//            Log.d(TAG,"FromZ showDetail: "+tmpString+address+"  Debug Counter: "+tmpDevice.getNumberOfDataOfTheDevice()+"  Sum of rssi: "+tmpDevice.getSumOfRssi());
//            arrayList.add(tmpString);
//        }
//        Log.d(TAG,"FromZ showDetail:  End");
//    }
//
//    private void sendData(){
//        Log.d(TAG,"SendData get called");
//        if(ble_deviceHashMap.isEmpty()){
//            Log.d(TAG,"BLE_HashMap is empty!");
//            return;
//        }
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()){
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            double rssiMeanValue = tmpDevice.getRssiMeanValue();
//               String name  = tmpDevice.getName();
//               String address = tmpDevice.getAddress();
//               String numberOfDataOfTheDevice = Double.toString(tmpDevice.getNumberOfDataOfTheDevice());
//               String sumOfRssi = Double.toString(tmpDevice.getSumOfRssi());
//               String tmpString = "Mac:"+address+" Name:"+name+" Rssi:"+rssiMeanValue+"  NumberOfDataOfTheDevice:"+numberOfDataOfTheDevice+"  Sum Of Rssi:"+sumOfRssi;
//               Log.d(TAG,"From sendData:  Mean Value: "+tmpString);
//               update(tmpString);
//        }
//        Log.d(TAG,"FromZ sendData:  End");
//    }
//    private void setBle_deviceHashMap(BluetoothDevice device,int rssi){
//        String address = device.getAddress();
//        if(!ble_deviceHashMap.containsKey(address)) {
//            BLE_Device myDevice = new BLE_Device(device,rssi);
//            ble_deviceHashMap.put(address,myDevice);
//        }else{
//            ble_deviceHashMap.get(address).setRssi(rssi*1.0);
//        }
//    }

    private ScanCallback generateScanCallback() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            ScanCallback scanCallback;
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                    super.onScanResult(callbackType, result);
                    SCANCALLBACK_COUNTER++;
                    String tmpString = result.getDevice().getAddress() + " " + result.getRssi();
                    update(tmpString);
                    Log.d(TAG,tmpString);
                    Log.d(TAG, "ScanCallbackCounter: " + SCANCALLBACK_COUNTER);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.d(TAG,"Scanning Failed Error Code:"+errorCode);
                }
            };
            return scanCallback;
        }else{
            return null;
        }
    }



    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if(bluetoothLeScanner == null){
            stopSelf();
        }
        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        meanValueHashMap = new HashMap<>();
        SERVICE_STATE_ON = true;
//        scanPeriod = SCAN_PERIOD;
        ble_deviceHashMap = new HashMap<>();
        try {
            printStream = new PrintStream(openFileOutput("DeviceList.txt",MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            Log.d(TAG,"Output Error!");
            e.printStackTrace();
        }
        setBleScanFilter();

        IntentFilter scanWifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,"Receiver get called");
                String action = intent.getAction();
                if(action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION){
                    if(wifiManager.getScanResults().size() != 0) {
                        for(ScanResult result: wifiManager.getScanResults()) {
                            //TODO 回傳wifi掃描結果
//                            String SSID = result.SSID;
//                            Log.d(TAG,SSID);
                        }
                    }else{
                        Log.d(TAG,"Result is empty");
                    }
                    Log.d(TAG,"Receiver is finishing");
//                    wifiManager.startScan();
                }
            }
        };
//        registerReceiver(receiver,scanWifiFilter);

        bleScanCallback = generateScanCallback();

        Log.d(TAG,"Service on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//            wifiManager.startScan();
//        bluetoothAdapter.startLeScan(leScanCallback);
        bluetoothLeScanner.startScan(bleScanFilter,generateScanSetting(),bleScanCallback);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
//        unregisterReceiver(receiver);
//        bluetoothAdapter.stopLeScan(leScanCallback);
        bluetoothLeScanner.stopScan(bleScanCallback);
        Log.d(TAG,"Service is stop");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void startForegroundService() {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,App.CHANNEL_ID);
        builder.setContentTitle("Scanning notification");
        builder.setContentTitle("Scanning Service");
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_android);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(FOREGROUND_SERVICE_CHANNEL_ID,notification);
    }

    private ScanSettings generateScanSetting(){
        ScanSettings.Builder scanSettingBuilder = new ScanSettings.Builder();
        scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        scanSettingBuilder.setReportDelay(0);
        return scanSettingBuilder.build();
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                // 顯示網路上抓取的資料
                case REFRESH_DATA:
                    String result = null;
                    if (msg.obj instanceof String)
                        result = (String) msg.obj;
                    if(result==null)
                        Toast.makeText(BLE_ScanningService.this,"資料庫連接失敗\n請檢查您的連線狀態",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void update(final String data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String mac=getMacAddress(getApplicationContext());
                String num2= mac+" "+data;
                String result = sendPostDataToInternet(num2);
                if(result == null) {
                    Log.d("ouo","Result is Null Pointer");
                }else{
                    Log.d("ouo",result);
                }
                mHandler.obtainMessage(REFRESH_DATA, result).sendToTarget();
            }

        }).start();

    }


    private String sendPostDataToInternet(String strTxt) {
        int state;
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpRequest = new HttpPost(uriAPI);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("data", strTxt));

        try {
            Log.d("ouo","Try ok");
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            state = httpResponse.getStatusLine().getStatusCode();
            Log.d("ouo",Integer.toString(state));
            //狀態碼為200的狀況才會執行
            if (state == 200) {
                Log.d("ouo","if ok");
                String strResult = EntityUtils.toString(httpResponse.getEntity());

                return strResult;
            }
        } catch (Exception e) {
            Log.d("ouo","exception ok");
            e.printStackTrace();
        }
        return null;


    }

    //拿取MacAddress
    public static String getMacAddress(Context context) {
        String mac = "02:00:00:00:00:00";
        //判斷Android版本為6.0之前
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mac = getMacDefault(context);
            Log.d("ouo",mac);
            //判斷Android版本為6.0~7.0之間
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mac = getMacAddress();
            Log.d("ouo",mac);
            //判斷Android版本為7.0以上
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mac = getMacFromHardware();
            Log.d("ouo",mac);
        }
        return mac;
    }

    //Android6.0以下
    private static String getMacDefault(Context context) {
        String mac = "02:00:00:00:00:00";
        if (context == null) {
            return mac;
        }

        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return mac;
        }
        WifiInfo info = null;
        try {
            info = wifi.getConnectionInfo();
        } catch (Exception e) {
        }
        if (info == null) {
            return null;
        }
        mac = info.getMacAddress();
        if (!TextUtils.isEmpty(mac)) {
            mac = mac.toUpperCase(Locale.ENGLISH);
        }
        return mac;
    }

    //Android 6.0~7.0
    private static String getMacAddress() {
        String WifiAddress = "02:00:00:00:00:00";
        try {
            WifiAddress = new BufferedReader(new FileReader(new File("/sys/class/net/wlan0/address"))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WifiAddress;
    }

    //Android 7.0以上
    private static String getMacFromHardware() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0"))
                    continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }


}