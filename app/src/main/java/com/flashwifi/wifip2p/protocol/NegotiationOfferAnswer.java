package com.flashwifi.wifip2p.protocol;


public class NegotiationOfferAnswer {
    private String type;
    private boolean agreeToConditions;
    private int duranceInMinutes;

    public NegotiationOfferAnswer(boolean agreeToConditions, int duranceInMinutes) {
        this.type = "answerToOffer";
        this.agreeToConditions = agreeToConditions;
        this.duranceInMinutes = duranceInMinutes;
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
}
