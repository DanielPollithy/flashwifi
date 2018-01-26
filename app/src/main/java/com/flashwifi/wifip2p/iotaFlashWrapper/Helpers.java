package com.flashwifi.wifip2p.iotaFlashWrapper;

import com.google.gson.Gson;
import com.flashwifi.wifip2p.iotaFlashWrapper.Model.*;
import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.model.Transaction;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helpers {
    private static boolean useTestnet = true;
    private static String seedGeneratorURL = "http://87.118.96.200:3000"; //"https://seeedy.tangle.works";
    private static String testNetNodeURL = "https://testnet140.tangle.works:443";
    private static String netNodeURL = "http://node.iotawallet.info:14265"; // "http://87.118.96.200:14700";//
    private static IotaAPI iotaAPI = null;

    /**
     * Get a transaction object. The object contains the address to use and if required the number of new addresses to generate
     * @param root multisig address at the top of the tree
     * @return Transaction object with address and number of addresses to create.
            */
    public static CreateTransactionHelperObject getTransactionHelper(Multisig root) {
        return IotaFlashBridge.updateLeafToRoot(root);
    }


    /**
     *
     * @param transfers
     * @param toUse Transaction helper object
     * @param user
     * @return
     */
    public static ArrayList<Bundle> createTransaction(ArrayList<Transfer> transfers, CreateTransactionHelperObject toUse, UserObject user) {
        // System.out.println("Creating a transaction of" + transfers.getValue() + " to " + transfers.getAddress());
        System.out.println("[INFO]: using address "  + toUse.getAddress().getAddress() + ", with boundle count " + toUse.getAddress().getBundles().size());


        FlashObject flash = user.getFlash();
        ArrayList<Bundle> bundles;
        // Prepare a new transaction.
        ArrayList<Transfer> newTransfers = IotaFlashBridge.prepare(
                flash.getSettlementAddresses(),
                flash.getDeposits(),
                user.getUserIndex(),
                transfers
        );

        if (newTransfers == null) {
            return new ArrayList<>();
        }

        // Compose the transaction. This may also add some management transactions (moving remainder tokens.)
        bundles = IotaFlashBridge.compose(
                flash.getBalance(),
                flash.getDeposits(),
                flash.getOutputs(),
                toUse.getAddress(),
                flash.getRemainderAddress(),
                flash.getTransfers(),
                newTransfers,
                false
        );

        System.out.println("[SUCCESS] Created signatures for user" + user.getUserIndex());
        // Apply the signature of the transaction creater to the current transactions bundle.
        ArrayList<Signature> signatures = IotaFlashBridge.sign(toUse.getAddress(), user.getSeed(), bundles);

        System.out.println("[SUCCESS] Parial applied Signature for user" +  user.getUserIndex() + " on transfer bundle");
        // Sign bundle with your USER ONE'S signatures
        return IotaFlashBridge.appliedSignatures(bundles, signatures);
    }

    public static ArrayList<Bundle> closeChannel(UserObject user) {
        FlashObject flash = user.getFlash();
        ArrayList<Transfer> closeTransfers = IotaFlashBridge.close(flash.getSettlementAddresses(), flash.getDeposits());
        // Compose the transaction. This may also add some management transactions (moving remainder tokens.)
        ArrayList<Bundle> bundles = IotaFlashBridge.compose(
                flash.getBalance(),
                flash.getDeposits(),
                flash.getOutputs(),
                flash.getRoot(),
                flash.getRemainderAddress(),
                flash.getTransfers(),
                closeTransfers,
                true
        );

        System.out.println("[SUCCESS] Created signatures for user" + user.getUserIndex());
        // Apply the signature of the transaction creater to the current transactions bundle.
        ArrayList<Signature> signatures = IotaFlashBridge.sign(flash.getRoot(), user.getSeed(), bundles);

        System.out.println("[SUCCESS] Parial applied Signature for user" +  user.getUserIndex() + " on transfer bundle");
        // Sign bundle with your USER ONE'S signatures
        return IotaFlashBridge.appliedSignatures(bundles, signatures);
    }

    /**
     *
     *  Tree management.
     *
     */

    /**
     *
     * @param user
     * @param toGenerate
     * @return
     */
    public static ArrayList<Digest> getNewBranchDigests(UserObject user, int toGenerate) {
        ArrayList<Digest> digests = new ArrayList<>();
        for (int i = 0; i < toGenerate; i++) {
            Digest digest = IotaFlashBridge.getDigest(user.getSeed(), user.getSeedIndex(), user.getSecurity());
            System.out.println("USING index for digest: " + user.getSeedIndex() );
            user.incrementSeedIndex();
            digests.add(digest);
        }
        return digests;
    }


    /**
     *
     * @param oneDigests
     * @param twoDigests
     * @param user
     * @param address
     * @return
     */
    public static Multisig getNewBranch(ArrayList<Digest> oneDigests, ArrayList<Digest> twoDigests, UserObject user, Multisig address) {
        List<List<Digest>> userDigestList = new ArrayList<>();
        userDigestList.add(oneDigests);
        userDigestList.add(twoDigests);
        return getNewBranch(userDigestList, user, address);
    }

    public static Multisig getNewBranch(List<List<Digest>> digestPairs, UserObject user, Multisig address) {
        List<Multisig> multisigs = getMultisigsForUser(digestPairs, user);

        System.out.println("[INFO]: Adding to address " + address.getAddress());

        // Build flash trees
        for (int i = 1; i < multisigs.size(); i++) {
            multisigs.get(i - 1).push(multisigs.get(i));
        }

        // Clone the address to avoid overwriting params.
        Multisig output = address.clone();

        // Add new multisigs to address.
        output.push(multisigs.get(0));

        return output;
    }


    /**
     *
     *  Digests and Multisig creation
     *
     */

    /**
     * Creates initial digests for a user. This will only create digests for a given TREE_DEPTH -> Digests.size == TREE_DEPTH + 1
     * Other transactions will be generated when required.
     * @param user user for which the digests should be generated
     * @param TREE_DEPTH number of initial digests to generate
     * @return digests for provided user.
     */
    public static ArrayList<Digest> getDigestsForUser(UserObject user, int TREE_DEPTH) {
        ArrayList<Digest> digests = new ArrayList<>();
        // Create digests for the start of the channel
        for (int i = 0; i < TREE_DEPTH + 1; i++) {
            // Create new digest
            Digest digest = IotaFlashBridge.getDigest(
                    user.getSeed(),
                    user.getSeedIndex(),
                    user.getSecurity()
            );
            user.incrementSeedIndex();
            System.out.println("Adding digest (" + digest.toString() + ") to user " + user.getUserIndex());
            // Increment key index

            digests.add(digest);
        }
        return digests;
    }


    /**
     *
     * @param allDigests
     * @param currentUser
     * @return
     */
    public static List<Multisig> getMultisigsForUser(List<List<Digest>> allDigests, UserObject currentUser) {

        // Generate the first addresses
        ArrayList<Multisig> multisigs = new ArrayList<Multisig>();

        // Loop for all digests.
        for (int index = 0; index < allDigests.get(0).size(); index++) {
            ArrayList<Digest> alignedDigests = new ArrayList<>();

            int securitySum = 0;

            // Loop for all users.
            for (int userIndex = 0; userIndex < allDigests.size(); userIndex++) {
                Digest digest = allDigests.get(userIndex).get(index);
                // Get array of digests for all users.
                alignedDigests.add(digest);
                securitySum += digest.getSecurity();
            }

            // Create multisgAddr from digests.
            Multisig multisig = IotaFlashBridge.composeAddress(alignedDigests);

            // Get digests data for current user.
            Digest digest = allDigests.get(currentUser.getUserIndex()).get(index);

            multisig.setIndex(digest.getIndex());
            multisig.setSigningIndex(currentUser.getUserIndex() * digest.getSecurity());
            multisig.setSecuritySum(securitySum);
            multisig.setSecurity(digest.getSecurity());
            System.out.println("[INFO] new multisig " + multisig.getAddress());
            // System.out.println("Creating address " + multisig.getAddress() + " index" + multisig.getIndex() + " signingIndex: " + multisig.getSigningIndex());

            multisigs.add(multisig);
        }

        return multisigs;
    }


    /**
     *
     * @param user
     * @param multisig
     * @return
     */
    public static Multisig updateMultisigChildrenForUser(UserObject user, Multisig multisig) {
        FlashObject flash = user.getFlash();
        Multisig originAddress = flash.getRoot().find(multisig.getAddress());
        if (originAddress != null) {

            System.out.println("[INFO]: found address in user" + user.getUserIndex() + " data");
            originAddress.setChildren(multisig.getChildren());
            originAddress.setBundles(multisig.getBundles());
            originAddress.setSecurity(multisig.getSecurity());
            return originAddress;
        }
        return null;
    }

    /**
     * Apply transfers to a user flash state.
     * @param signedBundles
     * @param user
     */
    public static void applyTransfers(ArrayList<Bundle> signedBundles, UserObject user) {
        // Apply transfers to User ONE
        FlashObject newFlash = IotaFlashBridge.applyTransfersToUser(user, signedBundles);

        // Set new flash object to user
        user.setFlash(newFlash);
    }

    /**
     * Send trytes array to the node specified in the IotaAPI setup.
     * @param trytes
     * @param api
     * @return returns the transactions applied to the node tangle.
     */
    public static List<jota.model.Transaction> sendTrytes(String[] trytes, IotaAPI api, int depth, int minWeightMagnitude) {

        try {
            System.out.println("[INFO] Sinding close bundle... This can take some time");
            List<jota.model.Transaction> txs = api.sendTrytes(trytes, depth, minWeightMagnitude);
            return txs;
        } catch (IllegalAccessError error) {
            System.out.println("[ERROR] " + error.getLocalizedMessage());
        } catch (Exception exception) {
            System.out.println("[ERROR]: could not send trytes " + exception.getLocalizedMessage());
        }

        return new ArrayList<jota.model.Transaction>();
    }


    public static List<Bundle> POWClosedBundle(List<Bundle> bundles, int depth, int minWeightMagnitude) {
        List<Bundle> attachedBundles = new ArrayList<>();
        for (Bundle b : bundles) {
            String[] trytes = b.toTrytesArray();
            List<jota.model.Transaction> txs = sendTrytes(trytes, getIotaAPI(), depth, minWeightMagnitude);
            if (txs != null && txs.size() > 0) {
                Bundle bundle = new Bundle(txs);
                attachedBundles.add(bundle);
            }
        }

        return attachedBundles;
    }

    /**
     * creates a new iota instace with the defined url and mode (testnet or not)
     * if api instance available the just return it
     * @return IotaAPI instance with setup url
     */
    public static IotaAPI getIotaAPI() {
        if (iotaAPI == null) {
            return getNewIotaAPI();
        }
        return iotaAPI;
    }

    /**
     * Creates a new instance of iota api and override the currently set one.
     * Can be used to change url settings.
     * @return IotaAPI instance with setup url
     */
    public static IotaAPI getNewIotaAPI() {
        URL nodeURL;

        try {
            if (useTestnet) {
                nodeURL = new URL(testNetNodeURL);
            } else {
                nodeURL = new URL(netNodeURL);
            }
            iotaAPI = new IotaAPI.Builder()
                    .protocol(nodeURL.getProtocol())
                    .host(nodeURL.getHost())
                    .port(String.valueOf(nodeURL.getPort()))
                    .build();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to create IotaAPI instance." + e.getLocalizedMessage());
            return null;
        }

        return iotaAPI;
    }


    /**
     *
     * Utilities
     */

    /**
     * gives a new funded seed from a seedGeneratorURL
     * @return
     */
    public static GeneratedSeed getNewSeed() {
        try {
            String seedData = readUrl(seedGeneratorURL);
            Gson gson = new Gson();
            GeneratedSeed genSeed = gson.fromJson(seedData, GeneratedSeed.class);
            return genSeed;
        } catch (Exception e) {
            System.out.println("[ERROR]: Failed to get new testned seed" + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Get the total left in the flash channel.
     * @param user
     * @return
     */
    public static double getFlashDeposits(UserObject user) {
        double sum = 0;
        for (double deposit : user.getFlash().getDeposits()) {
            sum += deposit;
        }
        return sum;
    }

    /**
     * get current output of the flash channel. All transactions and remainder.
     * @param user UserObject for which to compute amount
     * @return amount of IOTA
     */
    public static double getBalanceOfUser(UserObject user) {
        FlashObject flash = user.getFlash();
        double balance = flash.getDeposits().get(user.getUserIndex());
        Map<String, Integer> transfers = flash.getOutputs();
        for (Map.Entry<String, Integer> transfer : transfers.entrySet()) {
            String userSettlementAddr = flash.getSettlementAddresses().get(user.getUserIndex());
            if (transfer.getKey().equals(userSettlementAddr)) {
                balance += transfer.getValue();
            }
        }

        return balance;
    }

    /**
     * Returns the amount of iota deposited in a selected address
     * @param address
     * @return
     */
    public static long getBalance(String address) {
        ArrayList<String> addreses = new ArrayList<>();
        addreses.add(address);
        IotaAPI api = getIotaAPI();
        try {
            GetBalancesResponse resp = api.getBalances(100, addreses);
            return Long.parseLong(resp.getBalances()[0]);
        } catch (Exception e) {
            System.out.println("[ERROR]: could not read balance for account " + address + " with error " + e.getLocalizedMessage());
            return -1;
        }
    }

    /**
     * Utility for reading date from a provided url string.
     * @param urlString
     * @return
     * @throws Exception
     */
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static Transaction cloneTransaction(jota.model.Transaction transaction) {
        return new Transaction(
                transaction.getSignatureFragments(),
                transaction.getCurrentIndex(),
                transaction.getLastIndex(),
                transaction.getNonce(),
                transaction.getHash(),
                transaction.getObsoleteTag(),
                transaction.getTimestamp(),
                transaction.getTrunkTransaction(),
                transaction.getBranchTransaction(),
                transaction.getAddress(),
                transaction.getValue(),
                transaction.getBundle(),
                transaction.getTag(),
                transaction.getAttachmentTimestamp(),
                transaction.getAttachmentTimestampLowerBound(),
                transaction.getAttachmentTimestampUpperBound()
        );
    }

    public static String convertStreamToString(InputStream p_is) throws IOException {
    /*
     * To convert the InputStream to String we use the
     * BufferedReader.readLine() method. We iterate until the BufferedReader
     * return null which means there's no more data to read. Each line will
     * appended to a StringBuilder and returned as String.
     */
        if (p_is != null) {
            StringBuilder m_sb = new StringBuilder();
            String m_line;
            try {
                BufferedReader m_reader = new BufferedReader(
                        new InputStreamReader(p_is));
                while ((m_line = m_reader.readLine()) != null) {
                    m_sb.append(m_line).append("\n");
                }
            } finally {
                p_is.close();
            }
            return m_sb.toString();
        } else {
            return "";
        }
    }
}
