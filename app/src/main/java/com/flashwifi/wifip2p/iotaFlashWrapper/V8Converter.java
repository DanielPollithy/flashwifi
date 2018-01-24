package com.flashwifi.wifip2p.iotaFlashWrapper;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;
import jota.model.Transaction;

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

    public static V8Object multisigToV8Object(V8 engine, Multisig multisig) {
        Map<String, Object> sigMapg = multisig.toMap();
        return V8ObjectUtils.toV8Object(engine, sigMapg);
    }

    public static V8Object flashObjectToV8Object(V8 engine, FlashObject flash) {
        Map <String, Object> flashMap = flash.toMap();
        return V8ObjectUtils.toV8Object(engine, flashMap);
    }

    public static FlashObject flashObjectFromV8Object(V8Object input) {
        Map<String, Object> inputMap = V8ObjectUtils.toMap(input);

        Integer singersCount = (Integer) inputMap.get("signersCount");
        Integer balance = (Integer) inputMap.get("balance");
        ArrayList<String> settlementAddresses = (ArrayList<String>) inputMap.get("settlementAddresses");
        Multisig root = multisigAddressFromPropertyMap((Map<String, Object>) inputMap.get("root"));
        Multisig remainderAddress = multisigAddressFromPropertyMap((Map<String, Object>) inputMap.get("remainderAddress"));
        ArrayList<Double> deposits = new ArrayList<>();
        if (inputMap.get("deposits") instanceof ArrayList) {
            Object depositEntry = inputMap.get("deposits");
            if (((ArrayList<Object>) depositEntry).size() > 0 && ((ArrayList<Object>) depositEntry).get(0) instanceof Integer) {
                for (int val: (ArrayList<Integer>) depositEntry) {
                    deposits.add(new Double(val));
                }
            } else {
                deposits = (ArrayList<Double>) depositEntry;
            }
        }
        ArrayList<Bundle> transfers = bundleListFromArrayList((ArrayList<Object>) inputMap.get("transfers"));
        Map<String, Integer> outputs = (Map<String, Integer>) inputMap.get("outputs");
        Integer depth = (Integer) inputMap.get("depth");
        Integer security = (Integer) inputMap.get("security");
        return new FlashObject(singersCount, balance, settlementAddresses, deposits, outputs, transfers, root, remainderAddress, depth ,security);
    }

    public static V8Array bundleListToV8Array(V8 engine, List<Bundle> bundles) {

        List<Object> bundleTmp = new ArrayList<Object>();
        for (Bundle b: bundles) {
            List<Object> transactions = new ArrayList<Object>();
            for (jota.model.Transaction tx: b.getTransactions()) {
                transactions.add(transactionToMap((Transaction) tx));
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
            if (o instanceof Map) {
                if (((Map) o).get("bundles") instanceof String) {
                    ArrayList<Object> bundles = (ArrayList<Object>) ((Map<String, Object>) o).get("bundles");
                    ret.add(bundleFromArrayList(bundles));
                } else {
                    continue;
                }
            }
            if (o instanceof ArrayList) {
                ret.add(bundleFromArrayList((ArrayList<Object>) o));
            }
        }


        return ret;
    }

    public static Multisig multisigAddressFromV8Object(V8Object input) {
        if (input.isUndefined()) {
            System.out.println("[ERROR]: could not parse object");
            return null;
        }
        Map<String, ? super Object> multiSigMap = V8ObjectUtils.toMap(input);
        return multisigAddressFromPropertyMap(multiSigMap);
    }

    public static Multisig multisigAddressFromPropertyMap(Map<String, Object> propMap) {
        // Parse result into Java Obj.
        String addr = (String) propMap.get("address");
        int secSum = (Integer) propMap.get("securitySum");

        ArrayList<Multisig> children = new ArrayList<>();

        for (Object child: (ArrayList<Object>) propMap.get("children")) {
            Map<String, ? super Object> childPropMap = (Map<String, ? super Object>) child;
            children.add(multisigAddressFromPropertyMap(childPropMap));
        }

        Multisig multisig = new Multisig(addr, secSum, children);

        if (propMap.get("bundles") instanceof ArrayList) {
            ArrayList<Bundle> bundles = new ArrayList<>();
            for (Object bundle: (ArrayList<Object>) propMap.get("bundles")) {
                Bundle b = new Bundle();
                if (!(bundle instanceof  ArrayList)) {
                    continue;
                } else {
                    for (Object transactionMap: (ArrayList<Object>) bundle) {
                        b.getTransactions().add(transactionFromObject(transactionMap));
                    }
                }
                bundles.add(b);
            }

            multisig.setBundles(bundles);
        }

        if (propMap.get("index") != null) {
            multisig.setIndex((Integer) propMap.get("index"));
        }
        if (propMap.get("signingIndex") != null) {
            multisig.setSigningIndex((Integer) propMap.get("signingIndex"));
        }

        if (propMap.get("security") instanceof Integer) {
            multisig.setSecurity((Integer) propMap.get("security"));
        }

        return multisig;
    }

    public static ArrayList<Bundle> bundleListFromV8Array(V8Array input) {
        List<Object> inputList = V8ObjectUtils.toList(input);
        // Parse return as array of bundles
        ArrayList<Bundle> returnBundles = new ArrayList<>();
        for (Object bundleItem: inputList) {
            if (!(bundleItem instanceof  ArrayList)) {
                System.out.println("[ERROR]: got undefined for bunle");
                continue;
            }
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

    public static V8Array transferListToV8Array(V8 engine, List<Transfer> transfers) {
        List<Object> transferObj = new ArrayList<Object>();
        for (Transfer t: transfers) {
            transferObj.add(t.toMap());
        }
        return V8ObjectUtils.toV8Array(engine, transferObj);
    }

    public static V8Array transactionListToV8Array(V8 engine, ArrayList<Transaction> transactions) {
        List<Object> transfersObj = new ArrayList<Object>();
        for (Transaction tx: transactions) {
            transfersObj.add(transactionToMap(tx));
        }
        return V8ObjectUtils.toV8Array(engine, transfersObj);
    }

    public static Map<String, Object> transactionToMap(jota.model.Transaction transaction) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (transaction.getHash() != null && !transaction.getHash().equals("")) {
            map.put("hash", transaction.getHash());
        }
        map.put("signatureMessageFragment", transaction.getSignatureFragments());
        map.put("address", transaction.getAddress());
        map.put("value", transaction.getValue());
        map.put("obsoleteTag", transaction.getObsoleteTag());
        map.put("currentIndex", transaction.getCurrentIndex());
        map.put("timestamp", transaction.getTimestamp());
        map.put("lastIndex", transaction.getLastIndex());
        map.put("bundle", transaction.getBundle());
        map.put("trunkTransaction", transaction.getTrunkTransaction());
        map.put("branchTransaction", transaction.getBranchTransaction());
        map.put("nonce", transaction.getNonce());
        map.put("attachmentTimestamp", String.valueOf(transaction.getAttachmentTimestamp()));
        map.put("tag", transaction.getTag());
        map.put("attachmentTimestampLowerBound", String.valueOf(transaction.getAttachmentTimestampLowerBound()));
        map.put("attachmentTimestampUpperBound", String.valueOf(transaction.getAttachmentTimestampUpperBound()));
        return map;
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
