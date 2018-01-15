package com.flashwifi.wifip2p.protocol;


public class BillingOpenChannel {
    private int totalMegabytes;
    private int iotaPerMegabyte;
    private int timeoutMinutesClient;
    private String clientRefundAddress;
    private int treeDepth;
    private String[] clientDigests;

    public BillingOpenChannel(int totalMegabytes, int iotaPerMegabyte, String clientRefundAddress,
                              int treeDepth, String[] clientDigests, int timeoutMinutesClient) {
        this.totalMegabytes = totalMegabytes;
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

    public String getClientRefundAddress() {
        return clientRefundAddress;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

    public String[] getClientDigests() {
        return clientDigests;
    }
}
