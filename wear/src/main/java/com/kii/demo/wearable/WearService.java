package com.kii.demo.wearable;

import android.app.Service;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WearService extends Service implements SensorEventListener {

    private static final String TAG = "WEAR";

    static final public String WEAR_RESULT = "com.nitu.andrei.WearService.REQUEST_PROCESSED";
    static final public String WEAR_MESSAGE = "com.nitu.andrei.WearService.WEAR_MESSAGE";

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MOBILE_PATH = "/mobile";

    private GoogleApiClient mGoogleApiClient;
    private static final int SENSOR_TYPE_HEARTRATE = 65562;
    private String nodeId;
    private Thread tt;
    public Boolean loop = true;
    private Sensor mHeartRateSensor;
    private SensorManager mSensorManager;
    private Integer currentVal = -1;

    private final IBinder mBinder = new LocalBinder();

    LocalBroadcastManager broadcaster;

    @Override
    public void onDestroy() {
        Log.d(TAG, "STOP SERVICE");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(this);
        loop = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "SERVICE - onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "SERVICE - onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "SERVICE - onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        tt = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mGoogleApiClient != null && !(mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())) {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Log.d(TAG, "SERVICE - Opening connection");
                }

                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                List<Node> nodes = result.getNodes();

                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                    Log.d(TAG, "SERVICE - Node ID of phone: " + nodeId);
                } else {
                    Log.e(TAG, "SERVICE - No phone connected.");
                }

                while (loop) {
                    Log.d(TAG, "======== MESSAGE SENT");
                    Random rn = new Random();
                    Integer res = rn.nextInt(150 - 60 + 1) + 60;
                    currentVal = res;
                    byte[] bytes = ByteBuffer.allocate(4).putInt(res).array();
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, MOBILE_PATH, bytes).await();

                    // send broadcast message to update activity
                    Intent intent = new Intent(WEAR_RESULT);
                    intent.putExtra(WEAR_MESSAGE, currentVal.toString());
                    broadcaster.sendBroadcast(intent);

                    SystemClock.sleep(1500);
                }

                mGoogleApiClient.disconnect();
            }
        });
        tt.start();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(SENSOR_TYPE_HEARTRATE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, this.mHeartRateSensor, 3);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "=== BINDED");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        WearService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WearService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.values[0] > 0){
            String sensorValue = String.valueOf(sensorEvent.values[0]);
            byte[] bytes = sensorValue.getBytes();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, MOBILE_PATH, bytes).await();

            // send broadcast message to update activity
            Intent intent = new Intent(WEAR_RESULT);
            intent.putExtra(WEAR_MESSAGE, currentVal.toString());
            broadcaster.sendBroadcast(intent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadcaster = LocalBroadcastManager.getInstance(this);
        return super.onStartCommand(intent, flags, startId);
    }
}