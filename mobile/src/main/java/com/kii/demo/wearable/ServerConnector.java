package com.kii.demo.wearable;

import android.util.Pair;
import java.util.HashMap;

/**
 * Created by bogdan on 25/11/15.
 */
public class ServerConnector {

    //    private static final String endPoint = "http://10.0.2.2:3000/api/v1";
//    private static final String endPoint = "http://192.168.1.218:3000/api/v1";
    private static final String endPoint = "http://192.168.43.4";
//    private static final String endPoint = "http://10.0.2.2";
    private static final HTTPConnector httpConn = new HTTPConnector(false);
    public static String authToken;

    public ServerConnector() {}
    public ServerConnector(String mAuthToken) {
        authToken = mAuthToken;
    }

    public Pair<Integer, String> login(String email, String password) {
        HashMap<String, String> postParams = new HashMap<String, String>();
        postParams.put("username", email);
        postParams.put("password", password);

        HashMap<String, String> requestHeaders = new HashMap<String, String>();

        Pair<Integer, String> response = null;
        try {
            response = httpConn.sendRequest(endPoint + "/api/login", "POST", postParams, requestHeaders);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public Pair<Integer, String> getUser(String token) {
        HashMap<String, String> postParams = new HashMap<String, String>();

        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("X-Authorization", token);

        Pair<Integer, String> response = null;
        try {
            response = httpConn.sendRequest(endPoint + "/api/get_user", "GET", postParams, requestHeaders);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public Pair<Integer, String> sendHeartbeat(Heartbeat heartbeat, String token) {
        HashMap<String, String> postParams = new HashMap<String, String>();
        postParams.put("value", heartbeat.value + "");
        if (heartbeat.created != null) {
            postParams.put("created", heartbeat.created + "");
        }

        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("X-Authorization", token);

        Pair<Integer, String> response = null;
        try {
            response = httpConn.sendRequest(endPoint + "/api/add_heartbeat", "POST", postParams, requestHeaders);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}