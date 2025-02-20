package com.defold.firebase;

import androidx.annotation.NonNull;
import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.installations.InstallationTokenResult;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;

import org.json.JSONObject;
import org.json.JSONException;


public class FirebaseJNI {
    private static final String TAG = "FirebaseJNI";

    public static native void firebaseAddToQueue(int msg, String json);

    // duplicate of enums from firebase_callback.h:
    // CONSTANTS:
    private static final int MSG_ERROR =                   0;
    private static final int MSG_INITIALIZED =             1;
    private static final int MSG_INSTALLATION_AUTH_TOKEN = 2;
    private static final int MSG_INSTALLATION_ID =         3;

    private Activity activity;
    
    private FirebaseOptions.Builder optionsBuilder;

    public FirebaseJNI(Activity activity) {
        this.activity = activity;
    }

    public void initialize() {
        Log.d(TAG, "Firebase初期化関数が呼び出された");
        Log.d(TAG, "FirebaseAppの数（初期化前）は" + FirebaseApp.getApps(activity.getApplicationContext()).size());
        if (optionsBuilder != null) {
            Log.d(TAG, "オプションありで初期化");
            FirebaseApp.initializeApp(activity.getApplicationContext(), optionsBuilder.build());
            optionsBuilder = null;
        }
        else if (FirebaseApp.getApps(activity.getApplicationContext()).size() == 0) {
            Log.d(TAG, "オプションなしで初期化");
            FirebaseApp.initializeApp(activity.getApplicationContext());
        }
        Log.d(TAG, "FirebaseAppの数（初期化後）は" + FirebaseApp.getApps(activity.getApplicationContext()).size());
        sendSimpleMessage(MSG_INITIALIZED);
    }

    public boolean setOption(String key, String value) {
        if (optionsBuilder == null) {
            FirebaseOptions defaultOption = FirebaseOptions.fromResource(activity.getApplicationContext());
            if (defaultOption != null) {
                Log.d(TAG, "defaultOptionありでBuilder初期化");
                Log.d(TAG, "Default Option: " +
                        "API Key: " + defaultOption.getApiKey() + ", " +
                        "Application ID: " + defaultOption.getApplicationId() + ", " +
                        "Database URL: " + defaultOption.getDatabaseUrl() + ", " +
                        "GCM Sender ID: " + defaultOption.getGcmSenderId() + ", " +
                        "Project ID: " + defaultOption.getProjectId() + ", " +
                        "Storage Bucket: " + defaultOption.getStorageBucket());
                optionsBuilder = new FirebaseOptions.Builder(defaultOption);
            }
            else {
                Log.d(TAG, "defaultOptionなしでBuilder初期化");
                optionsBuilder = new FirebaseOptions.Builder();
            }
        }
        Log.d(TAG, "オプション値" + key + "を設定する：" + value);
        switch (key) {
            case "api_key":
                optionsBuilder.setApiKey(value);
                break;
            case "app_id":
                optionsBuilder.setApplicationId(value);
                break;
            case "database_url":
                optionsBuilder.setDatabaseUrl(value);
                break;
            case "messaging_sender_id":
                optionsBuilder.setGcmSenderId(value);
                break;
            case "project_id":
                optionsBuilder.setProjectId(value);
                break;
            case "storage_bucket":
                optionsBuilder.setStorageBucket(value);
                break;
            default:
                return false;
        }
        Log.d(TAG, "オプション値" + key + "を"  + value + "に設定した後のbuilderOptions: " +
                "API Key: " + optionsBuilder.build().getApiKey() + ", " +
                "Application ID: " + optionsBuilder.build().getApplicationId() + ", " +
                "Database URL: " + optionsBuilder.build().getDatabaseUrl() + ", " +
                "GCM Sender ID: " + optionsBuilder.build().getGcmSenderId() + ", " +
                "Project ID: " + optionsBuilder.build().getProjectId() + ", " +
                "Storage Bucket: " + optionsBuilder.build().getStorageBucket());
        return true;
    }

    public void getInstallationAuthToken() {
        Log.d(TAG, "Firebaseトークン取得関数が呼び出された");
        FirebaseInstallations.getInstance().getToken(false).addOnCompleteListener(new OnCompleteListener<InstallationTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<InstallationTokenResult> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    sendSimpleMessage(MSG_INSTALLATION_AUTH_TOKEN, "token", task.getResult().getToken());
                } else {
                    sendErrorMessage("Unable to get Installation auth token");
                }
            }
        });
    }

    public void getInstallationId() {
        Log.d(TAG, "FirebaseID取得関数が呼び出された");
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    sendSimpleMessage(MSG_INSTALLATION_ID, "id", task.getResult());
                } else {
                    sendErrorMessage("Unable to get Installation ID");
                }
            }
        });
    }

    // https://www.baeldung.com/java-json-escaping
    private String getJsonConversionErrorMessage(String errorText) {
        String message = null;
        
        try {
            JSONObject obj = new JSONObject();
            obj.put("error", errorText);
            message = obj.toString();
        } catch (JSONException e) {
            message = "{ \"error\": \"Error while converting simple message to JSON.\"}";
        }

        return message;
    }

    private void sendErrorMessage(String errorText) {
        String message = getJsonConversionErrorMessage(errorText);
        Log.d(TAG, "FIR Error");
        Log.d(TAG, message);
        firebaseAddToQueue(MSG_ERROR, message);
    }

    private void sendSimpleMessage(int msg) {
        firebaseAddToQueue(msg, "{}");
    }

    private void sendSimpleMessage(int msg, String key, String value) {
        String message = null;

        try {
            JSONObject obj = new JSONObject();
            obj.put(key, value);
            message = obj.toString();
            firebaseAddToQueue(msg, message);
        } catch (JSONException e) {
            message = getJsonConversionErrorMessage(e.getLocalizedMessage());
            firebaseAddToQueue(MSG_ERROR, message);
        }
    }    
}
