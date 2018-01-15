package com.flashwifi.wifip2p.billing;


public class Bill {
    private int index;
    private int minuteStart;
    private int duranceInMinutes = 1;
    private int megabytesUsed;
    private int priceInIota;

    private boolean acceptedByPeer;

    public Bill(int index, int minuteStart, int duranceInMinutes, int megabytesUsed, int priceInIota) {
        this.index = index;
        this.minuteStart = minuteStart;
        this.duranceInMinutes = duranceInMinutes;
        this.megabytesUsed = megabytesUsed;
        this.priceInIota = priceInIota;
    }

    public boolean isAcceptedByPeer() {
        return acceptedByPeer;
    }

    public void setAcceptedByPeer(boolean acceptedByPeer) {
        this.acceptedByPeer = acceptedByPeer;
    }

    public int getIndex() {
        return index;
    }

    public int getMinuteStart() {
        return minuteStart;
    }

    public int getDuranceInMinutes() {
        return duranceInMinutes;
    }

    public int getMegabytesUsed() {
        return megabytesUsed;
    }

    public int getPriceInIota() {
        return priceInIota;
    }
}
