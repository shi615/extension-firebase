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

    public static void logActivityContents(Activity activity) {
        if (activity == null) {
            Log.d(TAG, "Firebase: Activity is null");
            return;
        }

        // Activityのインスタンス自体（toString()の内容）をログに出力
        Log.d(TAG, "Firebase: Activity instance: " + activity.toString());

        // Activityのクラス名をログに出力
        Log.d(TAG, "Firebase: Activity class: " + activity.getClass().getName());

        // Activityのタイトル（setTitleで設定したもの）をログに出力
        CharSequence title = activity.getTitle();
        Log.d(TAG, "Firebase: Activity title: " + (title != null ? title.toString() : "null"));

        // Activityのインテント情報をログに出力
        Log.d(TAG, "Firebase: Activity intent: " + activity.getIntent());

        // その他、必要な情報があればここでログに出力する
        // 例: Activityのハッシュコードや、独自に管理しているフィールドなど
        Log.d(TAG, "Firebase: Activity hashCode: " + activity.hashCode());
    }

    public void initialize() {
        Log.d(TAG, "Firebase初期化関数が呼び出された");
        logActivityContents(activity);
        FirebaseApp firebaseApp;
        if (optionsBuilder != null) {
            Log.d(TAG, "Optionsで初期化");
            FirebaseOptions firebaseOptions = optionsBuilder.build();
            Log.d(TAG, "---------firebase 初期化前------------");
            Log.d(TAG, "firebase project id is:" + firebaseOptions.getProjectId());
            Log.d(TAG, "firebase api key is:" + firebaseOptions.getApiKey());
            Log.d(TAG, "firebase application id is:" + firebaseOptions.getApplicationId());
            Log.d(TAG, "firebase database id is:" + firebaseOptions.getDatabaseUrl());
            Log.d(TAG, "firebase sender id is:" + firebaseOptions.getGcmSenderId());
            Log.d(TAG, "firebase storage bucket is:" + firebaseOptions.getStorageBucket());
            firebaseApp = FirebaseApp.initializeApp(activity.getApplicationContext(), firebaseOptions);
            Log.d(TAG, "---------firebase 初期化後------------");
            Log.d(TAG, "firebase project id is:" + firebaseApp.getOptions().getProjectId());
            Log.d(TAG, "firebase api key is:" + firebaseApp.getOptions().getApiKey());
            Log.d(TAG, "firebase application id is:" + firebaseApp.getOptions().getApplicationId());
            Log.d(TAG, "firebase database id is:" + firebaseApp.getOptions().getDatabaseUrl());
            Log.d(TAG, "firebase sender id is:" + firebaseApp.getOptions().getGcmSenderId());
            Log.d(TAG, "firebase storage bucket is:" + firebaseApp.getOptions().getStorageBucket());
            optionsBuilder = null;
        }
        else if (FirebaseApp.getApps(activity.getApplicationContext()).size() == 0) {
            Log.d(TAG, "DefaultOptionsで初期化");
            firebaseApp = FirebaseApp.initializeApp(activity.getApplicationContext());
        }
        sendSimpleMessage(MSG_INITIALIZED);
    }

    public boolean setOption(String key, String value) {
        if (optionsBuilder == null) {
            // FirebaseOptions defaultOption = FirebaseOptions.fromResource(activity.getApplicationContext());
            // if (defaultOption != null) {
            //     optionsBuilder = new FirebaseOptions.Builder(defaultOption);
            // }
            // else {
            //     optionsBuilder = new FirebaseOptions.Builder();
            // }
            Log.d(TAG, "Default Optionの有無に関わらずBuilder()で空のOptionsを初期化");
            optionsBuilder = new FirebaseOptions.Builder();
        }
        Log.d(TAG, key + "オプションを設定する：" + value);
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
        return true;
    }

    public void getInstallationAuthToken() {
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
        Log.d(TAG, "getInstallationId関数が呼び出された");
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
