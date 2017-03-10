package com.rustfisher.watcher;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.rustfisher.watcher.utils.AppConfigs;

import java.io.File;

import static com.rustfisher.watcher.activity.MainActivity.TAG;

public class WiFiApp extends Application {

    private static WiFiApp wiFiApp;

    public static WiFiApp getApp() {
        return wiFiApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wiFiApp = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File imgDir = new File(AppConfigs.IMG_DIR);
                if (!imgDir.exists()) {
                    boolean i = imgDir.mkdirs();
                    if (i) {
                        Log.d(TAG, "Create folder " + imgDir.getAbsolutePath());
                    }
                } else {
                    Log.d(TAG, "Had folder");
                }
            }
        }).start();
    }
}
