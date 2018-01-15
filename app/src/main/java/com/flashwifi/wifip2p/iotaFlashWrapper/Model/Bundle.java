package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Bundle {
    private ArrayList<Transaction> bundles;

    public Bundle(ArrayList<Transaction> bundles) {
        this.bundles = bundles;
    }

    public Bundle() {
        this.bundles = new ArrayList<>();
    }

    @Override
    public String toString() {
        String out = "";
        for (Transaction t: bundles) {
            out += t.toString();
            out += "\n";
        }
        return out;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        List<Object> bundleList = new ArrayList<Object>();
        for (Transaction b: bundles) {
            bundleList.add(b.toMap());
        }
        map.put("bundles", bundleList);
        return map;
    }

    public ArrayList<Transaction> getBundles() {
        return bundles;
    }

    public  Bundle clone() {
        ArrayList<Transaction> clonedTransactions = new ArrayList<>();
        for (Transaction t: bundles) {
            clonedTransactions.add(t.clone());
        }
        return new Bundle(clonedTransactions);
    }
}


