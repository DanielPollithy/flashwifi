package com.flashwifi.wifip2p.iotaFlashWrapper;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V8Converter {
    public static ArrayList<Signature> v8ArrayToSignatureList(V8Array array) {
        // Try parsing signature array from response.
        ArrayList<Signature> signatures = new ArrayList<>();
        for (Object o: V8ObjectUtils.toList(array)) {
            Map<String, Object> returnValues = (Map<String, Object>) o;

            String addr = (String) returnValues.get("address");
            Integer index = (Integer) returnValues.get("index");
            String bundle = (String) returnValues.get("bundle");
            ArrayList<String> signatureFragments = (ArrayList<String>) returnValues.get("signatureFragments");

            Signature sig = new Signature();
            sig.setAddress(addr);
            sig.setSignatureFragments(signatureFragments);
            sig.setIndex(index);
            sig.setBundle(bundle);
            signatures.add(sig);

        }

        return signatures;
    }

    public static V8Object multisigToV8Object(V8 engine, MultisigAddress multisig) {
        Map<String, Object> sigMapg = multisig.toMap();
        return V8ObjectUtils.toV8Object(engine, sigMapg);
    }

    public static V8Object flashObjectToV8Object(V8 engine, FlashObject flash) {
        return V8ObjectUtils.toV8Object(engine, flash.toMap());
    }

    public static FlashObject flashObjectFromV8Object(V8Object input) {
        Map<String, Object> inputMap = V8ObjectUtils.toMap(input);

        Integer singersCount = (Integer) inputMap.get("signersCount");
        Integer balance = (Integer) inputMap.get("balance");
        ArrayList<String> settlementAddresses = (ArrayList<String>) inputMap.get("settlementAddresses");
        MultisigAddress root = multisigAddressFromPropertyMap((Map<String, Object>) inputMap.get("root"));
        MultisigAddress remainderAddress = multisigAddressFromPropertyMap((Map<String, Object>) inputMap.get("remainderAddress"));
        ArrayList<Integer> deposits = (ArrayList<Integer>) inputMap.get("deposits");
        ArrayList<Bundle> transfers = bundleListFromArrayList((ArrayList<Object>) inputMap.get("transfers"));
        ArrayList<Bundle> outputs = bundleListFromArrayList((ArrayList<Object>) inputMap.get("outputs"));

        return new FlashObject(singersCount, balance, settlementAddresses, deposits, outputs, transfers, root, remainderAddress);
    }

    public static V8Array bundleListToV8Array(V8 engine, ArrayList<Bundle> bundles) {

        List<Object> bundleTmp = new ArrayList<Object>();
        for (Bundle b: bundles) {
            List<Object> transactions = new ArrayList<Object>();
            for (Transaction t: b.getBundles()) {
                transactions.add(t.toMap());
            }
            bundleTmp.add(transactions);
        }
        return V8ObjectUtils.toV8Array(engine, bundleTmp);
    }

    public static Bundle bundleFromArrayList(ArrayList<Object> bundleObject) {
        ArrayList<Transaction> tr = new ArrayList<>();
        for (Object transaction: bundleObject) {
            tr.add(transactionFromObject(transaction));
        }
        return new Bundle(tr);
    }

    public static ArrayList<Bundle> bundleListFromArrayList(ArrayList<Object> input) {
        ArrayList<Bundle> ret = new ArrayList<>();

        for (Object o: input) {
            ret.add(bundleFromArrayList((ArrayList<Object>) o));
        }


        return ret;
    }

    public static MultisigAddress multisigAddressFromV8Object(V8Object input) {
        Map<String, ? super Object> multiSigMap = V8ObjectUtils.toMap(input);
        return multisigAddressFromPropertyMap(multiSigMap);
    }

    public static MultisigAddress multisigAddressFromPropertyMap(Map<String, Object> propMap) {
        // Parse result into Java Obj.
        String addr = (String) propMap.get("address");
        int secSum = (Integer) propMap.get("securitySum");


        ArrayList<MultisigAddress> children = new ArrayList<>();

        for (Object child: (ArrayList<Object>) propMap.get("children")) {
            Map<String, ? super Object> childPropMap = (Map<String, ? super Object>) child;
            children.add(multisigAddressFromPropertyMap(childPropMap));
        }

        MultisigAddress multisig = new MultisigAddress(addr, secSum, children);

        if (propMap.get("index") != null) {
            multisig.setIndex((Integer) propMap.get("index"));
        }
        if (propMap.get("signingIndex") != null) {
            multisig.setSigningIndex((Integer) propMap.get("signingIndex"));
        }

        return multisig;
    }

    public static ArrayList<Bundle> bundleListFromV8Array(V8Array input) {
        List<Object> inputList = V8ObjectUtils.toList(input);
        // Parse return as array of bundles
        ArrayList<Bundle> returnBundles = new ArrayList<>();
        for (Object bundleItem: inputList) {
            ArrayList<Object> bundleContent = (ArrayList<Object>) bundleItem;

            ArrayList<Transaction> returnedTransactions = new ArrayList<>();
            for (Object rawTransaction: bundleContent) {
                returnedTransactions.add(transactionFromObject(rawTransaction));
            }

            Bundle bundle = new Bundle(returnedTransactions);
            returnBundles.add(bundle);
        }
        return  returnBundles;
    }

    public static V8Array signatureListToV8Array(V8 engine, ArrayList<Signature> signatures) {
        List<Object> returnArr = new ArrayList<>();
        for (Signature sig: signatures) {
            Map<String, Object> signatureMap = new HashMap<String, Object>();
            signatureMap.put("bundle", sig.getBundle());
            signatureMap.put("index", sig.getIndex());
            signatureMap.put("address", sig.getAddress());
            signatureMap.put("signatureFragments", sig.getSignatureFragments());
            returnArr.add(signatureMap);
        }
        return V8ObjectUtils.toV8Array(engine, returnArr);
    }


    public static ArrayList<Transfer> transferListFromV8Array(V8Array input) {
        ArrayList<Transfer> transfers = new ArrayList<>();
        List<Object> jsTransferList = V8ObjectUtils.toList(input);
        if (jsTransferList != null) {
            for (Object obj: jsTransferList) {
                transfers.add(transferFromObject(obj));
            }
        }

        return transfers;
    }

    public static Transfer transferFromMap(Map<String, Object> values) {
        String obsoleteTag = (String) values.get("obsoleteTag");
        String address = (String) values.get("address");
        Long value = parseLongFromObject(values.get("value"));
        if (values.get("timestamp") instanceof String) {
            String timestamp = (String) values.get("timestamp");
            String hash = (String) values.get("hash");
            Boolean persistance = (Boolean) values.get("persistance");
            String message = (String) values.get("message");
            String tag = (String) values.get("tag");
            return new Transfer(
                    timestamp,
                    address,
                    hash,
                    persistance,
                    value,
                    message,
                    tag
            );
        } else {
            System.out.println("[WARN] Could not find key for full transfer creating slim transfer object");
            return new Transfer(address, value);
        }
    }

    public static Transfer transferFromObject(Object input) {
        if (input instanceof Map) {
            Map<String, ? super Object> values = (Map<String, ? super Object>) input;
            return transferFromMap(values);
        }
        return null;
    }

    public static V8Array transferListToV8Array(V8 engine, ArrayList<Transfer> transfers) {
        List<Object> transferObj = new ArrayList<Object>();
        for (Transfer t: transfers) {
            transferObj.add(t.toMap());
        }
        return V8ObjectUtils.toV8Array(engine, transferObj);
    }

    public static V8Array transactionListToV8Array(V8 engine, ArrayList<Transaction> transactions) {
        List<Object> transfersObj = new ArrayList<Object>();
        for (Transaction t: transactions) {
            transfersObj.add(t.toMap());
        }
        return V8ObjectUtils.toV8Array(engine, transfersObj);
    }


    public static Transaction transactionFromObject(Object input) {
        Map<String, Object> bundleData = (Map<String, Object>) input;
        String signatureMessageFragment = (String) bundleData.get("signatureMessageFragment");
        Long currentIndex = parseLongFromObject(bundleData.get("currentIndex"));
        Long lastIndex = parseLongFromObject(bundleData.get("lastIndex"));
        String nonce = (String) bundleData.get("nonce");
        String hash = "";
        if (bundleData.get("hash") instanceof String) {
            hash = (String) bundleData.get("hash");
        }
        String obsoleteTag = (String) bundleData.get("obsoleteTag");
        Long timestamp = parseLongFromObject(bundleData.get("timestamp"));
        String trunkTransaction = (String) bundleData.get("trunkTransaction");
        String branchTransaction = (String) bundleData.get("branchTransaction");
        String address = (String) bundleData.get("address");
        Long value = parseLongFromObject(bundleData.get("value"));
        String bundle = (String) bundleData.get("bundle");
        String tag = (String) bundleData.get("tag");
        Long attachmentTimestamp = parseLongFromObject(bundleData.get("attachmentTimestamp"));
        Long attachmentTimestampLowerBound = parseLongFromObject(bundleData.get("attachmentTimestampLowerBound"));
        Long attachmentTimestampUpperBound = parseLongFromObject(bundleData.get("attachmentTimestampUpperBound"));
        Transaction parsedTransaction = new Transaction(
                signatureMessageFragment,
                currentIndex,
                lastIndex,
                nonce,
                hash,
                obsoleteTag,
                timestamp,
                trunkTransaction,
                branchTransaction,
                address,
                value,
                bundle,
                tag,
                attachmentTimestamp,
                attachmentTimestampLowerBound,
                attachmentTimestampUpperBound
        );
        return parsedTransaction;
    }

    public static Long parseLongFromObject(Object value) {
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        if (value instanceof Integer) {
            return new Long((Integer) value);
        }

        return new Long(0);
    }
}
