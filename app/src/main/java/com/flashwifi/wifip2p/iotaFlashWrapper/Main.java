package com.flashwifi.wifip2p.iotaFlashWrapper;

import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Main {

   public static void runExample() throws Exception {

        // Run a test based on the flash example
        // Link: https://github.com/iotaledger/iota.flash.js/blob/master/examples/flash.js

        String oneSeed = "USERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSER";
        String oneSettlement = "USERONE9ADDRESS9USERONE9ADDRESS9USERONE9ADDRESS9USERONE9ADDRESS9USERONE9ADDRESS9U";
        String twoSeed = "USERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSERTWOUSER";
        String twoSettlement = "USERTWO9ADDRESS9USERTWO9ADDRESS9USERTWO9ADDRESS9USERTWO9ADDRESS9USERTWO9ADDRESS9U";

        //////////////////////////////////
        // INITIAL CHANNEL CONDITIONS

        // Security level
        int SECURITY = 2;
        // Number of parties taking signing part in the channel
        int SIGNERS_COUNT = 2;
        // Flash tree depth
        int TREE_DEPTH = 4;
        // Total channel Balance
        int CHANNEL_BALANCE = 2000;
        // Users deposits
        ArrayList<Integer> DEPOSITS = new ArrayList<>();
        DEPOSITS.add(1000);
        DEPOSITS.add(1000);
        // Setup users.
        FlashObject oneFlashObj = new FlashObject(SIGNERS_COUNT, CHANNEL_BALANCE, DEPOSITS);
        UserObject oneFlash = new UserObject(0, oneSeed, TREE_DEPTH, oneFlashObj);

        FlashObject twoFlashObj = new FlashObject(SIGNERS_COUNT, CHANNEL_BALANCE, DEPOSITS);
        UserObject twoFlash = new UserObject(1, twoSeed, TREE_DEPTH, twoFlashObj);

        // USER ONE
        setupUser(oneFlash, TREE_DEPTH);

        // USER TWO
        setupUser(twoFlash, TREE_DEPTH);

        //////////////////////////////////
        // INITAL MULTISIG

        // Make an array of digests
        ArrayList<UserObject> allUsers = new ArrayList<UserObject>();
        allUsers.add(oneFlash);
        allUsers.add(twoFlash);

        // Create partial digests for users.
        createInitialPartialDigests(allUsers, oneFlash);
        createInitialPartialDigests(allUsers, twoFlash);

        ArrayList<MultisigAddress> oneMultisigs = oneFlash.getMultisigDigests();
        ArrayList<MultisigAddress> twoMultisigs = twoFlash.getMultisigDigests();

        // Set renainder address.
        MultisigAddress oneRemainderAddr = oneMultisigs.remove(0); //shiftCopyArray();
        oneFlash.getFlash().setRemainderAddress(oneRemainderAddr);

        MultisigAddress twoRemainderAddr = twoMultisigs.remove(0);
        twoFlash.getFlash().setRemainderAddress(twoRemainderAddr);

        // Build flash trees
        for (int i = 1; i < oneMultisigs.size(); i++) {
            System.out.println(oneMultisigs.get(i - 1).toString() + " -> "  + oneMultisigs.get(i).toString());
            oneMultisigs.get(i - 1).push(oneMultisigs.get(i));
        }

        // Build flash trees
        for (int i = 1; i < twoMultisigs.size(); i++) {
            twoMultisigs.get(i - 1).push(twoMultisigs.get(i));
        }

        oneFlash.getFlash().setRoot(oneMultisigs.remove(0));
        twoFlash.getFlash().setRoot(twoMultisigs.remove(0));

        ArrayList<String> settlementAddresses = new ArrayList<>();
        settlementAddresses.add(oneSettlement);
        settlementAddresses.add(twoSettlement);
        oneFlash.getFlash().setSettlementAddresses(settlementAddresses);
        twoFlash.getFlash().setSettlementAddresses(settlementAddresses);

        // Set digest/key index
        oneFlash.setIndex(oneFlash.getPartialDigests().size());
        twoFlash.setIndex(twoFlash.getPartialDigests().size());

        System.out.println("Channel Setup!");

        ArrayList<Transfer> transfers = new ArrayList<>();
        transfers.add(new Transfer(twoSettlement, 1));
        transfers.add(new Transfer(twoSettlement, 400));

        System.out.println(oneFlash);

        System.out.println("Creating a transaction: 200 to " + twoSettlement);
        ArrayList<Bundle> bundles = Helpers.createTransaction(oneFlash, transfers, false);

        ArrayList<Bundle> partialSignedBundles = signTransfer(bundles, oneFlash);
        ArrayList<Bundle> signedBundles = signTransfer(partialSignedBundles, twoFlash);
        /////////////////////////////////
        /// APPLY SIGNED BUNDLES

        // Apply transfers to User ONE
        Helpers.applyTransfers(oneFlash, signedBundles);

        // Save latest channel bundles
        oneFlash.setBundles(signedBundles);

        // Apply transfers to User TWO
        Helpers.applyTransfers(twoFlash, signedBundles);
        // Save latest channel bundles
        twoFlash.setBundles(signedBundles);
        System.out.println("[SUCCESS] Apply Transfer to flash channel.");


        System.out.println("Transaction Applied!");
//        System.out.println(
//                "Transactable tokens: " +
//                oneFlash.getFlash().getDeposits().stream().mapToInt(v -> v.intValue()).sum()
//        );

        System.out.println("Closing channel... not yet working...");
    }

    private static ArrayList<Bundle> signTransfer(ArrayList<Bundle> bundles, UserObject user) {
        System.out.println("[SUCCESS] Created signatures for users.");
        ArrayList<Signature> oneSignatures = Helpers.signTransaction(user, bundles);

        System.out.println("[SUCCESS] Parial applied Signature for User one on transfer bundle");
        // Sign bundle with your USER ONE'S signatures
        ArrayList<Bundle> signedBundles = IotaFlashBridge.appliedSignatures(bundles, oneSignatures);

        return signedBundles;
    }

    private static void setupUser(UserObject user, int TREE_DEPTH) {
        // Create digests for the start of the channel
        for (int i = 0; i < TREE_DEPTH + 1; i++) {
            // Create new digest
            Digest digest = IotaFlashBridge.getDigest(
                    user.getSeed(),
                    user.getIndex(),
                    user.getSecurity()
            );
            System.out.println("Adding digest (" + digest.toString() + ") to user " + user.getUserIndex());
            // Increment key index
            user.incrementIndex();
            user.add(digest);
        }
    }

    private static void createInitialPartialDigests(ArrayList<UserObject> allUsers, UserObject currentUser) {

        // Generate the first addresses
        ArrayList<MultisigAddress> oneMultisigs = new ArrayList<MultisigAddress>();


        System.out.println("_________________________________________________________________");
        System.out.println("Creating multisigs on user: " + currentUser.getUserIndex());
        int index = 0;
        // Create address
        for (Digest digest: allUsers.get(index).getPartialDigests()) {
            int i = index;

            ArrayList<Digest> currentDigests = new ArrayList<>();
            int securitySum = 0;
            for (int j = 0; j < allUsers.size(); j++)  {
                Digest currentDigest = allUsers.get(j).getPartialDigests().get(i);
                currentDigests.add(currentDigest);
                securitySum += currentDigest.getSecurity();
            }

            MultisigAddress addy = IotaFlashBridge.composeAddress(currentDigests);

            System.out.println("Multisig: " + addy.toString());

            // Add key index in
            addy.setIndex(digest.getIndex());
            // Add the signing index to the object IMPORTANT
            addy.setSigningIndex(currentUser.getUserIndex() * digest.getSecurity());
            // Get the sum of all digest security to get address security sum
            addy.setSecuritySum(securitySum);
            addy.setSecurity(digest.getSecurity());
            oneMultisigs.add(addy);
            index++;
        }
        currentUser.setMultisigDigests(oneMultisigs);
    }

    private static ArrayList<MultisigAddress> shiftCopyArray(ArrayList<MultisigAddress> input) {
        ArrayList<MultisigAddress> output = new ArrayList<>();

        for (int i = 1; i < input.size(); i++) {
            output.add(input.get(i));
        }

        return output;
    }

    private static void test() throws IOException {

        System.out.println("IOTA Flash channel tester");

        String pathToLib = "res/iotaflash.js";

        System.out.println("Loading lib into V8 engine");
        System.out.println("Lib imported");


        System.out.println("Testing getDigest(seed, index, security):");
        Digest digest1 = IotaFlashBridge.getDigest("USERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSER", 0, 2);
        Digest digest2 = IotaFlashBridge.getDigest("USERTWOUSERTWOUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSERONEUSER", 0, 2);
        System.out.println("Digest1: " + digest1.toString());


        System.out.println("Testing composeAddress(digests):");
        ArrayList<Digest> digests = new ArrayList<Digest>();
        digests.add(digest1);
        digests.add(digest2);
        MultisigAddress composedAddr = IotaFlashBridge.composeAddress(digests);
        System.out.println("Got multisig addr for digests: " + composedAddr.getAddress() + ", securitySum: " + composedAddr.getSecuritySum());

        testPrepare();
    }

    private static void testPrepare() {

        System.out.println("Testing prepare()");
        ArrayList<String> settlementAddr = new ArrayList<String>();
        settlementAddr.add("RCZHCRDWMGJPHKROKEGVADVJXPGKEKNJRNLZZFPITUVEWNPGIWNUMKTYKMNB9DCNLWGMJZDNKYQDQKDLC");
        ArrayList<Integer> depositsPrep = new ArrayList<Integer>();
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();

        IotaFlashBridge.prepare(settlementAddr, depositsPrep, 0, transfers);
    }
}
