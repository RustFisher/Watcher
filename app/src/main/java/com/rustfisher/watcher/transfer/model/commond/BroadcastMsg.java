package com.rustfisher.watcher.transfer.model.commond;

import com.rustfisher.watcher.transfer.model.BaseMsg;

/**
 * 广播信息
 */
public final class BroadcastMsg extends BaseMsg {

    /**
     * cmd : 255
     * lan_ipv4 : 192.168.0.2
     * msg : Anyone hear me?
     */

    private String lan_ipv4;

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getLan_ipv4() {
        return lan_ipv4;
    }

    public void setLan_ipv4(String lan_ipv4) {
        this.lan_ipv4 = lan_ipv4;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "[BroadcastMsg] cmd: " + cmd + ", lan_ipv4: " + lan_ipv4 + ", msg: " + msg;
    }
}
