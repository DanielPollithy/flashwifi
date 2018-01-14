package com.flashwifi.wifip2p.protocol;


public class BillingOpenChannelAnswer {
    private int hotspotDepositIota;
    private int clientDepositIota;
    private int timeoutMinutesHotspot;
    private String hotspotRefundAddress;
    private String channelRootAddress;
    private String[] hotspotDigests;

    public BillingOpenChannelAnswer(int hotspotDepositIota, int clientDepositIota, String hotspotRefundAddress, String channelRootAddress, String[] hotspotDigests) {
        this.hotspotDepositIota = hotspotDepositIota;
        this.clientDepositIota = clientDepositIota;
        this.hotspotRefundAddress = hotspotRefundAddress;
        this.channelRootAddress = channelRootAddress;
        this.hotspotDigests = hotspotDigests;
    }

    public int getHotspotDepositIota() {
        return hotspotDepositIota;
    }

    public int getClientDepositIota() {
        return clientDepositIota;
    }

    public int getTimeoutMinutesHotspot() {
        return timeoutMinutesHotspot;
    }

    public String getHotspotRefundAddress() {
        return hotspotRefundAddress;
    }

    public String getChannelRootAddress() {
        return channelRootAddress;
    }

    public String[] getHotspotDigests() {
        return hotspotDigests;
    }
}
