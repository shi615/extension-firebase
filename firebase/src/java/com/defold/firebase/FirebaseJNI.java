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
    private static final int MSG_ERROR = 0;
    private static final int MSG_INITIALIZED = 1;
    private static final int MSG_INSTALLATION_AUTH_TOKEN = 2;
    private static final int MSG_INSTALLATION_ID = 3;

    private Activity activity;

    private FirebaseOptions.Builder optionsBuilder;

    public FirebaseJNI(Activity activity) {
        this.activity = activity;
    }

    public void initialize() {
        Log.d(TAG, "Firebase JNIでのFirebase初期化関数が呼ばれた");
        FirebaseApp app = null;
        if (optionsBuilder != null) {
            // optionsBuilderが存在する場合は、カスタムオプションで初期化
            app = FirebaseApp.initializeApp(activity.getApplicationContext(), optionsBuilder.build());
            optionsBuilder = null;
        } else if (FirebaseApp.getApps(activity.getApplicationContext()).size() == 0) {
            // アプリがまだ初期化されていない場合は、デフォルトの初期化を実施
            app = FirebaseApp.initializeApp(activity.getApplicationContext());
        } else {
            // 既に初期化されている場合は、既存のインスタンスを取得
            app = FirebaseApp.getInstance();
        }

        if (app != null) {
            String appName = app.getName();
            Log.d(TAG, "AppName is " + appName);
        } else {
            Log.d(TAG, "FirebaseApp is not initialized.");
        }

        sendSimpleMessage(MSG_INITIALIZED);
    }

    public boolean setOption(String key, String value) {
        Log.d(TAG, "オプション関数が呼ばれた:" + key + ", " + value);
        if (optionsBuilder == null) {
            FirebaseOptions defaultOption = FirebaseOptions.fromResource(activity.getApplicationContext());
            if (defaultOption != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Default FirebaseOptions:\n");
                sb.append("Default API Key: ").append(defaultOption.getApiKey()).append("\n");
                sb.append("Default Application ID: ").append(defaultOption.getApplicationId()).append("\n");
                sb.append("Default Database URL: ").append(defaultOption.getDatabaseUrl()).append("\n");
                sb.append("Default GCM Sender ID: ").append(defaultOption.getGcmSenderId()).append("\n");
                sb.append("Default Project ID: ").append(defaultOption.getProjectId()).append("\n");
                sb.append("Default Storage Bucket: ").append(defaultOption.getStorageBucket()).append("\n");
                Log.d(TAG, sb.toString());
            } else {
                Log.d(TAG, "DefaultOption is null.");
            }

            if (defaultOption != null) {
                Log.d(TAG, "BuilderをOption付きで初期化");
                optionsBuilder = new FirebaseOptions.Builder(defaultOption);
            } else {
                Log.d(TAG, "BuilderをOptionなしで初期化");
                optionsBuilder = new FirebaseOptions.Builder();
            }
        } else {
            Log.d(TAG, "optionsBuilder is not nill");
        }
        switch (key) {
            case "api_key":
                Log.d(TAG, "set api_key前のBuilder.apiKey:" + optionsBuilder.getApiKey());
                optionsBuilder.setApiKey(value);
                Log.d(TAG, "set api_key後のBuilder.apiKey:" + optionsBuilder.getApiKey());
                break;
            case "app_id":
                Log.d(TAG, "set app_id前のBuilder.app_id:" + optionsBuilder.getApplicationId());
                optionsBuilder.setApplicationId(value);
                Log.d(TAG, "set app_id後のBuilder.app_id:" + optionsBuilder.getApplicationId());
                break;
            case "database_url":
                Log.d(Tag, "set database_url前のBuilder.database_url:" + optionsBuilder.getDatabaseUrl());
                optionsBuilder.setDatabaseUrl(value);
                Log.d(Tag, "set database_url後のBuilder.database_url:" + optionsBuilder.getDatabaseUrl());
                break;
            case "messaging_sender_id":
                Log.d(TAG, "set messaging_sender_id前のBuilder.messaging_sender_id:" + optionsBuilder.getGcmSenderId());
                optionsBuilder.setGcmSenderId(value);
                Log.d(TAG, "set messaging_sender_id後のBuilder.messaging_sender_id:" + optionsBuilder.getGcmSenderId());
                break;
            case "project_id":
                Log.d(TAG, "set project_id前のBuilder.project_id:" + optionsBuilder.getProjectId());
                optionsBuilder.setProjectId(value);
                Log.d(TAG, "set project_id後のBuilder.project_id:" + optionsBuilder.getProjectId());
                break;
            case "storage_bucket":
                Log.d(TAG, "set storage_bucket前のBuilder.storage_bucket:" + optionsBuilder.getStorageBucket());
                optionsBuilder.setStorageBucket(value);
                Log.d(TAG, "set storage_bucket後のBuilder.storage_bucket:" + optionsBuilder.getStorageBucket());
                break;
            default:
                return false;
        }
        return true;
    }

    public void getInstallationAuthToken() {
        FirebaseInstallations.getInstance().getToken(false)
                .addOnCompleteListener(new OnCompleteListener<InstallationTokenResult>() {
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
