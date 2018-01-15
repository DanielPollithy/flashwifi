package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;

public class FlashObject {
    int signersCount = 2;
    int balance;
    ArrayList<String> settlementAddresses;
    ArrayList<Integer> deposits; // Clone correctly
    ArrayList<Bundle> outputs = new ArrayList<Bundle>();
    ArrayList<Bundle> transfers = new ArrayList<Bundle>();
    MultisigAddress root;
    MultisigAddress remainderAddress;


    public FlashObject(int signersCount, int balance, ArrayList<Integer> deposits) {
        this.signersCount = signersCount;
        this.balance = balance;
        this.deposits = deposits;
    }

    public FlashObject(int signersCount, int balance, ArrayList<String> settlementAddresses, ArrayList<Integer> deposits, ArrayList<Bundle> outputs, ArrayList<Bundle> transfers, MultisigAddress root, MultisigAddress remainderAddress) {
        this.signersCount = signersCount;
        this.balance = balance;
        this.settlementAddresses = settlementAddresses;
        this.deposits = deposits;
        this.outputs = outputs;
        this.transfers = transfers;
        this.root = root;
        this.remainderAddress = remainderAddress;
    }

    @Override
    public String toString() {
        String out = "";
        out += "signersCount: " + signersCount + "\n";
        out += "balance: " + balance + "\n";
        out += "settlementAddresses: " + "\n";
        for (String b: settlementAddresses) {
            out += "\t" + b + "\n";
        }
        out += "deposits: " + "\n";
        for (Integer b: deposits) {
            out += "\t" + b + "\n";
        }

        out += "outputs: " + "\n";
        for (Bundle b: outputs) {
            out += "\t" + b.toString() + "\n";
        }

        out += "transfers: " + "\n";
        for (Bundle b: transfers) {
            out += "\t" + b.toString() + "\n";
        }
        out += "remainderAddress: " + remainderAddress.toString() + "\n";
        out += "root: " + root.toString() + "\n";

        return out;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("signersCount", signersCount);
        objectMap.put("balance", getBalance());
        objectMap.put("root", root.toMap());
        objectMap.put("remainderAddress", remainderAddress.toMap());
        objectMap.put("settlementAddresses", getSettlementAddresses());

        ArrayList<Object> outputMap = new ArrayList<>();
        for (Bundle b: outputs) {
            outputMap.add(b.toMap());
        }
        objectMap.put("outputs", outputMap);

        objectMap.put("deposits", getDeposits());

        ArrayList<Object> transfersMap = new ArrayList<>();
        for (Bundle b: transfers) {
            outputMap.add(b.toMap());
        }
        objectMap.put("transfers", transfersMap);
        return objectMap;

    }

    public int getSignersCount() {
        return signersCount;
    }

    public int getBalance() {
        return balance;
    }

    public MultisigAddress getRoot() {
        return root;
    }

    public ArrayList<Integer> getDeposits() {
        return deposits;
    }

    public ArrayList<Bundle> getOutputs() {
        return outputs;
    }

    public ArrayList<Bundle> getTransfers() {
        return transfers;
    }

    public void setRemainderAddress(MultisigAddress remainderAddress) {
        this.remainderAddress = remainderAddress;
    }

    public MultisigAddress getRemainderAddress() {
        return remainderAddress;
    }

    public void setRoot(MultisigAddress root) {
        this.root = root;
    }

    public void setSettlementAddresses(ArrayList<String> settlementAddresses) {
        this.settlementAddresses = settlementAddresses;
    }

    public ArrayList<String> getSettlementAddresses() {
        return settlementAddresses;
    }
}
