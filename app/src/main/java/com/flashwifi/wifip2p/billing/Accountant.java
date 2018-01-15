package com.flashwifi.wifip2p.billing;


import java.util.ArrayList;

public class Accountant {
    private static final Accountant ourInstance = new Accountant();


    private ArrayList<Bill> bills;
    private int totalMegabytes;
    private int totalIotaPrice;
    private int totalDurance;
    private int bookedMegabytes;
    private int timeoutMinutes;

    private FlashChannelHelper flashChannelHelper;

    private boolean closed = true;

    static Accountant getInstance() {
        return ourInstance;
    }

    private Accountant() {
    }

    public void start(int bookedMegabytes, int timeoutMinutes){
        if (closed) {
            bills = new ArrayList<Bill>();
            this.bookedMegabytes = bookedMegabytes;
            this.timeoutMinutes = timeoutMinutes;
            totalMegabytes = 0;
            totalIotaPrice = 0;
            totalDurance = 0;
            flashChannelHelper = new FlashChannelHelper();
            closed = false;
        }
    }

    public int getNextBillNumber() {
        return bills.size() + 1;
    }

    private void appendBill(Bill b) {
        bills.add(b);
    }

    private void applyTransferToFlashChannel(int iota) {

    }

    public Bill createBill(int megaByte, int priceInIota, int duranceMinutes){
        if (!closed) {
            Bill b = new Bill(getNextBillNumber(), totalDurance, duranceMinutes, megaByte, priceInIota);

            totalMegabytes += megaByte;
            totalIotaPrice += priceInIota;
            totalDurance += duranceMinutes;

            // 1) modify flash channel
            applyTransferToFlashChannel(priceInIota);

            appendBill(b);
            return b;
        }
        return null;
    }

    public void close() {
        if (!closed) {
            closed = true;
        }
    }

    public void markAcceptance(int index, boolean accepted) {
        bills.get(index).setAcceptedByPeer(accepted);
    }

    public boolean billsNotAccepted() {
        for (Bill b: bills) {
            if (!b.isAcceptedByPeer()) {
                return true;
            }
        }
        return false;
    }
}
