package com.flashwifi.wifip2p.protocol;


public class NegotiationOffer {
    private String type;
    private int minMinutes;
    private int maxMinutes;
    private int iotaPerMegabyte;
    private String hotspotMac;

    public NegotiationOffer(int minMinutes, int maxMinutes, int iotaPerMegabyte, String hotspotMac) {
        this.type = "offer";
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
        this.iotaPerMegabyte = iotaPerMegabyte;
        this.hotspotMac = hotspotMac;
    }

    public String getType() {
        return type;
    }

    public int getMinMinutes() {
        return minMinutes;
    }

    public int getMaxMinutes() {
        return maxMinutes;
    }

    public int getIotaPerMegabyte() {
        return iotaPerMegabyte;
    }

    public String getHotspotMac() {
        return hotspotMac;
    }
}
