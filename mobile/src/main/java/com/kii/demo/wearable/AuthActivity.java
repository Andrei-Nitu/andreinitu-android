package com.kii.demo.wearable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;


/**
 * A login screen that offers login via 4 digit pin.

 */
public class AuthActivity extends Activity {
    public static Boolean isRunning = false;
    private static final String TAG = "PHN";

    private UserSignInTask mSignInTask = null;
    private UserTokenSignInTask mTokenSignInTask = null;

    // UI references.
    private EditText mPasswordView;
    private EditText mUsernameView;
    private View mProgressView;
    private View mAuthFormView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Set up the auth form.
        mPasswordView = (EditText) findViewById(R.id.passw);
        mUsernameView = (EditText) findViewById(R.id.username);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);

        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuth();
            }
        });

        mAuthFormView = findViewById(R.id.auth_form);
        mProgressView = findViewById(R.id.progressBar);

        tryLoginWithToken();
    }

    @Override
    protected void onStart() {
        SharedPreferences sp = getSharedPreferences("OURINFO", MODE_MULTI_PROCESS);
        Log.d(TAG, "==== ASDASDSADASDSA");
        System.out.println("====== ASDASDSADSADASDS");
        Log.d(TAG, "onStart : " + sp.getBoolean("active", false) + "");
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();

        super.onStart();
    }

    @Override
    protected void onStop() {
        SharedPreferences sp = getSharedPreferences("OURINFO", MODE_MULTI_PROCESS);
        Log.d(TAG, "onStop : " + sp.getBoolean("active", false) + "");
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void tryLoginWithToken() {
        String token = Settings.loadAccessToken(this);
        if (token != null) {
            Log.d(TAG, "Token: " + token);
            showProgress(true);
            mTokenSignInTask = new UserTokenSignInTask(this, token);
            mTokenSignInTask.execute((Void) null);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptAuth() {
        // Reset errors.
        mPasswordView.setError(null);

        // Store values at the time of the auth attempt.
        String password = mPasswordView.getText().toString();
        String username = mUsernameView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            focusView.requestFocus();
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            focusView.requestFocus();
            cancel = true;
        }

        if (!cancel) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mSignInTask = new UserSignInTask(this, password, username);
            mSignInTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mAuthFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mPassword;
        private final String mUsername;

        UserSignInTask(Context context, String password, String username) {
            mContext = context;
            mPassword = password;
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String id = Settings.id(mContext);
                Log.d(TAG, "Authentication");
                Pair<Integer, String> response = new ServerConnector().login(mUsername, mPassword);
                Log.d(TAG, "RESPONSE: " + response.first + " : " + response.second);
                if (response.first/100 == 2) {
                    JSONObject jObject = new JSONObject(response.second);
                    String token = jObject.getString("authkey");
                    Settings.saveAccessToken(mContext, token);
                    Log.d(TAG, token);
                } else {
                    throw new Exception("Cannot authenticate");
                }
            } catch (Exception e) {
                return false;
            }
            Log.d(TAG, "Sign in successful");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSignInTask = null;
            showProgress(false);

            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mSignInTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user via an access token.
     */
    public class UserTokenSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mToken;

        UserTokenSignInTask(Context context, String token) {
            mContext = context;
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Log.d(TAG, "Attempting sign in with access token");
                Pair<Integer, String> response = new ServerConnector().getUser(mToken);
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
            mTokenSignInTask = null;
            showProgress(false);
            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.e(TAG, "Error signing in with token");
            }
        }

        @Override
        protected void onCancelled() {
            mTokenSignInTask = null;
            showProgress(false);
        }
    }
}
