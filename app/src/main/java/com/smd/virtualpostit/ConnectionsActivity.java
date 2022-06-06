package com.smd.virtualpostit;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.material.snackbar.Snackbar;
import com.smd.virtualpostit.DataModel.Post;
import com.smd.virtualpostit.DatabaseConf.DBHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static android.os.Build.VERSION.SDK_INT;
import static com.smd.virtualpostit.Constants.TAG;


public abstract class ConnectionsActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final String[] REQUIRED_PERMISSIONSQ =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private ConnectionsClient mConnectionsClient;

    /**
     * The devices we've discovered near us.
     */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();


    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> outgoingPayloads = new SimpleArrayMap<>();

    private boolean mIsConnecting = false;
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    private Payload filePayload;
    private Endpoint endpnt;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectionsClient = Nearby.getConnectionsClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }

        if (SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Snackbar.make(findViewById(android.R.id.content), "Permission needed!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", v -> {
                            try {
                                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                                startActivity(intent);
                            } catch (Exception ex) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
     * we've found out if we successfully entered this mode.
     */
    protected void startAdvertising() {
        Log.v(TAG, "start advertising");
        mIsAdvertising = true;
        final String localEndpointName = getName();

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        mConnectionsClient.startAdvertising(localEndpointName, getServiceId(), mConnectionLifecycleCallback, advertisingOptions.build())
                .addOnSuccessListener(
                        e -> {
                            logV("Now advertising endpoint " + localEndpointName);
                            onAdvertisingStarted();
                        })
                .addOnFailureListener(
                        e -> {
                            mIsAdvertising = false;
                            logW("startAdvertising() failed.", e);
                            onAdvertisingFailed();
                        });
    }

    protected void startDiscovering() {
        Log.d(TAG, "startDiscovering");
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(getStrategy());
        mConnectionsClient.startDiscovery(getServiceId(), mEndpointDiscoveryCallback,
                discoveryOptions.build())
                .addOnSuccessListener(e -> onDiscoveryStarted())
                .addOnFailureListener(e -> {
                    mIsDiscovering = false;
                    onDiscoveryFailed();
                });
    }


    protected void stopAdvertising() {
        Log.v(TAG, "stop advertising");
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    protected void stopDiscovering() {
        Log.v(TAG, "stop discovering");
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
            Log.d(TAG, "Disconect from all endpoints");
        }
        mEstablishedConnections.clear();
    }

    protected void stopAllEndpoints() {
        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    protected void disconnect(Endpoint endpoint) {
        Log.d(TAG, "Disconnect from endpoit");
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }


    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(TAG, String.format(
                            "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                            endpointId, connectionInfo.getEndpointName()));
                    logD(String.format("onConnectionInitiated(endpointId=%s, endpointName=%s)", endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    ConnectionsActivity.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
                    Log.d(TAG, String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));
                    logD(String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    mIsConnecting = false;

                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            connectedToEndpoint(mPendingConnections.remove(endpointId));
                            sendFile();
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.w(TAG, String.format("Connection failed. Received status"));
                            logW(String.format("Connection failed. Received status %s.", ConnectionsActivity.toString(result.getStatus())));
                            onConnectionFailed(mPendingConnections.remove(endpointId));
                            break;
                        default:
                    }

                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        logW("Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                }
            };

    /**
     * Callbacks for payloads (bytes of data) sent from another device to us.
     */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    Log.d(TAG, "onPayloadReceived(endpointId= " + endpointId + " payload=" + payload + " )");
                    logD(String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));

                    if (payload.getType() == Payload.Type.BYTES) {
                        Log.d(TAG, "onPayloadReceived: Payload.Type.BYTES");
                        String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);
                        Log.d(TAG, "onPayloadReceived: BYTES " + payloadFilenameMessage);

                        if (payloadFilenameMessage.split(",").length == 8) {
                            Log.d(TAG, payloadFilenameMessage);
                            insertToDb(payloadFilenameMessage);
                        } else {
                            long payloadId = addPayloadFilename(payloadFilenameMessage);
                            processFilePayload(payloadId);
                        }
                    } else if (payload.getType() == Payload.Type.FILE) {
                        incomingFilePayloads.put(payload.getId(), payload);
                        Log.d(TAG, "onPayloadReceived: Payload.Type.FILE");
                    }
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    Log.d(TAG, String.format("onPayloadTransferUpdate(endpointId=%s, update=%s)",
                            endpointId, update));


                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        Log.d(TAG, "onPayloadTransferUpdate: SUCCESS");

                        Payload payload = incomingFilePayloads.remove(update.getPayloadId());
                        completedFilePayloads.put(update.getPayloadId(), payload);
                        if (payload != null && payload.getType() == Payload.Type.FILE) {
                            Log.d(TAG, "onPayloadTransferUpdate: FILE " + payload.getId());
                            // Retrieve the filename that was received in a bytes payload.
//                            String newFilename = filePayloadFilenames.remove(update.getPayloadId());

                            processFilePayload(update.getPayloadId());
//                            File payloadFile = payload.asFile().asJavaFile();
//                            // Rename the file.
//                            payloadFile.renameTo(new File(payloadFile.getParentFile(), newFilename));
                        }
                    } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                        Log.d(TAG, "onPayloadTransferUpdate: FAILURE");
                    }
                }
            };

    //trimite request de connectare
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                    logD(String.format("onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)", endpointId, discoveredEndpointInfo.getServiceId(), discoveredEndpointInfo.getEndpointName()));

                    if (getServiceId().equals(discoveredEndpointInfo.getServiceId())) {
                        Endpoint endpoint = new Endpoint(endpointId, discoveredEndpointInfo.getEndpointName());
                        mDiscoveredEndpoints.put(endpointId, endpoint);
                        onEndpointDiscovered(endpoint);
                    }
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                    logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
                }
            };

    /**
     * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
     * call {@link #rejectConnection(Endpoint)}.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        //automat acceptam
        acceptConnection(endpoint);
    }


    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient.acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnSuccessListener(e -> Log.d(TAG, "Successfully accepted the connection"))
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to accet the connection");
                    logW("acceptConnection() failed.", e);
                });
    }

    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient.rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        e -> logW("rejectConnection() failed.", e));
    }

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call {@link
     * #connectToEndpoint(Endpoint)}.
     */
    protected void onEndpointDiscovered(Endpoint endpoint) {
        Log.d(TAG, "Endpoint  found " + endpoint);
        //pahod nu trebuie
//        stopDiscovering();
        connectToEndpoint(endpoint);
    }

    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        logV("Sending a connection request to endpoint " + endpoint);
        if (mIsAdvertising) {
            stopAdvertising();
        }

        if (mIsConnecting) {
            Log.w(TAG, "Already connecting, so ignoring this endpoint: " + endpoint);
            return;
        }
        mIsConnecting = true;
        // Ask to connect
        mConnectionsClient.requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnSuccessListener(e -> {
                    Log.d(TAG, "Successfully sent coneection request");
                })
                .addOnFailureListener(e -> {
                    logW("requestConnection() failed.", e);
                    mIsConnecting = false;
                    onConnectionFailed(endpoint);
                });
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
        stopDiscovering();
        stopAdvertising();

    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    public void sendFile() {
        if (mEstablishedConnections.values().size() > 0) {
            for (Endpoint endpoint : mEstablishedConnections.values()) {
                try {
                    DBHelper dbHelper = new DBHelper(this);
                    List<Post> posts = dbHelper.getAllPost();
                    for (Post post : posts) {
                        String imageNamePath = post.getImagePath() + "/" + post.getImageText();
                        Uri uri = Uri.fromFile(new File(imageNamePath));

                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                        filePayload = Payload.fromFile(pfd);
                        endpnt = endpoint;

                        Payload dbData = Payload.fromBytes((post.toString().getBytes()));
                        Log.d(TAG, "sendData: filename message " + post.toString());
                        mConnectionsClient.sendPayload(endpoint.getId(), dbData);

                        // Construct a simple message mapping the ID of the file payload to the desired filename.
                        String payloadFilenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();
                        Log.d(TAG, "sendFile: filename message " + payloadFilenameMessage);

                        // Send the filename message as a bytes payload.
                        Payload payload = Payload.fromBytes(payloadFilenameMessage.getBytes(StandardCharsets.UTF_8));
                        outgoingPayloads.put(payload.getId(), payload);

                        mConnectionsClient.sendPayload(endpoint.getId(), payload);

//                         Finally, send the file payload.
                        mConnectionsClient.sendPayload(endpoint.getId(), filePayload);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(TAG, "sendFile: EstablishedConnections == 0");
        }
    }

    private long addPayloadFilename(String payloadFilenameMessage) {
        int colonIndex = payloadFilenameMessage.indexOf(':');
        String payloadId = payloadFilenameMessage.substring(0, colonIndex);
        String filename = payloadFilenameMessage.substring(colonIndex + 1);
        filePayloadFilenames.put(Long.valueOf(payloadId), filename);
        return Long.parseLong(payloadId);
    }

    private void processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        String filename = filePayloadFilenames.get(payloadId);
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId);
            filePayloadFilenames.remove(payloadId);

            // Get the received file (which will be in the Downloads folder)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                Uri uri = filePayload.asFile().asUri();
                try {
                    // Copy the file to a new location.
                    InputStream in = this.getContentResolver().openInputStream(uri);
                    copyStream(in, new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/VirtualPostIt"), filename)));
                } catch (IOException e) {
                    // Log the error.
                    e.printStackTrace();
                    Log.d(TAG, "processFilePayload ---- error ");
                } finally {
                    // Delete the original file.
                    this.getContentResolver().delete(uri, null, null);
                }
//            } else {
//                File payloadFile = filePayload.asFile().asJavaFile();
//
//                // Rename the file.
//                payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
            }
        }
//    }

    private void insertToDb(String data) {
        String[] dataAsArr = data.split(",");
        List<String> dataList = new ArrayList<>(Arrays.asList(dataAsArr));
        Post post = new Post();
        DBHelper dbHelper = new DBHelper(this);

        List<Post> posts = dbHelper.getAllPost();
        if (SDK_INT >= Build.VERSION_CODES.N) {
            if (!ifContains(posts, dataList.get(5))) {
                post.setName(dataList.get(0));
                post.setComment(dataList.get(1));
                post.setLon(Double.valueOf(dataList.get(2)));
                post.setLat(Double.valueOf(dataList.get(3)));
                post.setDob(dataList.get(4));
                post.setImagePath(dataList.get(5));
                post.setImageText(dataList.get(6));
                post.setDeviceId(dataList.get(7));

                boolean check = dbHelper.insertPostData(post);
                if (check) {
                    Log.d(TAG, "Inserted successfully data imageName =" + post.getImagePath());
                } else {
                    Log.d(TAG, "Get some error insertToDd data = " + data);
                }
            }
        }
    }

    private boolean ifContains(List<Post> posts, String element) {
        if (posts != null) {
            for (Post post : posts) {
                if (post.getImageText().equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            in.close();
            out.close();
        }
    }

    protected String[] getRequiredPermissions() {
        if (SDK_INT >= 30) {
            return REQUIRED_PERMISSIONSQ;
        } else {

            return REQUIRED_PERMISSIONS;
        }

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getName();

    protected abstract String getServiceId();

    protected abstract Strategy getStrategy();

    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    @CallSuper
    protected void logV(String msg) {
        Log.v(TAG, msg);
    }

    @CallSuper
    protected void logD(String msg) {
        Log.d(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg) {
        Log.w(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg, Throwable e) {
        Log.w(TAG, msg, e);
    }

    @CallSuper
    protected void logE(String msg, Throwable e) {
        Log.e(TAG, msg, e);
    }

    protected static class Endpoint {
        @NonNull
        private final String id;
        @NonNull
        private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }

    protected void onConnectionFailed(Endpoint endpoint) {
    }

    protected void onEndpointConnected(Endpoint endpoint) {
    }

    protected void onEndpointDisconnected(Endpoint endpoint) {
    }

    protected void onReceive(Endpoint endpoint, Payload payload) {
    }

    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    protected Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    protected Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
    }

    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    protected boolean isAdvertising() {

        return mIsAdvertising;
    }

    protected void onDiscoveryStarted() {
    }

    protected void onDiscoveryFailed() {
    }

    protected void onAdvertisingFailed() {
    }

    protected void onAdvertisingStarted() {
    }

    /**
     * Called when the user has accepted (or denied) our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}