package com.flashwifi.wifip2p.protocol;


import com.flashwifi.wifip2p.billing.Bill;

public class BillMessageAnswer {
    private String billId;
    private boolean accepted;
    // ToDo: Replace this by a real flash instance
    private String flashObject;
    private boolean closeAfterwards;

    public BillMessageAnswer(String billId, boolean accepted, String flashObject,
                             boolean closeAfterwards) {
        this.billId = billId;
        this.accepted = accepted;
        this.flashObject = flashObject;
        this.closeAfterwards = closeAfterwards;
    }

    public String getBillId() {
        return billId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getFlashObject() {
        return flashObject;
    }

    public boolean isCloseAfterwards() {
        return closeAfterwards;
    }
}
