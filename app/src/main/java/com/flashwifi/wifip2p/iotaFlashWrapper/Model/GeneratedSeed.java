package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

public class GeneratedSeed {
    private String address;
    private String seed;
    private long amount;

    public GeneratedSeed(String address, String seed, long amount) {
        this.address = address;
        this.seed = seed;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
