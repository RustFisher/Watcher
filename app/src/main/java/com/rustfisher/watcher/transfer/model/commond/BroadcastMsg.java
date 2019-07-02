package com.rustfisher.watcher.transfer.model.commond;

import com.rustfisher.watcher.transfer.model.BaseMsg;

/**
 * 广播信息
 */
public final class BroadcastMsg extends BaseMsg {

    private String lan_ipv4;
    private String nickname; // 手机昵称

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "[BroadcastMsg] cmd: " + cmd + ", lan_ipv4: " + lan_ipv4 + ", nickname:" + nickname
                + ", msg: " + msg;
    }
}
