package com.flashwifi.wifip2p.iotaFlashWrapper;

import android.util.Log;

import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Bundle;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.CreateTransactionHelperObject;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Digest;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.FlashObject;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Multisig;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Signature;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.Transfer;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.UserObject;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetInclusionStateResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.utils.Checksum;

/**
 * Created by wlad on 24.01.18.
 */

public class FlashChannelHelper {
    public static FlashChannelHelper getInstance() {
        if (instance == null) {
            instance = new FlashChannelHelper();
        }
        return instance;
    }
    private static FlashChannelHelper instance;


    private UserObject user;

    /**
     *  When ready change public to private and only use a singleton.
     */
    public FlashChannelHelper() {}

    /**
     *
     * @param userIndex
     * @param seed
     * @param seedIndex
     * @param security
     */
    public void setupUser(int userIndex, String seed, int seedIndex, int security) {
        this.user = new UserObject(userIndex, seed, seedIndex, security);
    }

    /**
     *
     * @param deposits
     * @param depth
     */
    public void setupFlash(double[] deposits, int depth) {
        if (user == null) {
            Log.d("[ERROR]", "setupUser must be called before calling setupFlash");
            return;
        }
        this.user.setFlash(new FlashObject(deposits, depth, user.getSecurity()));
    }

    /**
     * Create digests for the depth of the flash channel. Branches will be added when needed.
     * @return
     */
    public List<Digest> initialChannelDigests() {
        return Helpers.getDigestsForUser(this.user, this.user.getFlash().getDepth());
    }

    /**
     * Sets settlement addresses in the flash object, the addresses must be ordered according to the userIndex.
     * @param addresses
     */
    public void setupSettlementAddresses(ArrayList<String> addresses) {
        if (addresses.size() != user.getFlash().getSignersCount()) {
            Log.d("[ERROR]", "Not enough addresses found...");
            return;
        }
        user.getFlash().setSettlementAddresses(addresses);
    }

    /**
     * Combine all user digests pairs, create multisig addresses and setup tree structure.
     * @param digestPairs
     */
    public void setupChannelWithDigests(List<List<Digest>> digestPairs) {
        if (digestPairs.size() != user.getFlash().getSignersCount()) {
            Log.d("[ERROR]", " Not enough digest pairs found...");
            return;
        }

        // Create multisigs.
        List<Multisig> mulitisigs = Helpers.getMultisigsForUser(digestPairs, user);

        // Set renainder address.
        Multisig oneRemainderAddr = mulitisigs.remove(0); //shiftCopyArray();
        user.getFlash().setRemainderAddress(oneRemainderAddr);

        // Build flash trees
        for (int i = 1; i < mulitisigs.size(); i++) {
            // System.out.println(mulitisigs.get(i - 1).toString() + " -> "  + mulitisigs.get(i).toString());
            mulitisigs.get(i - 1).push(mulitisigs.get(i));
        }
        user.getFlash().setRoot(mulitisigs.remove(0));
    }

    public String getRootAddressWithChecksum() {
        try {
            return Checksum.addChecksum(user.getFlash().getRoot().getAddress());
        } catch (Exception e) {
            Log.d("[ERROR]", "Failed to get root address");
            return "";
        }
    }

    public String getRootAddress() {
        return user.getFlash().getRoot().getAddress();
    }


    /**
     * Transfer utils
     */

    /**
     * Create new multisig and insert it into the flash channel tree of the user.
     * @param digestPairs
     * @param address address to which the new address will be attached.
     */
    public Multisig updateTreeWithDigests(List<List<Digest>> digestPairs, Multisig address) {
        Multisig multisig = Helpers.getNewBranch(digestPairs, user, address);

        // Find the multisig with the address and append new address to children
        return Helpers.updateMultisigChildrenForUser(user, multisig);
    }

    public Multisig getMultisigByAddress(String address) {
        return user.getFlash().getRoot().find(address);
    }

    public ArrayList<Signature> createSignaturesForBundles(ArrayList<Bundle> bundles) {
        return IotaFlashBridge.sign(user.getFlash().getRoot(), user.getSeed(), bundles);
    }

    public void applyTransfers(ArrayList<Bundle> signedBundles) {
        Helpers.applyTransfers(signedBundles, user);
    }

    /**
     * Returns a helper with the address to use and the number of new addresses to generate.
     * Please create the new addresses before making the transaction.
     * @return
     */
    public CreateTransactionHelperObject getTransactionHelper() {
        return Helpers.getTransactionHelper(user.getFlash().getRoot());
    }

    public SendTransferResponse sendFundingTransfer(List<Input> inputs, long amount, String remainder, int security, int depth, int mwm) throws ArgumentException {
        List<jota.model.Transfer> fundingTransfers = new ArrayList<>();
        fundingTransfers.add((jota.model.Transfer) new Transfer(user.getFlash().getRoot().getAddress(), 100));
        IotaAPI api = Helpers.getIotaAPI();
        return api.sendTransfer(user.getSeed(), user.getSecurity(), depth, mwm, fundingTransfers, inputs, remainder, true);
    }

    /**
     *  Closing utils
     */

    public ArrayList<Bundle> createCloseTransactions() {
        return Helpers.closeChannel(user);
    }



    /**
     *  General utils
     */

    public UserObject getUser() {
        return user;
    }

    public int totalUsersCount() {
        return user.getFlash().getSignersCount();
    }

    public String getSettlementAddress() {
        return user.getFlash().getSettlementAddresses().get(user.getUserIndex());
    }

    public double getOutput() {
        return Helpers.getBalanceOfUser(user);
    }

    public static int getRequiredDepth(int transactionCount) {
        return (int) Math.ceil(Math.log(transactionCount)/Math.log(2));
    }

    public GetNewAddressResponse getNewAddresses(int number) throws ArgumentException {
        IotaAPI api = Helpers.getIotaAPI();
        GetNewAddressResponse resp = api.getNewAddress(user.getSeed(), user.getSecurity(), user.getSeedIndex(), false, number, true);
        user.setSeedIndex(user.getSeedIndex() + number);
        return resp;
    }

    public static void waitForTransfersToComplete(SendTransferResponse resp) throws ArgumentException {
        // Create array with all hashes.
        String[] transactionHashes = new String[resp.getTransactions().size()];
        for (int i = 0; i < resp.getTransactions().size(); i++) {
            transactionHashes[i] = resp.getTransactions().get(i).getHash();
        }

        IotaAPI api = Helpers.getIotaAPI();
        boolean attached = false;
        int time = 0;
        while (true) {
            if (time >= 30) {
                Log.e("[ERROR]", "no attachment after 30 seconds...");
                return;
            }

            GetInclusionStateResponse inclusionResp = api.getLatestInclusion(transactionHashes);

            boolean completed = true;
            for (boolean status : inclusionResp.getStates()) {
                completed = completed && status;
            }

            if (completed) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d("[WARN]", "Thread does not want to sleep " + e.getLocalizedMessage());
            }

            time++;
        }
    }
}
