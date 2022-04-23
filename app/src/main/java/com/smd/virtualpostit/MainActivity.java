package com.smd.virtualpostit;


import static com.smd.virtualpostit.Constants.COLORS;
import static com.smd.virtualpostit.Constants.SERVICE_ID;


import android.Manifest;

import android.content.Intent;

import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.Objects;
import java.util.Random;

public class MainActivity extends ConnectionsActivity {

    /**
     * A running log of debug messages. Only visible when DEBUG=true.
     */
    private TextView mDebugLogView;

    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /**
     * A running log of debug messages. Only visible when DEBUG=true.
     */
    public static final boolean DEBUG = true;

    private State mState = State.UNKNOWN;

    private String mName;

    private int startStopFlag = 0;

    private Button btnStart;

    @ColorInt
    public int mConnectedColor = COLORS[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDebugLogView = findViewById(R.id.debug_log);
        mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        mName = generateRandomName();

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(view ->
        {
            if (startStopFlag == 0) {
                btnStart.setText(R.string.stop);
                setState(State.SEARCHING);
                startStopFlag = 1;
            }else{
                btnStart.setText(R.string.start);
                setState(State.UNKNOWN);
                startStopFlag = 0;
            }
        });

        Button btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(view -> {
            Intent intent = new Intent(this, ViewImagesActivity.class);
            startActivity(intent);
        });

        Button btnViewImages = findViewById(R.id.btnViewImages);
        btnViewImages.setOnClickListener(view ->
        {
            Intent intent = new Intent(this, ViewImagesActivity.class);
            startActivity(intent);
        });
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
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        stopDiscovering();
        connectToEndpoint(endpoint);
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll use the auth token, which is the
        // same on both devices, to pick a color to use when we're connected. This way, users can
        // visually see which device they connected with.
        mConnectedColor = COLORS[connectionInfo.getAuthenticationToken().hashCode() % COLORS.length];

        // We accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(
                this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.SEARCHING);
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.SEARCHING) {
            startDiscovering();
            startAdvertising();
        }
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
//                stopAdvertising();
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        // onReceive imaginea
    }

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

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    private State getState() {
        return mState;
    }
}
