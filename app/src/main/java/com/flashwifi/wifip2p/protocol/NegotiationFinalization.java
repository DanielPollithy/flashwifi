package com.flashwifi.wifip2p.protocol;


public class NegotiationFinalization {
    private String type;
    private String hotspotName;
    private String hotspotPassword;
    private String depositAddressFlashChannel;
    private String hotspotSettlementAddress;
    private int depositServerFlashChannelInIota;
    private int depositClientFlashChannelInIota;

    public NegotiationFinalization(String hotspotName, String hotspotPassword, String depositAddressFlashChannel, int depositServerFlashChannelInIota, int depositClientFlashChannelInIota, String hotspotSettlementAddress) {
        this.type = "negotiationFinalization";
        this.hotspotName = hotspotName;
        this.hotspotPassword = hotspotPassword;
        this.depositAddressFlashChannel = depositAddressFlashChannel;
        this.depositServerFlashChannelInIota = depositServerFlashChannelInIota;
        this.depositClientFlashChannelInIota = depositClientFlashChannelInIota;
        this.hotspotSettlementAddress = hotspotSettlementAddress;
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

    public String getHotspotSettlementAddress() {
        return hotspotSettlementAddress;
    }
}
