package com.flashwifi.wifip2p.protocol;


import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Digest;

import java.util.List;

public class BillingOpenChannelAnswer {
    private int hotspotDepositIota;
    private int clientDepositIota;
    private int timeoutMinutesHotspot;
    private String channelRootAddress;
    private List<Digest>  hotspotDigests;

    public BillingOpenChannelAnswer(int hotspotDepositIota, int clientDepositIota, String channelRootAddress, List<Digest> hotspotDigests) {
        this.hotspotDepositIota = hotspotDepositIota;
        this.clientDepositIota = clientDepositIota;
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

    public String getChannelRootAddress() {
        return channelRootAddress;
    }

    public List<Digest> getHotspotDigests() {
        return hotspotDigests;
    }
}
