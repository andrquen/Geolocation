package net.wifi.geolocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import android.provider.Settings;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;


public class MainActivity extends AppCompatActivity {
    WifiManager wifiManager;
    List<Integer> scannedlevel;
    List<String> scannedBSSID;
    List<String> scannedResult;
    TextView textView;
    String bssid = "bssid";
    String name = "network";
    String bestlat;
    String bestlon;
    DatabaseHelper databaseHelper;
    SQLiteDatabase db;
    int n = 0;
    private MapView mMapView;
    private MapController mMapController;
    private Thread t1;
    //private int mInterval = 5000; // 5 seconds by default, can be changed later
    //private Handler mHandler;
    //Location location;
    //Context context;

    public boolean canGetLocation() {
        LocationManager lm;
        boolean networkEnabled = false;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            networkEnabled = lm
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        return networkEnabled;
    }

    public void showSettingsAlert() {

        Intent intent = new Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
        //Toast.makeText(this,"Turn on location and Wi-Fi scanning",Toast.LENGTH_LONG).show();

    }

    private void setMock(double latitude, double longitude /*,float accuracy*/) {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locMgr.addTestProvider(LocationManager.GPS_PROVIDER,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    android.location.Criteria.POWER_LOW,
                    Criteria.ACCURACY_HIGH);

        }

