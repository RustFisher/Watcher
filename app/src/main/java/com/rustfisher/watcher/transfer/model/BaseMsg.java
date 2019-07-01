package com.rustfisher.watcher.transfer.model;

/**
 * 基础消息模型
 * Created on 2019-7-1
 */
public class BaseMsg {
    protected int cmd;
    protected String msg;

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "] cmd: " + cmd + ", msg: " + msg;
    }
}
