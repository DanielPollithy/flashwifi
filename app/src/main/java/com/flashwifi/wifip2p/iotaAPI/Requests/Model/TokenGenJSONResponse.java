package com.flashwifi.wifip2p.iotaAPI.Requests.Model;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class TokenGenJSONResponse extends GenericJson {

    @Key("seed")
    private String seed;
    @Key("address")
    private String address;
    @Key("amount")
    private Integer amount;

    private String success;

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
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

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }
}