        Location newLocation = new Location(LocationManager.GPS_PROVIDER);

        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAltitude(0);
        newLocation.setAccuracy(1);
        newLocation.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        locMgr.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        locMgr.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null, System.currentTimeMillis());

        locMgr.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark = prefs.getBoolean("dark", false);
        boolean locate = prefs.getBoolean("locate", false);
        boolean checklocation = prefs.getBoolean("checklocation", false);

        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        } else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        Configuration.getInstance().setUserAgentValue("a");
        textView = findViewById(R.id.textView2);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        scannedlevel = new ArrayList<>();
        scannedBSSID = new ArrayList<>();
        scannedResult = new ArrayList<>();

        databaseHelper = new DatabaseHelper(getApplicationContext());
        databaseHelper.create_db();


        mMapView = findViewById(R.id.mapview);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        if (dark)
            mMapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(3);
        if (!wifiManager.isWifiEnabled() && !locate) {
            wifiManager.setWifiEnabled(true);
            textView.setText(R.string.please_wait);
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            textView.setText(R.string.nope);
                        }
                    },
                    5000
            );

        }

        String time_locate = prefs.getString("list_preference", "1");
        assert time_locate != null;
        if (time_locate.equals("1") && locate && (canGetLocation() || checklocation)) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                textView.setText(R.string.please_wait);
                t1 = new Thread(new Task1());
                t1.start();
            } else scan();

        }
        if (!time_locate.equals("1") && locate && (canGetLocation() || checklocation)) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                textView.setText(R.string.please_wait);
                if (time_locate.equals("2")) {
                    t1 = new Thread(new Task1_2());
                    t1.start();
                }
                if (time_locate.equals("3")) {
                    t1 = new Thread(new Task1_3());
                    t1.start();
                }
                if (time_locate.equals("4")) {
                    t1 = new Thread(new Task1_4());
                    t1.start();
                }
            } else {
                if (time_locate.equals("2")) {
                    scan();
                    t1 = new Thread(new Task());
                    t1.start();
                }
                if (time_locate.equals("3")){
                    scan();
                    t1 = new Thread(new Task2());
                    t1.start();
                }
                if (time_locate.equals("4")){
                    scan();
                    t1 = new Thread(new Task3());
                    t1.start();
                }
            }
        } else {
            if (locate && !canGetLocation()) {
                showSettingsAlert();
            }
        }
            /*if(!time_locate.equals("1") &&locate && (canGetLocation()||checklocation)) {
               synchronized (this) {
                   try {
                       scanbyint(time_locate);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
            }*/


    }

    public boolean isMockLocationEnabled() {
        boolean isMockLocation;
        try {
            //if marshmallow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID) == AppOpsManager.MODE_ALLOWED);
            } else {
                // in marshmallow this will always return true
                isMockLocation = !android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "mock_location").equals("0");
            }
        } catch (Exception e) {
            return false;
        }
        return isMockLocation;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);

    }
        /*public void scanbyint(final String s) throws InterruptedException {
            if (s.equals("2")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scan();
                        try {
                            wait(15000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            scanbyint(s);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });


            }
            if (s.equals("3")){
                synchronized(this) {
                    scan();
                    wait(30000);
                    scanbyint(s);
                }
            }
            if (s.equals("4")){
                synchronized (this) {
                    scan();
                    wait(60000);
                    scanbyint(s);
                }
            }
        }*/


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) startActivity(new Intent(this, SettingsActivity.class));
        if (id == R.id.help) startActivity(new Intent(this, HelpActivity.class));
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("StaticFieldLeak")
    public void scan() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checklocation = prefs.getBoolean("checklocation", false);
        if (scannedResult.size() > 0 | scannedlevel.size() > 0) {
            scannedResult.clear();
            scannedlevel.clear();
        }
        if (canGetLocation() || checklocation) {
            try {
                db = databaseHelper.open();
                //Toast.makeText(getApplicationContext(), "Locating...", Toast.LENGTH_SHORT).show();
                new AsyncTask<Void, String, String>() {
                    @Override
                    protected String doInBackground(Void... voids) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        final boolean scan = prefs.getBoolean("scanning", false);
                        //Log.d("scan", "1");
                        if (scan) wifiManager.startScan();
                        List<ScanResult> scanResultList = wifiManager.getScanResults();
                        //Log.d("scanned", scanResultList.toString());
                        if (!scanResultList.isEmpty()) {
                            for (int i = 0; i < scanResultList.size(); i++) {
                                String a = scanResultList.get(i).BSSID;

                                 Cursor resultSet = db.rawQuery("SELECT bestlat, bestlon FROM " + name + " WHERE "
                                        + bssid + " = " + "'" + a + "'", null);
                                 //resultSet.requery();
                                if (resultSet.moveToFirst()) {
                                    bestlat = resultSet.getString(resultSet.getColumnIndex("bestlat"));
                                    bestlon = resultSet.getString(resultSet.getColumnIndex("bestlon"));
                                }
                                resultSet.close();
                                if (bestlat != null && bestlon != null) {
                                    scannedResult.add(bestlat + "," + bestlon);
                                    scannedlevel.add(scanResultList.get(i).level);
                                }
                            }
                            float x1 = 0;
                            float y1 = 0;
                            float x2 = 0;
                            float y2 = 0;
                            for (int i = 0; i < scannedResult.size(); i++) {
                                String[] bestcords = scannedResult.get(i).split(",");

                                x1 += (float) (Float.parseFloat(bestcords[0]));

                                y1 += (float) (Float.parseFloat(bestcords[1]));


                            }
                            /*Log.d("x1", String.valueOf(x1));
                            Log.d("y1", String.valueOf(y1));*/
                            x1 /= scannedResult.size();
                            y1 /= scannedResult.size();
                            /*Log.d("x1_1", String.valueOf(x1));
                            Log.d("y1_1", String.valueOf(y1));*/
                            for (int i = 0; i < scanResultList.size(); i++){
                                String[] bestcords = scannedResult.get(i).split(",");

                                x2 += (float) (Math.pow(Float.parseFloat(bestcords[0])-x1,2));
                                y2 += (float) (Math.pow(Float.parseFloat(bestcords[1])-y1,2));
                            }
                            /*Log.d("x2", String.valueOf(x2));
                            Log.d("y2", String.valueOf(y2));*/
                            x2 /= scannedResult.size()-1;
                            y2 /= scannedResult.size()-1;
                            /*Log.d("x2_2", String.valueOf(x2));
                            Log.d("y2_2", String.valueOf(y2));*/
                            for (int i = 0; i < scannedResult.size(); i++) {

                                    String[] bestcords = scannedResult.get(i).split(",");
                                if (abs(Float.parseFloat(bestcords[0]) - x1) > sqrt(x2) || abs(Float.parseFloat(bestcords[0]) - y1) > sqrt(y2)) {
                                    //Log.d("remove","OK");
                                    scannedResult.remove(i);
                                    scannedlevel.remove(i);
                                }

                            }

                            if (!scannedResult.isEmpty() && !scannedlevel.isEmpty()) {
                                float x = 0;
                                float y = 0;
                                float w = 0;
                                for (int n = 0; n < scannedResult.size(); n++) {
                                    String[] bestcords = scannedResult.get(n).split(",");
                                    x += (float) (Float.parseFloat(bestcords[0]) * Math.pow(10, 0.05 * Float.parseFloat(scannedlevel.get(n).toString())));
                                    y += (float) (Float.parseFloat(bestcords[1]) * Math.pow(10, 0.05 * Float.parseFloat(scannedlevel.get(n).toString())));
                                    w += Math.pow(10, 0.05 * Float.parseFloat(scannedlevel.get(n).toString()));
                                }
                                x /= w;
                                y /= w;
                                final float finalX = x;
                                final float finalY = y;
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        setmMapController(finalX, finalY);
                                    }
                                });
                                boolean mock = prefs.getBoolean("mock", false);
                                if (mock && isMockLocationEnabled()) {
                                    try {
                                       /*
                                        final float finalX1 = x;
                                        final float finalY1 = y;
                                        Thread t1 = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    setMock(finalX1, finalY1);
                                                    Thread.sleep(5000);
                                                    scan();
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        });
                                        t1.start();*/
                                        setMock(x, y);
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), "Can't set mock location", LENGTH_SHORT).show();
                                    }
                                }
                                    /*}
                                }*/
                                return (x) + ", " + (y);
                            } else return "No Wi-Fi networks from DB";
                        } else return "No Wi-Fi networks generally";
                    }
                    @Override
                    protected void onPostExecute(String s) {
                        textView.setText(s);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        boolean marker = prefs.getBoolean("marker", false);
                        if (marker) newPoint(null);
                        //setTime_locate();
                    }
                }.execute();
            } catch (SQLiteException sqle) {
                Toast.makeText(this, "Couldn't open database", Toast.LENGTH_LONG).show();
                textView.setText(R.string.try_again);
            }
        } else showSettingsAlert();
    }

    /*void bluetoothScanning(){

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startDiscovery();

    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Cursor resultSet = db.rawQuery("SELECT bestlat, bestlon FROM " + name + " WHERE "
                        + bssid + " = " + "'" + deviceHardwareAddress + "'", null);
                if (resultSet != null && resultSet.moveToFirst()) {
                    if (!bestlat.equals(null) && !bestlon.equals(null)) {
                        bestlat = resultSet.getString(resultSet.getColumnIndex("bestlat"));
                        bestlon = resultSet.getString(resultSet.getColumnIndex("bestlon"));
                        resultSet.close();
                    }

                }
                //Log.i("deviceHardwareAddress " , "hard"  + deviceHardwareAddress+bestlat+bestlon);
            }
        }
    };*/
    public void setmMapController(float x, float y) {
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(18);
        GeoPoint gPt = new GeoPoint(x, y);
        mMapController.setCenter(gPt);
    }

    public void newPoint(View view) {
        CharSequence myStr = textView.getText();
        if (!myStr.equals(getString(R.string.nope)) && !myStr.equals("Get your location first.") &&
                !myStr.equals("No Wi-Fi networks generally") &&
                !myStr.equals("Wait and try again") &&
                !myStr.equals("No Wi-Fi networks from DB") &&
                !myStr.equals("Please wait…") && !myStr.equals("Couldn't open database")) {
            try {
                String[] bestcords = textView.getText().toString().split(",");
                GeoPoint startPoint = new GeoPoint(Float.parseFloat(bestcords[0]), Float.parseFloat(bestcords[1]));
                Marker startMarker = new Marker(mMapView);
                startMarker.setPosition(startPoint);
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mMapView.getOverlays().add(startMarker);
            } catch (NullPointerException npe) {
                Toast.makeText(this, "NullPointerException", Toast.LENGTH_LONG).show();
            }
        } else {
            textView.setText(R.string.get_location_first);
        }
    }

    public void maps(View view) {
        CharSequence myStr = textView.getText();
        if (!myStr.equals(getString(R.string.nope)) && !myStr.equals("Get your location first.") &&
                !myStr.equals("No Wi-Fi networks generally") &&
                !myStr.equals("Wait and try again") &&
                !myStr.equals("No Wi-Fi networks from DB") &&
                !myStr.equals("Please wait…") && !myStr.equals("Couldn't open database")) {
            try {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?q=" + textView.getText().toString()));
                startActivity(intent);
            } catch (NullPointerException npe) {
                Toast.makeText(this, "NullPointerException", Toast.LENGTH_LONG).show();
            }
        } else {
            textView.setText(R.string.get_location_first);
        }
    }

    public void doProcess(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }
        Toast.makeText(getApplicationContext(), "Locating...", LENGTH_SHORT).show();
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            textView.setText(R.string.please_wait);
            Thread t1 = new Thread(new Task());
            t1.start();
        } else {
            scan();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // String scan = prefs.getString("list_preference_1", "1");
        if (requestCode == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Locating...", LENGTH_SHORT).show();
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                textView.setText(R.string.please_wait);
                Thread t1 = new Thread(new Task());
                t1.start();
            } else scan();
            // if (scan.equals("1"))
            //  if (scan.equals("2"))bluetoothScanning();


            // if (scan.equals("1"))
            // if (scan.equals("2"))bluetoothScanning();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        //try {

        if (db!=null)db.close();

        if (t1!=null)t1.interrupt();

            //stopRepeatingTask();
        //} catch (NullPointerException ignored) {

        //}


    }

    class Task1 implements Runnable {
        public void run() {
            try {
                if(!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5000);
                    scan();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

    class Task1_2 implements Runnable {

        public void run() {

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if (n == 0) {
                        Thread.sleep(5000);
                        n = 1;
                    } else {
                        Thread.sleep(15000);
                    }
                    scan();
                   // run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

    class Task1_3 implements Runnable {
        public void run() {

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if (n == 0) {
                        Thread.sleep(5000);
                        n = 1;
                    } else {
                        Thread.sleep(30000);
                    }
                    scan();
                    //run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

    class Task1_4 implements Runnable {
        public void run() {

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if (n == 0) {
                        Thread.sleep(5000);
                        n = 1;
                    } else {
                        Thread.sleep(60000);
                    }
                    scan();
                    //run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

    class Task implements Runnable {
        public void run() {

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(15000);
                    scan();
                    //run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }


    }

    class Task2 implements Runnable {
        public void run() {

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000);
                    scan();
                    //run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
   /* public void setTime_locate(){
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String time_locate = prefs.getString("list_preference", "1");
                switch (time_locate) {
                    case "2": {
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        scan();
                                    }
                                },
                                15000
                        );
                    }
                    case "3": {
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        scan();
                                    }
                                },
                                30000
                        );
                    }
                    case "4": {
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        scan();
                                    }
                                },
                                60000
                        );
                    }
                }
    }*/
    class Task3 implements Runnable {
        public void run() {
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(60000);
                    scan();
                    //run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}