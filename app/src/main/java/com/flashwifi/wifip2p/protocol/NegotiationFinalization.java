package com.flashwifi.wifip2p.protocol;


public class NegotiationFinalization {
    private String type;
    private String hotspotName;
    private String hotspotPassword;
    private String depositAddressFlashChannel;
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
}
