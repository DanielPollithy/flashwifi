package com.flashwifi.wifip2p.protocol;


public class NegotiationOfferAnswer {
    private String type;
    private boolean agreeToConditions;
    private int duranceInMinutes;
    private String consumerMac;

    public NegotiationOfferAnswer(boolean agreeToConditions, int duranceInMinutes, String consumerMac) {
        this.type = "answerToOffer";
        this.agreeToConditions = agreeToConditions;
        this.duranceInMinutes = duranceInMinutes;
        this.consumerMac = consumerMac;
    }

    public String getType() {
        return type;
    }

    public boolean isAgreeToConditions() {
        return agreeToConditions;
    }

    public int getDuranceInMinutes() {
        return duranceInMinutes;
    }

    public String getConsumerMac() {
        return consumerMac;
    }
}
