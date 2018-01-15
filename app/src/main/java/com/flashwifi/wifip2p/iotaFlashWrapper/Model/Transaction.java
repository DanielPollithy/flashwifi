package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.HashMap;
import java.util.Map;

public class Transaction {
    private String hash;
    private String signatureFragments;
    private String address;
    private long value;
    private String obsoleteTag;
    private long timestamp;
    private long currentIndex;
    private long lastIndex;
    private String bundle;
    private String trunkTransaction;
    private String branchTransaction;
    private String nonce;
    private Boolean persistence;
    private long attachmentTimestamp;
    private String tag;
    private long attachmentTimestampLowerBound;
    private long attachmentTimestampUpperBound;


    // Unsigned constructor
    public Transaction(String address, int value, String obsoleteTag, String tag, Integer timestamp) {
        this.address = address;
        this.value = value;
        this.obsoleteTag = obsoleteTag;
        this.tag = tag;
        this.timestamp = timestamp;
    }

    public Transaction(String signatureFragments, Long currentIndex, Long lastIndex, String nonce,
                       String hash, String obsoleteTag, Long timestamp, String trunkTransaction,
                       String branchTransaction, String address, Long value, String bundle, String tag,
                       Long attachmentTimestamp, Long attachmentTimestampLowerBound, Long attachmentTimestampUpperBound) {

        this.hash = hash;
        this.obsoleteTag = obsoleteTag;
        this.signatureFragments = signatureFragments;
        this.address = address;
        this.value = value;
        this.timestamp = timestamp;
        this.currentIndex = currentIndex;
        this.lastIndex = lastIndex;
        this.bundle = bundle;
        this.trunkTransaction = trunkTransaction;
        this.branchTransaction = branchTransaction;
        this.tag = tag;
        this.attachmentTimestamp = attachmentTimestamp;
        this.attachmentTimestampLowerBound = attachmentTimestampLowerBound;
        this.attachmentTimestampUpperBound = attachmentTimestampUpperBound;
        this.nonce = nonce;
    }


    public String getSignatureFragments() {
        return signatureFragments;
    }

    public void setSignatureFragments(String signatureFragments) {
        this.signatureFragments = signatureFragments;
    }

    public long getValue() {
        return value;
    }

    public String getAddress() {
        return address;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        if (hash != null && !hash.equals("")) {
            map.put("hash", hash);
        }
        map.put("signatureMessageFragment", signatureFragments);
        map.put("address", address);
        map.put("value", value);
        map.put("obsoleteTag", obsoleteTag);
        map.put("currentIndex", currentIndex);
        map.put("timestamp", timestamp);
        map.put("lastIndex", lastIndex);
        map.put("bundle", bundle);
        map.put("trunkTransaction", trunkTransaction);
        map.put("branchTransaction", branchTransaction);
        map.put("nonce", nonce);
        map.put("attachmentTimestamp", String.valueOf(attachmentTimestamp));
        map.put("tag", tag);
        map.put("attachmentTimestampLowerBound", String.valueOf(attachmentTimestampLowerBound));
        map.put("attachmentTimestampUpperBound", String.valueOf(attachmentTimestampUpperBound));
        return map;
    }

    public Transaction clone() {
        return new Transaction(
            this.signatureFragments,
            this.currentIndex,
            this.lastIndex,
            this.nonce,
            this.hash,
            this.obsoleteTag,
            this.timestamp,
            this.trunkTransaction,
            this.branchTransaction,
            this.address,
            this.value,
            this.bundle,
            this.tag,
            this.attachmentTimestamp,
            this.attachmentTimestampLowerBound,
            this.attachmentTimestampUpperBound
        );
    }

    public String toString() {
        Map<String, Object> mapObj = toMap();
        String value = "{";
        for (Map.Entry<String, Object> entry: mapObj.entrySet()) {
            value += "'" + entry.getKey() + "':'" + entry.getValue().toString() + "', \n";
        }
        value += "}";
        return  value;
    }
}