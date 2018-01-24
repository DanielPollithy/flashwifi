package com.flashwifi.wifip2p.billing;


public class Bill {
    private int index;
    private int bytesUsed;
    private int priceInIota;
    private long duranceInSeconds;
    private long time;

    private boolean acceptedByPeer;

    public Bill(int index, long time, long duranceInSeconds, int bytesUsed, int priceInIota) {
        this.index = index;
        this.time = time;
        this.duranceInSeconds = duranceInSeconds;
        this.bytesUsed = bytesUsed;
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

    public int getBytesUsed() {
        return bytesUsed;
    }

    public int getPriceInIota() {
        return priceInIota;
    }

    public long getTime() {
        return time;
    }
}
