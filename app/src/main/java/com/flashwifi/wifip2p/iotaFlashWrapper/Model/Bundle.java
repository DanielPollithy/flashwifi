package com.flashwifi.wifip2p.iotaFlashWrapper.Model;

import com.flashwifi.wifip2p.iotaFlashWrapper.V8Converter;
import com.flashwifi.wifip2p.iotaFlashWrapper.Helpers;
import jota.model.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bundle extends jota.model.Bundle {

    public Bundle(List<Transaction> transactions) {
        super(transactions, transactions.size());
    }

    public Bundle() {
        super();
    }


    @Override
    public String toString() {
        String out = "";
        for (Transaction t: getTransactions()) {
            out += t.toString();
            out += "\n";
        }
        return out;
    }

    public String[] toTrytesArray() {
        String[] bundleTrytes = new String[getTransactions().size()];
        List<jota.model.Transaction> transactions = getTransactions();
        for (int i = 0; i < bundleTrytes.length; i++) {
            bundleTrytes[(bundleTrytes.length - 1) - i] =  transactions.get(i).toTrytes();
        }

        return bundleTrytes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("bundles", toArrayList());
        return map;
    }

    public List<Object> toArrayList() {
        List<Object> bundleList = new ArrayList<Object>();
        for (Transaction tx: getTransactions()) {
            bundleList.add(V8Converter.transactionToMap(tx));
        }
        return bundleList;
    }

    public Bundle clone() {
        ArrayList<Transaction> clonedTransactions = new ArrayList<>();
        for (Transaction tx: getTransactions()) {
            clonedTransactions.add(Helpers.cloneTransaction(tx));
        }
        return new Bundle(clonedTransactions);
    }
}


