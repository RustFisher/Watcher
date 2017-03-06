package com.rustfisher.watcher.utils;

/**
 * Say something.
 * Created by Rust Fisher on 2017/3/2.
 */
public final class WPProtocol {

    public static final String MY_IP_ADDRESS = "Here is my address";
    public static final byte DATA_HEAD_1 = (byte) 0x33;
    public static final byte DATA_HEAD_2 = (byte) 0x44;
    public static final byte DATA_TYPE_PIC = 0x02;
    public static final byte DATA_END_1 = (byte) 0x65;
    public static final byte DATA_END_2 = (byte) 0x66;
    public static final byte DATA_END_3 = (byte) 0x55;

    public static final byte[] DATA_HEAD_CAMERA = new byte[]{DATA_HEAD_1, DATA_HEAD_2};
    public static final byte[] DATA_HEAD_ONE_PIC = new byte[]{DATA_HEAD_1, DATA_HEAD_2, DATA_TYPE_PIC};
    public static final byte[] DATA_END = new byte[]{DATA_END_1, DATA_END_2, DATA_END_3};

}
