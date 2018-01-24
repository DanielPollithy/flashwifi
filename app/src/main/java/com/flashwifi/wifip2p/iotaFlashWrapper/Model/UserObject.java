package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserObject {
    private int userIndex;
    private String seed;
    private int seedIndex;
    private int security;

    private FlashObject flash;

    public UserObject(int userID, String seed, int seedIndex, int security) {
        this.userIndex = userID;
        this.seed = seed;
        this.seedIndex = seedIndex;
        this.security = security;
    }

    public int incrementSeedIndex() {
        this.seedIndex = this.seedIndex + 1;
        return seedIndex;
    }

    @Override
    public String toString() {
        String out = "";
        out += "userIndex: " + userIndex + "\n";
        out += "seed: " + seed + "\n";
        out += "seedIndex: " + seedIndex + "\n";
        out += "security: " + getSecurity() + "\n";
        out += "bundles: " + "\n";
        out += "Flash: " + "\n";
        out += flash.toString();

        return out;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("userIndex", getUserIndex());
        objectMap.put("seed", getSeed());
        objectMap.put("seedIndex", getSeedIndex());
        objectMap.put("security", getSecurity());
        objectMap.put("flash", flash.toMap());
        return objectMap;
    }

    /**
     *
     * Getters and Setters
     */

    public void setFlash(FlashObject flash) {
        this.flash = flash;
    }

    public void setSeedIndex(int index) {
        this.seedIndex = index;
    }

    public int getSecurity() {
        return security;
    }

    public String getSeed() {
        return seed;
    }

    public int getSeedIndex() {
        return seedIndex;
    }

    public int getUserIndex() {
        return userIndex;
    }

    public FlashObject getFlash() {
        return flash;
    }

}

