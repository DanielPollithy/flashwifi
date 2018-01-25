package com.flashwifi.wifip2p.iotaFlashWrapper;

import android.util.Log;

import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Bundle;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.CreateTransactionHelperObject;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Digest;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Multisig;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Signature;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Transfer;

import java.util.ArrayList;
import java.util.List;

import jota.model.Transaction;

/**
 * Created by wlad on 24.01.18.
 */

public class Example {
    public static FlashChannelHelper one;
    public static FlashChannelHelper two;

    public static void setup() {

        double[] deposits = new double[]{100,100};
        int depth = 4;


        // Example of a user setup.
        one = FlashChannelHelper.getInstance();
        one.setupUser(0, "RDNUSLPNOQUGDIZVOINTYRIRRIJMLODOC9ZTQU9KQSCDXPVSBILXUE9AHEOA9MNYZWNSECAVPQ9QSAHCN", 0, 1);
        one.setupFlash(deposits, depth);

        // Create a second user
        // Here i needed a second instance so I left the public in the constructor of FlashChannelHelper
        two = new FlashChannelHelper();
        two.setupUser(1, "IUQDBHFDXK9EHKC9VUHCUXDLICLRANNDHYRMDYFCGSZMROWCZBLBNRKXWBSWZYDMLLHIHMP9ZPOPIFUSW", 0, 1);
        two.setupFlash(deposits, depth);

        // The addresses must be exchanged over the network.
        // When done setup the settlementAddresses.
        ArrayList<String> settlementAddresses = new ArrayList<>();
        settlementAddresses.add("RDNUSLPNOQUGDIZVOINTYRIRRIJMLODOC9ZTQU9KQSCDXPVSBILXUE9AHEOA9MNYZWNSECAVPQ9QSAHCN");
        settlementAddresses.add("IUQDBHFDXK9EHKC9VUHCUXDLICLRANNDHYRMDYFCGSZMROWCZBLBNRKXWBSWZYDMLLHIHMP9ZPOPIFUSW");

        // Set the addresses
        one.setupSettlementAddresses(settlementAddresses);
        two.setupSettlementAddresses(settlementAddresses);

        // Now generate the digests for each user.
        List<Digest> initialDigestsOne = one.initialChannelDigests();
        List<Digest> initialDigestsTwo = two.initialChannelDigests();

        // Now the users should exchange the digests over the network
        // When the both have the digests create a array with them. make sure the order is kept.
        List<List<Digest>> digestPairs = new ArrayList<>();
        digestPairs.add(initialDigestsOne);
        digestPairs.add(initialDigestsTwo);

        // This will create the initial multisig addresses and the root address.
        one.setupChannelWithDigests(digestPairs);
        two.setupChannelWithDigests(digestPairs);

        // After this the root must be set. You can check for it using this call.
        Log.d("[ROOT ADDR]", one.getRootAddressWithChecksum());


        // Now the setup is ready
        Log.d("[SUCCESS]", "demo setup completed");
    }

    public static void transaction(FlashChannelHelper sender, FlashChannelHelper reciever) {
        // Check if we need to expand the tree
        CreateTransactionHelperObject helper = sender.getTransactionHelper();

        // If the generate value is larget 0 we need to generate some new addresses
        if (helper.getGenerate() > 0) {
            // Get digests from the sender
            ArrayList<Digest> senderDigests = Helpers.getNewBranchDigests(sender.getUser(), helper.getGenerate());

            // Send senderDigests to user two and wait for his digests.

            // User two makes digests.
            ArrayList<Digest> recieverDigests = Helpers.getNewBranchDigests(reciever.getUser(), helper.getGenerate());

            // When you have both you can create a digest array
            List<List<Digest>> digestPairs = new ArrayList<>();
            digestPairs.add(senderDigests);
            digestPairs.add(recieverDigests);

            // Now each party must generate the digests.
            Multisig updatedAddress = sender.updateTreeWithDigests(digestPairs, helper.getAddress());

            // Since the other user does not have the address to use (the object reference). We search for it.
            Multisig extansionMultisigTwo =  reciever.getMultisigByAddress(helper.getAddress().getAddress());
            if (extansionMultisigTwo == null) {
                Log.d("[ERROR]", "Could not find attachment address for tree expansion.");
            }

            reciever.updateTreeWithDigests(digestPairs, extansionMultisigTwo);

            // Now the sender just has to update the helper objects multisig address. I will optimize this in the future.
            helper.setAddress(updatedAddress);
        }

        // Now you can perform a transaction

        // Create transfers.
        ArrayList<Transfer> transfers = new ArrayList<>();
        transfers.add(new Transfer(reciever.getSettlementAddress(), 10));

        // Create a transaction from a transfer.
        ArrayList<Bundle> senderBundles = Helpers.createTransaction(transfers, helper, sender.getUser());

        // Now send the senderBundles to the receiver and wait for the response signatures.

        // USER2: check if bundle content is okay. Only the first transaction of the last bundle is interesting.
        // But if we want to be sure we need to check both bundles and all transactions.

        // User two generates signatures if all is okay.
        ArrayList<Signature> receiverSignatures = reciever.createSignaturesForBundles(senderBundles);

        // Now both just have to apply the signatures and check if all is good.
        ArrayList<Bundle> senderSignedBundles = IotaFlashBridge.appliedSignatures(senderBundles, receiverSignatures);
        ArrayList<Bundle> receiverSignedBundles = IotaFlashBridge.appliedSignatures(senderBundles, receiverSignatures);

        // Now if all is good just apply. In future I will throw a error on invalid signing.
        sender.applyTransfers(senderSignedBundles);
        reciever.applyTransfers(receiverSignedBundles);

        Log.d("[SUCCESS]", "demo transaction completed");
    }

