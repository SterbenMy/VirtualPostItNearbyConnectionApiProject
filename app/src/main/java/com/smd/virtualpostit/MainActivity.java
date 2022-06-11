package com.smd.virtualpostit;


import android.Manifest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;

import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Strategy;
import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.io.File;
import java.security.spec.ECField;
import java.util.*;

import static com.smd.virtualpostit.Constants.*;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MainActivity extends ConnectionsActivity {

    private TextView mDebugLogView;

    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    public static final boolean DEBUG = true;

    private State mState = State.UNKNOWN;

    private String mName;

    private int startStopFlag = 0;

    NotificationManagerCompat notificationManagerCompat;
    Notification notification;
    private Button btnStart;
    private Button btnCamera;
    private Button btnViewImages;
    private Button btnViewMyImages;
    private Thread deleteDataThread;
    private boolean isRunning;
    LocationManager locationManager;
    int PERMISSION_ID = 44;
    private String[] deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("myCh", "My channerl", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        mDebugLogView = findViewById(R.id.debug_log);
        mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        deviceId = new String[]{Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)};
        mName = generateRandomName();
        createNotification();
        deleteDataThread = null;
        isRunning = false;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(view ->
        {
            if (startStopFlag == 0) {
                btnStart.setText(R.string.stop);
                setState(State.SEARCHING);
                startStopFlag = 1;


                deleteDataThread = new Thread(() -> {
                    isRunning = true;
                    while (isRunning) {
                        try {
                            if (getPostByLocation().size() != 0) {
//                                push();
                            }
                            deleteExpiredData();
                            Thread.sleep(5 * 60 * 1000);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "caught exception  InterruptedException (thread)");
                        }
                    }
                });
                deleteDataThread.start();
            } else {
                isRunning = false;
                if (deleteDataThread != null) {
                    try {
                        deleteDataThread.interrupt();
                    } catch (Exception e) {
                        Log.d(TAG, "caught exception  on interrupt (thread)");
                    }
                }
                btnStart.setText(R.string.start);
                setState(State.UNKNOWN);
                startStopFlag = 0;

            }


        });

        btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(view -> {
            Intent intent = new Intent(this, TakeAPhotoActivity.class);
            startActivity(intent);
        });

        btnViewImages = findViewById(R.id.btnViewImages);
        btnViewImages.setOnClickListener(view ->
        {
            Intent intent = new Intent(this, ViewMyImagesActivity.class);
            startActivity(intent);
        });

        btnViewMyImages = findViewById(R.id.btnViewMyImages);
        btnViewMyImages.setOnClickListener(view ->
        {
            Intent intent = new Intent(this, ViewImagesActivity.class);
            startActivity(intent);
        });

        if (!isLocationEnabled()) {
            Log.d(TAG, "Required to turn on location");
            Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("My notification id", "My Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onBackPressed() {
        if (getState() == State.CONNECTED) {
            setState(State.SEARCHING);
            return;
        }

        if (getState() == State.SEARCHING) {
            btnStart.setText(R.string.start);
            setState(State.UNKNOWN);
            startStopFlag = 0;
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Log.d(TAG, "onEndPointConnected ---- connected to " + endpoint);
        Toast.makeText(this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT).show();
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Log.d(TAG, "onEndpointDisconnected ---- endpointDisconnected");
        Toast.makeText(this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT).show();
        setState(State.SEARCHING);
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        Log.d(TAG, "onConnectionFailed ---- connection failed " + endpoint);
        if (getState() == State.SEARCHING) {
            startDiscovering();
            startAdvertising();
        }
    }

    @Override
    protected void onDiscoveryStarted() {
        Log.d(TAG, "Discovery started");
    }

    @Override
    protected void onDiscoveryFailed() {
        Log.d(TAG, "Discovery failed");
    }

    @Override
    protected void onAdvertisingFailed() {
        Log.d(TAG, "Advertising failed");
    }

    @Override
    protected void onAdvertisingStarted() {
        Log.d(TAG, "Advertising started");
    }

    private void setState(State state) {
        if (mState == state) {
            logW("State set to " + state + " but already in that state");
            return;
        }

        logD("State set to " + state);
        mState = state;

        onStateChanged(state);
    }

    private void onStateChanged(State newState) {
        switch (newState) {
            case SEARCHING:
                disconnectFromAllEndpoints();
                startDiscovering();
                startAdvertising();
                break;
            case CONNECTED:
                stopDiscovering();
                stopAdvertising();
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                break;
        }
    }

    private void deleteExpiredData() {
        DBHelper dbHelper = new DBHelper(this);
        String[] date = new String[]{(addHoursToJavaUtilDate(new Date(), -24)).toString()};
        List<Post> posts = dbHelper.getAllExpiredPosts(date);
        if (posts.size() > 0) {
            for (Post post : posts) {
                String imageNamePath = post.getImagePath() + "/" + post.getImageText();
                File imgFile = new File(imageNamePath);
                boolean deleted = imgFile.delete();
                dbHelper.deletePostById(post.getUid());
                Log.d(TAG, "Was deleted " + post.toString());
            }
        }
    }

    public Date addHoursToJavaUtilDate(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected String[] getRequiredPermissions() {
        return join(
                super.getRequiredPermissions(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static String[] join(String[] a, String... b) {
        String[] join = new String[a.length + b.length];
        System.arraycopy(a, 0, join, 0, a.length);
        System.arraycopy(b, 0, join, a.length, b.length);
        return join;
    }

    @Override
    protected String getName() {
        return mName;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public Strategy getStrategy() {
        return STRATEGY;
    }

    @Override
    protected void logV(String msg) {
        super.logV(msg);
        appendToLogs(toColor(msg, getResources().getColor(R.color.log_verbose)));
    }

    @Override
    protected void logD(String msg) {
        super.logD(msg);
        appendToLogs(toColor(msg, getResources().getColor(R.color.log_debug)));
    }

    @Override
    protected void logW(String msg) {
        super.logW(msg);
        appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
    }

    @Override
    protected void logW(String msg, Throwable e) {
        super.logW(msg, e);
        appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
    }

    @Override
    protected void logE(String msg, Throwable e) {
        super.logE(msg, e);
        appendToLogs(toColor(msg, getResources().getColor(R.color.log_error)));
    }

    private void appendToLogs(CharSequence msg) {
        mDebugLogView.append("\n");
        mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()) + ": ");
        mDebugLogView.append(msg);
    }

    private static CharSequence toColor(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
        return spannable;
    }

    private State getState() {
        return mState;
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 9; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    private void createNotification() {
        String message = "You have a few new posts";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "myCh")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Virtual Post-it notification")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notification = builder.build();
        notificationManagerCompat = NotificationManagerCompat.from(this);

        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    public void push() {
        notificationManagerCompat.notify(1, notification);
    }

    private List<Post> getPostByLocation() {
        DBHelper dbHelper = new DBHelper(this);
        List<Post> posts = dbHelper.getAllImgExceptMine(deviceId);
        List<Post> postList = new ArrayList<>();
        for (Post post : posts) {
            if (checkDistanceBetween(post.getLat(), post.getLon())) {
                postList.add(post);
            }
        }

        return postList;
    }

    public double lat;
    public double lon;

    private boolean checkDistanceBetween(Double startLat, Double startLog) {
        Location locationStart = new Location("Location A");
        Location locationEnd = new Location("Location B");

        locationStart.setLatitude(startLat);
        locationStart.setLongitude(startLog);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (isLocationEnabled()) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location locationPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                if (location != null) {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                } else if (locationNetwork != null) {
                    lat = locationNetwork.getLatitude();
                    lon = locationNetwork.getLongitude();
                } else if (locationPassive != null) {
                    lon = locationPassive.getLongitude();
                    lat = locationPassive.getLatitude();
                }

            } else {
                Log.d(TAG, "Required to turn on location");
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            Log.d(TAG, "Required permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ID);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        }


        locationEnd.setLatitude(lat);
        locationEnd.setLongitude(lon);
        double distance = locationStart.distanceTo(locationEnd);


        if (distance >= 0 && distance <= 1000) {
            return true;
        }
        return false;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "Check isLocationEnabled");
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
