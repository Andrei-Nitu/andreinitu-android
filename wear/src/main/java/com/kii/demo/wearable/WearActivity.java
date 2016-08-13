/*
 * Copyright (C) 2014 Marc Lester Tan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kii.demo.wearable;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class WearActivity extends Activity {

    private static final String TAG = "WEAR";

    private TextView rate;
    private TextView accuracy;
    private TextView sensorInformation;

    private Boolean mBound = false;
    private WearService mService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            WearService.LocalBinder binder = (WearService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.v(TAG, "[MainActivity]: onServiceConnected()");
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mService = null;
            mBound = false;
            Log.v(TAG, "[MainActivity]: onServiceDisconnected()");
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra(WearService.WEAR_MESSAGE);
            rate.setText(s);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    rate = (TextView) stub.findViewById(R.id.rate);
                    rate.setText("Reading...");

                    accuracy = (TextView) stub.findViewById(R.id.accuracy);
                    sensorInformation = (TextView) stub.findViewById(R.id.sensor);

                    final Button button = (Button) stub.findViewById(R.id.ss_service);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Intent serviceIntent = new Intent(WearActivity.this, WearService.class);
                            if (isServiceRunning()) {
                                button.setText("Start service");
                                if (mBound) {
                                    unbindService(mServiceConnection);
                                    mBound = false;
                                }
                                stopService(serviceIntent);
                            } else {
                                button.setText("Stop service");
                                startService(serviceIntent);
                                bindService(serviceIntent, mServiceConnection, WearActivity.this.BIND_AUTO_CREATE);
                                mBound = true;
                            }
                        }
                    });

                    if (isServiceRunning()) {
                        button.setText("Stop service");
                    } else {
                        button.setText("Start service");
                    }

                }
            });
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(WearService.WEAR_RESULT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.kii.demo.wearable.WearService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
