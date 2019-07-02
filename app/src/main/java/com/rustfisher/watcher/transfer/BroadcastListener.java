package com.rustfisher.watcher.transfer;

import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;

import java.util.List;

/**
 * 收听广播的监听器
 * Created on 2019-7-2
 */
public abstract class BroadcastListener {

    public abstract void onDeviceList(List<BroadcastMsg> list);

}
