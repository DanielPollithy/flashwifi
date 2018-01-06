package com.flashwifi.wifip2p.protocol;


public class NegotiationOffer {
    private String type;
    private int minMinutes;
    private int maxMinutes;
    private int iotaPerMegabyte;

    public NegotiationOffer(int minMinutes, int maxMinutes, int iotaPerMegabyte) {
        this.type = "offer";
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
        this.iotaPerMegabyte = iotaPerMegabyte;
    }
}
