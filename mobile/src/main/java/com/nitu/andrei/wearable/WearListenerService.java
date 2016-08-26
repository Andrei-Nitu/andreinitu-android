package com.nitu.andrei.wearable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;

public class WearListenerService extends WearableListenerService {

    private final String TAG = "PHN";
    private Boolean activityStarted = false;

    static final public String PHN_RESULT = "com.nitu.andrei.WearListenerService.REQUEST_PROCESSED";
    static final public String PHN_MESSAGE = "com.nitu.andrei.WearListenerService.WEAR_MESSAGE";

    private static final String MOBILE_PATH = "/mobile";
    private final IBinder mBinder = new LocalBinder();

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String WEAR_PATH = "/wear";
    private String nodeId;

    private ServerConnector serverConnector;
    String token = null;

    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    public void onCreate() {
        String serverAddress = Settings.loadServerAddress(this);
        if (serverAddress != null) {
            serverConnector = new ServerConnector(serverAddress);
        }
        tryLoginWithToken();

        Log.d(TAG, "ON CREATE");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<Heartbeat> heatbeats = db.getUnsyncedHeartbeats();
        for (Heartbeat hb: heatbeats) {
            serverConnector.sendHeartbeat(hb, token);
        }

        Log.d(TAG, "ON START COMMAND");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(MOBILE_PATH)) {
            ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData()); // big-endian by default
            Integer heartbeat = wrapped.getInt(); // 1
            Log.d(TAG, "=================== MESSAGE received = " + heartbeat);

            // Add to database
            long hbid = db.addHeartbeat(heartbeat, false);
            Log.d(TAG, "======= HBID" + hbid);

            // Send heartbeat to activity for display
            Intent intent = new Intent(PHN_RESULT);
            intent.putExtra(PHN_MESSAGE, heartbeat.toString());
            getApplicationContext().sendBroadcast(intent);

            if (token != null) {
                Pair<Integer, String> response = serverConnector.sendHeartbeat(new Heartbeat(heartbeat, null, null), token);

                if (response.first/100 == 2) {
                    JSONObject jObject = null;
                    String status = null;
                    try {
                        jObject = new JSONObject(response.second);
                        status = jObject.getString("status");
                        if (Objects.equals(status.toLowerCase(), "ok")) {
                            db.setHeartbeatAsSynced(hbid);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void tryLoginWithToken() {
        token = Settings.loadAccessToken(this);
        String serverAddress = Settings.loadServerAddress(this);
        Log.d(TAG, "==== TOKEN "+token);
        if (token != null && serverAddress != null) {
            Log.d(TAG, "Token: " + token);
            UserTokenSignInTask mTokenSignInTask = new UserTokenSignInTask(this, token, serverAddress);
            mTokenSignInTask.execute((Void) null);
        } else {
            SharedPreferences sp = getSharedPreferences("OURINFO", MODE_MULTI_PROCESS);
            activityStarted = sp.getBoolean("active", false);
            Log.d(TAG, "service : " + sp.getBoolean("active", false) + "");

            if (!activityStarted) {
                Log.d(TAG, "Launch activity");
                activityStarted = true;
                Intent dialogIntent = new Intent(this, AuthActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
                stopSelf();
            }
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user via an access token.
     */
    public class UserTokenSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mToken;
        private final String mServerAddress;

        UserTokenSignInTask(Context context, String token, String serverAddress) {
            mContext = context;
            mToken = token;
            mServerAddress = serverAddress;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Log.d(TAG, "Attempting sign in with access token");
                Pair<Integer, String> response = new ServerConnector(mServerAddress).getUser(mToken);
                Log.d(TAG, "RESPONSE: " + response.first + " : " + response.second);
                if (response.first/100 == 2) {
                    JSONObject jObject = new JSONObject(response.second);
                    String token = jObject.getString("authkey");
                } else {
                    throw new Exception("Cannot authenticate");
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return false;
            }
            Log.d(TAG, "Sign in successful. User id: ");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
            } else {
                Log.e(TAG, "Error signing in with token");
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class LocalBinder extends Binder {
        WearListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WearListenerService.this;
        }
    }
}