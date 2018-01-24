package com.flashwifi.wifip2p.iotaFlashWrapper.Model;
import java.util.HashMap;
import java.util.Map;

public class Digest {
    private int index;
    private int security;
    private String digest;

    public Digest(String digest, int index, int security) {
        this.digest = digest;
        this.index = index;
        this.security = security;
    }

    public int getIndex() {
        return index;
    }
    public String getDigest() {
        return digest;
    }

    public int getSecurity() {
        return security;
    }

    public String toString() {
        return "{'digest':'"+ getDigest() + "', 'index':" + getIndex() + ", 'security':" + getSecurity() + "}";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("index", getIndex());
        map.put("digest", getDigest());
        map.put("security", getSecurity());
        return  map;
    }

}
