package com.flashwifi.wifip2p.billing;


import java.util.ArrayList;

public class Accountant {
    private static final Accountant ourInstance = new Accountant();


    private ArrayList<Bill> bills;
    private int totalBytes;
    private int totalIotaPrice;
    private int totalDurance;
    private int bookedMegabytes;
    private int bookedMinutes;
    private int totalIotaDeposit;
    private int timeoutMinutes;
    private int iotaPerMegaByte;
    private boolean closeAfterwards;
    private long startTime;

    private FlashChannelHelper flashChannelHelper;

    private boolean closed = true;
    private String seed;

    public static Accountant getInstance() {
        return ourInstance;
    }

    private Accountant() {
    }

    public void start(int bookedMegabytes, int timeoutMinutes, int bookedMinutes, int totalIotaDeposit, int iotaPerMegaByte){
        bills = new ArrayList<Bill>();
        this.bookedMegabytes = bookedMegabytes;
        this.timeoutMinutes = timeoutMinutes;
        this.totalIotaDeposit = totalIotaDeposit;
        this.iotaPerMegaByte = iotaPerMegaByte;
        totalBytes = 0;
        totalIotaPrice = 0;
        totalDurance = 0;
        flashChannelHelper = new FlashChannelHelper();
        closeAfterwards = false;
        startTime = System.currentTimeMillis() / 1000L;
        this.bookedMinutes = bookedMinutes;
        closed = false;
    }

    public long getLastTime() {
        if (bills.isEmpty()) {
            return startTime;
        } else {
            return bills.get(bills.size()-1).getTime();
        }
    }

    public void reset() {
        this.bookedMegabytes = 0;
        this.timeoutMinutes = 0;
        this.totalIotaDeposit = 0;
        this.iotaPerMegaByte = 0;
        totalBytes = 0;
        totalIotaPrice = 0;
        totalDurance = 0;
        bookedMinutes = 0;
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

        totalBytes += b.getBytesUsed();
        totalIotaPrice += b.getPriceInIota();
        totalDurance += b.getDuranceInSeconds();

        // ToDo: apply transfer to flash channel

        return true;
    }

    public Bill createBill(int bytes){
        if (!closed) {
            long now = System.currentTimeMillis() / 1000L;
            long duranceInSeconds = now - getLastTime();
            int priceInIota = (int) (bytes * getIotaPerByte());
            Bill b = new Bill(getNextBillNumber(), now, duranceInSeconds, bytes, priceInIota);

            totalBytes += bytes;
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

    public boolean shouldCloseChannel() {
        if (getTotalBytes() >= getBookedBytes()) {
            return true;
        }
        if ((Accountant.getInstance().getTotalDurance() / 60) >= Accountant.getInstance().getBookedMinutes()) {
            return true;
        }
        if (Accountant.getInstance().getTotalIotaPrice() >= Accountant.getInstance().getTotalIotaDeposit()) {
            return true;
        }
        return false;
    }

    public int getTotalMegabytes() {
        return totalBytes / (1024*1024);
    }

    public int getTotalBytes() {
        return totalBytes / (1024*1024);
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

    public int getBookedBytes() {
        return bookedMegabytes * 1000000;
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

    public int getIotaPerMegaByte() {
        return iotaPerMegaByte;
    }

    public double getIotaPerByte() {
        return ((double)getIotaPerMegaByte()) / (1024.0d * 1024.0d);
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getSeed() {
        return seed;
    }
}
