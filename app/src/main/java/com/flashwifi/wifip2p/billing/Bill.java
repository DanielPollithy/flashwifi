package com.flashwifi.wifip2p.billing;


public class Bill {
    private int index;
    private int megabytesUsed;
    private int priceInIota;
    private long duranceInSeconds;
    private long time;

    private boolean acceptedByPeer;

    public Bill(int index, long time, long duranceInSeconds, int megabytesUsed, int priceInIota) {
        this.index = index;
        this.time = time;
        this.duranceInSeconds = duranceInSeconds;
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


    public int getDuranceInMinutes() {
        return (int) duranceInSeconds/60;
    }

    public int getDuranceInSeconds() {
        return (int) duranceInSeconds;
    }

    public int getMegabytesUsed() {
        return megabytesUsed;
    }

    public int getPriceInIota() {
        return priceInIota;
    }

    public long getTime() {
        return time;
    }
}
