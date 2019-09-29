package com.wix.reactnativenotifications.fcm;

import android.os.Bundle;
import android.util.Log;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import com.wix.reactnativenotifications.core.notification.IPushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotification;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

/**
 * Instance-ID + token refreshing handling service. Contacts the FCM to fetch the updated token.
 *
 * @author amitd
 */
public class FcmInstanceIdListenerService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message){
        final Bundle bundle = message.toIntent().getExtras();
        Log.d(LOGTAG, "New message from FCM :): " + bundle + message.getData());


            // We need to run this on the main thread, as the React code assumes that is true.
            // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
            // "Can't create handler inside thread that has not called Looper.prepare()"
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    // Construct and load our normal React JS code bundle
                    ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                    ReactContext context = mReactInstanceManager.getCurrentReactContext();
                    // If it's constructed, send a notification
                    if (context != null) {
                        try {
                            final IPushNotification notification = PushNotification.get(getApplicationContext(), bundle);
                            notification.onReceived();

                        } catch (IPushNotification.InvalidNotificationException e) {
                            // An FCM message, yes - but not the kind we know how to work with.
                            Log.v(LOGTAG, "FCM message handling aborted", e);
                        }
                    } else {
                        // Otherwise wait for construction, then send the notification
                        mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                            public void onReactContextInitialized(ReactContext context) {
                                try {
                                    final IPushNotification notification = PushNotification.get(getApplicationContext(), bundle);
                                    notification.onReceived();

                                } catch (IPushNotification.InvalidNotificationException e) {
                                    // An FCM message, yes - but not the kind we know how to work with.
                                    Log.v(LOGTAG, "FCM message handling aborted", e);
                                }
                            }
                        });
                        if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                            // Construct it in the background
                            mReactInstanceManager.createReactContextInBackground();
                        }
                    }
                }
            });

    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isApplicationInForeground() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        if (processInfos != null) {
            for (RunningAppProcessInfo processInfo : processInfos) {
                if (processInfo.processName.equals(getApplication().getPackageName())) {
                    if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (String d : processInfo.pkgList) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
