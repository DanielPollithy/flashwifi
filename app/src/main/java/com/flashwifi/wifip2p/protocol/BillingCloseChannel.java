package com.flashwifi.wifip2p.protocol;


public class BillingCloseChannel {
    private int totalIotaRemainder;
    private int iotaRemainderClient;
    private int iotaRemainderHotspot;

    private int unconfirmedBills;

    // was the flash channel full, the connection dropped or the attachmentTimeout reached
    private String closeReason;

    private String clientRefundAddress;
    private String hotspotRefundAddress;

    public BillingCloseChannel(int totalIotaRemainder, int iotaRemainderClient, int iotaRemainderHotspot, int unconfirmedBills, String closeReason, String clientRefundAddress, String hotspotRefundAddress) {
        this.totalIotaRemainder = totalIotaRemainder;
        this.iotaRemainderClient = iotaRemainderClient;
        this.iotaRemainderHotspot = iotaRemainderHotspot;
        this.unconfirmedBills = unconfirmedBills;
        this.closeReason = closeReason;
        this.clientRefundAddress = clientRefundAddress;
        this.hotspotRefundAddress = hotspotRefundAddress;
    }
}
