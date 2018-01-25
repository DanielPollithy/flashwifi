package com.flashwifi.wifip2p.datastore;

import android.util.Log;

import com.flashwifi.wifip2p.protocol.BillingCloseChannel;
import com.flashwifi.wifip2p.protocol.BillingCloseChannelAnswer;
import com.flashwifi.wifip2p.protocol.BillingOpenChannel;
import com.flashwifi.wifip2p.protocol.BillingOpenChannelAnswer;
import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class PeerStore {
    private static final PeerStore ourInstance = new PeerStore();

    private HashMap<String, PeerInformation> peers = new HashMap<String, PeerInformation>();

    private NegotiationOffer storedNegotiationOffer = null;
    private NegotiationOfferAnswer storedNegotiationOfferAnswer = null;
    private NegotiationFinalization storedNegotiationFinalization = null;

    private BillingOpenChannel storedBillingOpenChannel = null;
    private BillingOpenChannelAnswer storedBillingOpenChannelAnswer = null;

    private boolean debug = true;

    private String lastMAC = null;



    public static synchronized PeerStore getInstance() {
        return ourInstance;
    }

    private PeerStore() {
        // ToDo: load and store stuff in SQLite or gson JSON
    }

    public synchronized void clear() {
        peers.clear();
    }

    /**
     * Update or create a peer based on the MAC address
     *
     * @return was the peer created?
     */
    public synchronized boolean updateOrCreate(PeerInformation peer) {
        String macAddress = peer.getWifiP2pDevice().deviceAddress;
        boolean exists = peers.containsKey(macAddress);

        if (exists) {
            // Temp store for the important values
            peer.setSelected(peers.get(macAddress).isSelected());
        }
        // overwrite or insert
        peers.put(macAddress, peer);



        return !exists;
    }

    public synchronized ArrayList<PeerInformation> getPeerArrayList() {
        ArrayList<PeerInformation> ary = new ArrayList<>(peers.values());
        Collections.sort(ary, new Comparator<PeerInformation>() {
            @Override
            public int compare(PeerInformation lhs, PeerInformation rhs) {

                return Integer.valueOf(lhs.getAge()).compareTo(rhs.getAge());
            }
        });
        return ary;
    }

    /**
     * This sets an old flag to all the peers
     */
    public synchronized void makeNewGeneration() {
        for (PeerInformation peer : peers.values()) {
            peer.incrementAge();
        }
    }

    private synchronized PeerInformation getOrCreatePeer(String address_) {
        address_ = address_.toLowerCase();
        if (peers.containsKey(address_)) {
            return peers.get(address_);
        }

        PeerInformation temp = new PeerInformation();
        peers.put(address_, temp);
        return temp;
    }

    public synchronized void setLatestOffer(String macAddress, NegotiationOffer offer) {
        storedNegotiationOffer = offer;
        getOrCreatePeer(macAddress.toLowerCase()).setLatestOffer(offer);
    }

    public synchronized void setErrorMessage(String macAddress, String msg) {
        getOrCreatePeer(macAddress.toLowerCase()).setErrorMessage(msg);
    }

    public synchronized void setSelected(String macAddress, boolean selected) {
        getOrCreatePeer(macAddress.toLowerCase()).setSelected(selected);
    }

    public synchronized void unselectAll() {
        for (PeerInformation p: peers.values()) {
            p.setSelected(false);
        }
    }

    public synchronized void setLatestOfferAnswer(String macAddress, NegotiationOfferAnswer answer) {
        storedNegotiationOfferAnswer = answer;
        getOrCreatePeer(macAddress.toLowerCase()).setLatestOfferAnswer(answer);
    }

    public synchronized void setLatestFinalization(String macAddress, NegotiationFinalization finalization) {
        lastMAC = macAddress;
        storedNegotiationFinalization = finalization;
        getOrCreatePeer(macAddress.toLowerCase()).setLatestFinalization(finalization);
    }

    public synchronized NegotiationFinalization getLatestFinalization(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestFinalization();
        }
        return storedNegotiationFinalization;
    }

    public synchronized NegotiationOffer getLatestNegotiationOffer(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestNegotiationOffer();
        }
        return storedNegotiationOffer;
    }

    public synchronized NegotiationOfferAnswer getLatestNegotiationOfferAnswer(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestOfferAnswer();
        }
        return storedNegotiationOfferAnswer;
    }

    public synchronized void setIPAddress(String macAddress, InetAddress IPAddress) {
        getOrCreatePeer(macAddress.toLowerCase()).setIPAddress(IPAddress.getHostAddress());
    }

    public synchronized void setLatestBillingOpenChannel(String macAddress, BillingOpenChannel o) {
        storedBillingOpenChannel = o;
        getOrCreatePeer(macAddress.toLowerCase()).setBillingOpenChannel(o);
    }

    public synchronized void setLatestBillingOpenChannelAnswer(String macAddress, BillingOpenChannelAnswer o) {
        storedBillingOpenChannelAnswer = o;
        getOrCreatePeer(macAddress.toLowerCase()).setBillingOpenChannelAnswer(o);
    }

    public synchronized BillingOpenChannel getLatestBillingOpenChannel(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getBillingOpenChannel();
        }
        return storedBillingOpenChannel;
    }

    public synchronized BillingOpenChannelAnswer getLatestBillingOpenChannelAnswer(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getBillingOpenChannelAnswer();
        }
        return storedBillingOpenChannelAnswer;
    }

    public synchronized void setLatestBillingCloseChannel(String macAddress, BillingCloseChannel o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingCloseChannel(o);
    }

    public synchronized void setLatestBillingCloseChannelAnswer(String macAddress, BillingCloseChannelAnswer o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingCloseChannelAnswer(o);
    }

    public synchronized PeerInformation getPeer(String address) {
        address = address.toLowerCase();
        if (peers.containsKey(address)) {
            return peers.get(address);
        }
        return null;
    }

}
