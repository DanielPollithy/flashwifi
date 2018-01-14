package com.flashwifi.wifip2p.protocol;


import com.flashwifi.wifip2p.billing.Bill;

public class BillMessage {
    private Bill bill;
    // ToDo: Replace this by a real flash instance
    private String flashObject;
    private boolean closeAfterwards;

    public BillMessage(Bill bill, String flashObject, boolean closeAfterwards) {
        this.bill = bill;
        this.flashObject = flashObject;
        this.closeAfterwards = closeAfterwards;
    }

    public Bill getBill() {
        return bill;
    }

    public String getFlashObject() {
        return flashObject;
    }

    public boolean isCloseAfterwards() {
        return closeAfterwards;
    }
}
