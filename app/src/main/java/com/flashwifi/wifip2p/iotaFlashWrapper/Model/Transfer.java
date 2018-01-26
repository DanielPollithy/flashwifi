package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Transfer extends jota.model.Transfer {


    /**
     * Initializes a new instance of the Transfer class.
     */
    public Transfer(String timestamp, String address, String hash, Boolean persistence, long value, String message,
                    String tag) {
        super(timestamp, address, hash, persistence,value,message,tag);
    }

    /**
     * Initializes a new instance of the Transfer class.
     */
    public Transfer(String address, long value) {
        super(address, value);

    }

    /**
     * Initializes a new instance of the Transfer class.
     */
    public Transfer(String address, long value, String message, String tag) {
        super(address, value, message, tag);
    }

    /**
     * Returns a Json Object that represents this object.
     *
     * @return Returns a string representation of this object.
     */
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("timestamp", getTimestamp());
        map.put("address", getAddress());
        map.put("hash", getHash());
        map.put("persistance", getPersistence());
        map.put("value", getValue());
        map.put("message", getMessage());
        map.put("tag", getTag());
        return map;

    }
}