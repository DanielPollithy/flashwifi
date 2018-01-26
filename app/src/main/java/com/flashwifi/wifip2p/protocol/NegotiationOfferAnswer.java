package com.flashwifi.wifip2p.protocol;


public class NegotiationOfferAnswer {
    private String type;
    private boolean agreeToConditions;
    private int duranceInMinutes;
    private String consumerMac;
    private String clientSettlementAddress;

    public NegotiationOfferAnswer(boolean agreeToConditions, int duranceInMinutes, String consumerMac, String clientSettlementAddress) {
        this.type = "answerToOffer";
        this.agreeToConditions = agreeToConditions;
        this.duranceInMinutes = duranceInMinutes;
        this.consumerMac = consumerMac;
        this.clientSettlementAddress = clientSettlementAddress;
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

    public String getClientSettlementAddress() {
        return clientSettlementAddress;
    }
}
