package com.flashwifi.wifip2p.datastore;

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

    public static PeerStore getInstance() {
        return ourInstance;
    }

    private PeerStore() {
        // ToDo: load and store stuff in SQLite or gson JSON
    }

    public void clear() {
        peers.clear();
    }

    /**
     * Update or create a peer based on the MAC address
     *
     * @return was the peer created?
     */
    public boolean updateOrCreate(PeerInformation peer) {
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

    public ArrayList<PeerInformation> getPeerArrayList() {
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
    public void makeNewGeneration() {
        for (PeerInformation peer : peers.values()) {
            peer.incrementAge();
        }
    }

    private PeerInformation getOrCreatePeer(String address_) {
        address_ = address_.toLowerCase();
        if (peers.containsKey(address_)) {
            return peers.get(address_);
        }

        PeerInformation temp = new PeerInformation();
        peers.put(address_, temp);
        return temp;
    }

    public void setLatestOffer(String macAddress, NegotiationOffer offer) {
        getOrCreatePeer(macAddress.toLowerCase()).setLatestOffer(offer);
    }

    public void setErrorMessage(String macAddress, String msg) {
        getOrCreatePeer(macAddress.toLowerCase()).setErrorMessage(msg);
    }

    public void setSelected(String macAddress, boolean selected) {
        getOrCreatePeer(macAddress.toLowerCase()).setSelected(selected);
    }

    public void unselectAll() {
        for (PeerInformation p: peers.values()) {
            p.setSelected(false);
        }
    }

    public void setLatestOfferAnswer(String macAddress, NegotiationOfferAnswer answer) {
        getOrCreatePeer(macAddress.toLowerCase()).setLatestOfferAnswer(answer);
    }

    public void setLatestFinalization(String macAddress, NegotiationFinalization finalization) {
        getOrCreatePeer(macAddress.toLowerCase()).setLatestFinalization(finalization);
    }

    public NegotiationFinalization getLatestFinalization(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestFinalization();
        }
        return null;
    }

    public NegotiationOffer getLatestNegotiationOffer(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestNegotiationOffer();
        }
        return null;
    }

    public NegotiationOfferAnswer getLatestNegotiationOfferAnswer(String macAddress) {
        if (peers.containsKey(macAddress.toLowerCase())) {
            return peers.get(macAddress.toLowerCase()).getLatestOfferAnswer();
        }
        return null;
    }

    public void setIPAddress(String macAddress, InetAddress IPAddress) {
        getOrCreatePeer(macAddress.toLowerCase()).setIPAddress(IPAddress.getHostAddress());
    }

    public void setLatestBillingOpenChannel(String macAddress, BillingOpenChannel o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingOpenChannel(o);
    }

    public void setLatestBillingOpenChannelAnswer(String macAddress, BillingOpenChannelAnswer o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingOpenChannelAnswer(o);
    }

    public void setLatestBillingCloseChannel(String macAddress, BillingCloseChannel o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingCloseChannel(o);
    }

    public void setLatestBillingCloseChannelAnswer(String macAddress, BillingCloseChannelAnswer o) {
        getOrCreatePeer(macAddress.toLowerCase()).setBillingCloseChannelAnswer(o);
    }

    public PeerInformation getPeer(String address) {
        address = address.toLowerCase();
        if (peers.containsKey(address)) {
            return peers.get(address);
        }
        return null;
    }

}
