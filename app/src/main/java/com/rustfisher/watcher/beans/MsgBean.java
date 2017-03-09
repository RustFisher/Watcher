package com.rustfisher.watcher.beans;

import java.io.Serializable;

/**
 * Package the information
 * Created by Rust Fisher on 2017/3/9.
 */
public final class MsgBean implements Serializable {


    private static final long serialVersionUID = 2817564515497626133L;

    public static final int TYPE_TEXT = 0x0001;
    public static final int TYPE_PNG = 0x0010;

    private int types = 0;

    private String msg;
    private byte[] pngBytes;

    public MsgBean(String msg) {
        this.msg = msg;
        types |= TYPE_TEXT;
    }

    public MsgBean(byte[] d, int type) {
        pngBytes = d;
        types |= type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
        types |= TYPE_TEXT;
    }

    public byte[] getPNGBytes() {
        return this.pngBytes;
    }

    public boolean hasText() {
        return (TYPE_TEXT & types) == TYPE_TEXT;
    }

    public boolean hasPNG() {
        return (TYPE_PNG & types) == TYPE_PNG;
    }
}
