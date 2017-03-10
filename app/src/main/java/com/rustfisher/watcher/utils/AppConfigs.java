package com.rustfisher.watcher.utils;

import android.os.Environment;

import java.io.File;

/**
 * Store some configs
 * Created by Rust Fisher on 2017/3/1.
 */
public final class AppConfigs {
    public static final int PORT_GROUP_OWNER = 8988;
    public static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "watcherFolder";
    public static final String IMG_DIR = ROOT_DIR + File.separator + "img";

    public static final String MSG_ONE_PIC = "com.rustfisher.msg_one_pic";
    public static final String MSG_ONE_CAMERA = "com.rustfisher.msg_camera";
    public static final String MSG_ONE_STR = "com.rustfisher.msg_one_str";
}
