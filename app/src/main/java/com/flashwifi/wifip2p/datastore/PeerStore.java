package com.flashwifi.wifip2p.datastore;

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
        boolean created = peers.containsKey(macAddress);

        if (!created) {
            // Temp store for the important values
        }
        // overwrite or insert
        peers.put(macAddress, peer);


        return created;
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
        if (peers.containsKey(address_)) {
            return peers.get(address_);
        }

        PeerInformation temp = new PeerInformation();
        peers.put(address_, temp);
        return temp;
    }

    public void setLatestOffer(String macAddress, NegotiationOffer offer) {
        getOrCreatePeer(macAddress).setLatestOffer(offer);
    }

    public void setLatestOfferAnswer(String macAddress, NegotiationOfferAnswer answer) {
        getOrCreatePeer(macAddress).setLatestOfferAnswer(answer);
    }

    public void setLatestFinalization(String macAddress, NegotiationFinalization finalization) {
        getOrCreatePeer(macAddress).setLatestFinalization(finalization);
    }
}
