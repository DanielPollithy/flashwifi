package com.flashwifi.wifip2p.iotaFlashWrapper;



import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;

import java.util.ArrayList;

public class Helpers {
    public static ArrayList<Bundle> createTransaction(UserObject user, ArrayList<Transfer> transfers, boolean shouldClose) {
        CreateTransactionHelperObject toUse = IotaFlashBridge.updateLeafToRoot(user.getFlash().getRoot());
        if (toUse.getGenerate() != 0) {
            // TODO: tell the server to gen new address.
            System.out.println("No more addresses in channel.");
        }

        ArrayList<Transfer> newTransfers;

        if (shouldClose) {
            newTransfers = IotaFlashBridge.close(user.getFlash().getSettlementAddresses(), user.getFlash().getDeposits());
        } else {
            newTransfers = IotaFlashBridge.prepare(
                    user.getFlash().getSettlementAddresses(),
                    user.getFlash().getDeposits(),
                    user.getUserIndex(),
                    transfers
                    );

        }

        ArrayList<Bundle> bundles = IotaFlashBridge.compose(
                user.getFlash().getBalance(),
                user.getFlash().getDeposits(),
                user.getFlash().getOutputs(),
                toUse.getAddress(),
                user.getFlash().getRemainderAddress(),
                user.getFlash().getTransfers(),
                newTransfers,
                shouldClose
        );
        return bundles;
    }

    public static ArrayList<Signature> signTransaction(UserObject user, ArrayList<Bundle> bundles) {
        return IotaFlashBridge.sign(user.getFlash().getRoot(), user.getSeed(), bundles);
    }
//
//    public static ArrayList<Bundle> appliedSignatures(ArrayList<Bundle> bundles, ArrayList<Signature> signatures) {
//        ArrayList<Bundle> clonedBundles = clone(bundles);
//        bundles.clone();
//
//        for (int i = 0; i < bundles.size(); i++) {
//            Signature sig = signatures.get(i);
//            Bundle b = bundles.get(i);
//            if (sig == null) {
//                continue;
//            }
//
//            ArrayList<Transaction> transactions = b.getBundles();
//            String addy = transactions.stream().filter(tx -> tx.getValue() < 0).findFirst().get().getAddress();
//            List<Transaction> tmp = transactions.stream()
//                    .filter(tx -> tx.getAddress().equals(addy))
//                    .collect(Collectors.toList());
//
//            tmp = tmp.subList(sig.getIndex(), sig.getIndex() + sig.getSignatureFragments().size());
//
//            for (int j = 0; j < tmp.size(); j++) {
//                tmp.get(j).setSignatureFragments(sig.getSignatureFragments().get(j));
//            }
//        }
//
//        return clonedBundles;
//    }

    public static void applyTransfers(UserObject user, ArrayList<Bundle> bundles) {
        FlashObject flash = IotaFlashBridge.applyTransfersToUser(user, bundles);
        user.setFlash(flash);
        //        IotaFlashBridge.applyTransfers(
//                user.getFlash().getRoot(),
//                user.getFlash().getDeposits(),
//                user.getFlash().getOutputs(),
//                user.getFlash().getRemainderAddress(),
//                user.getFlash().getTransfers(),
//                bundles
//        );
    }

    public static ArrayList<Bundle> clone(ArrayList<Bundle> bundles) {
        ArrayList<Bundle> clonedBundles = new ArrayList<>();
        for (Bundle b : bundles) {
            clonedBundles.add(b.clone());
        }
        return clonedBundles;
    }
}
