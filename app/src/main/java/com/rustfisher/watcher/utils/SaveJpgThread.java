package com.rustfisher.watcher.utils;

import android.content.Intent;
import android.util.Log;

import com.rustfisher.watcher.WiFiApp;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Save jpg file
 * Created by Rust Fisher on 2017/3/10.
 */
public class SaveJpgThread extends Thread {
    private static final String TAG = "rustApp";

    private byte[] data;

    public SaveJpgThread(byte[] data) {
        this.data = data;
    }

    @Override
    public void run() {
        super.run();
        File jpg = new File(AppConfigs.IMG_DIR + File.separator +
                "img_" + System.currentTimeMillis() + ".jpg");
        if (jpg.exists()) {
            jpg.delete();
        }
        try {
            jpg.createNewFile();
            Log.d(TAG, "save new file: " + jpg.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(jpg);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jpg.exists()) {
            Intent intent = new Intent(AppConfigs.MSG_ONE_CAMERA);
            intent.putExtra(AppConfigs.MSG_ONE_CAMERA, jpg.getAbsolutePath());
            WiFiApp.getApp().sendBroadcast(intent);
        }
    }
}
