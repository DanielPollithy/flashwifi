package com.flashwifi.wifip2p.protocol;


import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Digest;

import java.util.List;

public class BillingOpenChannel {
    private int totalMegabytes;
    private int totalMinutes;
    private int iotaPerMegabyte;
    private int timeoutMinutesClient;
    private String clientRefundAddress;
    private int treeDepth;
    private List<Digest> clientDigests;

    public BillingOpenChannel(int totalMegabytes, int iotaPerMegabyte,
                              int treeDepth, List<Digest> clientDigests, int timeoutMinutesClient,
                              int totalMinutes) {
        this.totalMegabytes = totalMegabytes;
        this.totalMinutes = totalMinutes;
        this.iotaPerMegabyte = iotaPerMegabyte;
        this.clientRefundAddress = clientRefundAddress;
        this.treeDepth = treeDepth;
        this.clientDigests = clientDigests;
        this.timeoutMinutesClient = timeoutMinutesClient;
    }

    public int getTimeoutMinutesClient() {
        return timeoutMinutesClient;
    }

    public int getTotalMegabytes() {
        return totalMegabytes;
    }

    public int getIotaPerMegabyte() {
        return iotaPerMegabyte;
    }


    public int getTreeDepth() {
        return treeDepth;
    }

    public List<Digest> getClientDigests() {
        return clientDigests;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }
}
