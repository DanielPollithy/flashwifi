package com.flashwifi.wifip2p.billing;


import java.util.ArrayList;

public class Accountant {
    private static final Accountant ourInstance = new Accountant();


    private ArrayList<Bill> bills;
    private int totalMegabytes;
    private int totalIotaPrice;
    private int totalDurance;
    private int bookedMegabytes;
    private int bookedMinutes;
    private int totalIotaDeposit;
    private int timeoutMinutes;
    private boolean closeAfterwards;
    private long startTime;

    private FlashChannelHelper flashChannelHelper;

    private boolean closed = true;

    public static Accountant getInstance() {
        return ourInstance;
    }

    private Accountant() {
    }

    public void start(int bookedMegabytes, int timeoutMinutes, int bookedMinutes, int totalIotaDeposit){
        if (closed) {
            bills = new ArrayList<Bill>();
            this.bookedMegabytes = bookedMegabytes;
            this.timeoutMinutes = timeoutMinutes;
            this.totalIotaDeposit = totalIotaDeposit;
            totalMegabytes = 0;
            totalIotaPrice = 0;
            totalDurance = 0;
            flashChannelHelper = new FlashChannelHelper();
            closeAfterwards = false;
            startTime = System.currentTimeMillis() / 1000L;
            this.bookedMinutes = bookedMinutes;
            closed = false;
        }
    }

    private long getLastTime() {
        if (bills.isEmpty()) {
            return startTime;
        } else {
            return bills.get(bills.size()-1).getTime();
        }
    }

    public boolean isCloseAfterwards() {
        return closeAfterwards;
    }

    public void setCloseAfterwards(boolean closeAfterwards) {
        this.closeAfterwards = closeAfterwards;
    }

    public int getNextBillNumber() {
        return bills.size() + 1;
    }

    private void appendBill(Bill b) {
        bills.add(b);
    }

    private void applyTransferToFlashChannel(int iota) {
    }

    public boolean includeBillFromPeer(Bill b) {
        appendBill(b);

        // ToDo: check bill

        totalMegabytes += b.getMegabytesUsed();
        totalIotaPrice += b.getPriceInIota();
        totalDurance += b.getDuranceInSeconds();

        // ToDo: apply transfer to flash channel

        return true;
    }

    public Bill createBill(int megaByte, int priceInIota){
        if (!closed) {
            long now = System.currentTimeMillis() / 1000L;
            long duranceInSeconds = now - getLastTime();
            Bill b = new Bill(getNextBillNumber(), now, duranceInSeconds, megaByte, priceInIota);

            totalMegabytes += megaByte;
            totalIotaPrice += priceInIota;
            totalDurance += duranceInSeconds;

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

    public int getTotalMegabytes() {
        return totalMegabytes;
    }

    public int getTotalIotaPrice() {
        return totalIotaPrice;
    }

    public int getTotalDurance() {
        return totalDurance;
    }

    public int getBookedMegabytes() {
        return bookedMegabytes;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public int getBookedMinutes() {
        return bookedMinutes;
    }

    public int getTotalIotaDeposit() {
        return totalIotaDeposit;
    }
}
