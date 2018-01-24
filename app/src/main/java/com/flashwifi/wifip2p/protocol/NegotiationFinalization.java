package com.flashwifi.wifip2p.protocol;


public class NegotiationFinalization {
    private String type;
    private String hotspotName;
    private String hotspotPassword;
    private String depositAddressFlashChannel;
    private String clientRefundAddress;
    private String hotspotRefundAddress;
    private int depositServerFlashChannelInIota;
    private int depositClientFlashChannelInIota;
    private String flashObject;

    public NegotiationFinalization(String hotspotName, String hotspotPassword, String depositAddressFlashChannel, int depositServerFlashChannelInIota, int depositClientFlashChannelInIota, String flashObject) {
        this.type = "negotiationFinalization";
        this.hotspotName = hotspotName;
        this.hotspotPassword = hotspotPassword;
        this.depositAddressFlashChannel = depositAddressFlashChannel;
        this.depositServerFlashChannelInIota = depositServerFlashChannelInIota;
        this.depositClientFlashChannelInIota = depositClientFlashChannelInIota;
        this.flashObject = flashObject;
    }

    public String getHotspotName() {
        return hotspotName;
    }

    public String getHotspotPassword() {
        return hotspotPassword;
    }

    public String getDepositAddressFlashChannel() {
        return depositAddressFlashChannel;
    }

    public int getDepositServerFlashChannelInIota() {
        return depositServerFlashChannelInIota;
    }

    public int getDepositClientFlashChannelInIota() {
        return depositClientFlashChannelInIota;
    }

    public String getFlashObject() {
        return flashObject;
    }

    public String getClientRefundAddress() {
        return clientRefundAddress;
    }

    public String getHotspotRefundAddress() {
        return hotspotRefundAddress;
    }
}