    /**
     *
     * @param sender
     * @param reciever
     */
    public static void close(FlashChannelHelper sender, FlashChannelHelper reciever) {
        // Close is more or a less just a transaction.
        // So the start is exactly the same.

        // Check if we need to expand the tree
        CreateTransactionHelperObject helper = sender.getTransactionHelper();

        // If the generate value is larget 0 we need to generate some new addresses
        if (helper.getGenerate() > 0) {
            // Get digests from the sender
            ArrayList<Digest> senderDigests = Helpers.getNewBranchDigests(sender.getUser(), helper.getGenerate());

            // Send senderDigests to user two and wait for his digests.

            // User two makes digests.
            ArrayList<Digest> recieverDigests = Helpers.getNewBranchDigests(reciever.getUser(), helper.getGenerate());

            // When you have both you can create a digest array
            List<List<Digest>> digestPairs = new ArrayList<>();
            digestPairs.add(senderDigests);
            digestPairs.add(recieverDigests);

            // Now each party must generate the digests.
            Multisig updatedAddress = sender.updateTreeWithDigests(digestPairs, helper.getAddress());

            // Since the other user does not have the address to use (the object reference). We search for it.
            Multisig extansionMultisigTwo =  reciever.getMultisigByAddress(helper.getAddress().getAddress());
            if (extansionMultisigTwo == null) {
                Log.d("[ERROR]", "Could not find attachment address for tree expansion.");
            }

            reciever.updateTreeWithDigests(digestPairs, extansionMultisigTwo);

            // Now the sender just has to update the helper objects multisig address. I will optimize this in the future.
            helper.setAddress(updatedAddress);
        }

        // Now we create is closing transaction.
        ArrayList<Bundle> senderBundles = sender.createCloseTransactions();

        // Here the same game as before with the signatures...


        // Now send the senderBundles to the receiver and wait for the response signatures.

        // USER2: check if bundle content is okay. Only the first transaction of the last bundle is interesting.
        // But if we want to be sure we need to check both bundles and all transactions.

        // User two generates signatures if all is okay.
        ArrayList<Signature> receiverSignatures = reciever.createSignaturesForBundles(senderBundles);

        // Now both just have to apply the signatures and check if all is good.
        ArrayList<Bundle> senderSignedBundles = IotaFlashBridge.appliedSignatures(senderBundles, receiverSignatures);
        ArrayList<Bundle> receiverSignedBundles = IotaFlashBridge.appliedSignatures(senderBundles, receiverSignatures);

        // Now if all is good just apply. In future I will throw a error on invalid signing.
        sender.applyTransfers(senderSignedBundles);
        reciever.applyTransfers(receiverSignedBundles);

        // Check if bundle is valid before attaching.
        if (senderBundles.size() <= 0) {
            Log.e("[ERROR]", "Invalid closing bundle length");
            return;
        }

        // Check if bundle has correct transactions count.
        if (senderBundles.get(0).getTransactions().size() != 4) {
            Log.e("[ERROR]", "Invalid closing transactions count should be 4 is" + senderBundles.get(0).getTransactions().size());
        }

        // Check if transactions are going to correct addresses
        Transaction userOneTransaction = senderBundles.get(0).getTransactions().get(sender.getUser().getUserIndex());
        if (!userOneTransaction.getAddress().equals(sender.getSettlementAddress())) {
            Log.e("[ERROR]", "Invalid closing bundle user one output address");
            return;
        }

        if (userOneTransaction.getValue() != sender.getOutput()) {
            Log.e("[ERROR]", "Invalid output amount for user one");
            return;
        }

        // Check user two outputs
        Transaction userTwoTransaction = senderBundles.get(0).getTransactions().get(reciever.getUser().getUserIndex());
        if (!userTwoTransaction.getAddress().equals(reciever.getSettlementAddress())) {
            Log.e("[ERROR]", "Invalid closing bundle user one output address");
            return;
        }

        if (userTwoTransaction.getValue() != reciever.getOutput()) {
            Log.e("[ERROR]", "Invalid output amount for user one");
            return;
        }

        // Check if the origin is correct.
        Transaction rootMultisigTransaction = senderBundles.get(0).getTransactions().get(reciever.totalUsersCount());
        if (!rootMultisigTransaction.getAddress().equals(sender.getRootAddress())) {
            Log.e("[ERROR]", "Invalid flash channel root in output");
            return;
        }

        // Check if money adds up.
        if (rootMultisigTransaction.getValue() != -reciever.getUser().getFlash().getBalance()) {
            Log.e("[ERROR]", "Invalid flash channel root in output amount. Is" + rootMultisigTransaction.getValue() + " should be " + -reciever.getUser().getFlash().getBalance());
            return;
        }

        Log.d("[SUCCESS]", "Output bundles passed all tests, ready to attach");

        // And here we attach the bundle to the tangle. We can check the response bundles since we may need to rebroadcast them.
        // As far as I understand only one needs to send them.
        List<Bundle> attachedBundles = Helpers.POWClosedBundle(senderSignedBundles, 4, 13);

        if (attachedBundles.size() != senderBundles.size() && attachedBundles.size() > 0) {
            Log.d("[SUCCESS]", "demo channel closed. Attached hash " + attachedBundles.get(0).getTransactions().get(0).getHash());
        } else {
            Log.d("[ERROR]", "attachment failed. Nothing attached. ");
        }
    }
}
