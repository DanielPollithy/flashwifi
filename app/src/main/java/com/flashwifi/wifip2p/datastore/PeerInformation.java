package com.flashwifi.wifip2p.datastore;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.support.annotation.NonNull;

import com.flashwifi.wifip2p.protocol.NegotiationFinalization;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;
import com.flashwifi.wifip2p.protocol.NegotiationOfferAnswer;

import java.util.Date;


public class PeerInformation  {
    private String ipAddress;
    private WifiP2pInfo p2pInfo;
    private WifiP2pDevice wifiP2pDevice;
    private Date lastUpdate;

    // age stores how long it has been since the last signal from this peer
    // it is not stored in seconds but in update cycles
    private int age;

    private NegotiationOffer latestNegotiationOffer;
    private NegotiationOfferAnswer latestOfferAnswer;
    private NegotiationFinalization latestFinalization;

    public PeerInformation() {
        age = 0;
    }

    public WifiP2pDevice getWifiP2pDevice() {
        return wifiP2pDevice;
    }

    public void setWifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }

    public void incrementAge() {
        age += 1;
    }

    public int getAge() {
        return age;
    }

    public void setLatestOffer(NegotiationOffer offer) {
        latestNegotiationOffer = offer;
    }

    public NegotiationOffer getLatestNegotiationOffer() {
        return latestNegotiationOffer;
    }

    public void setLatestOfferAnswer(NegotiationOfferAnswer latestOfferAnswer) {
        this.latestOfferAnswer = latestOfferAnswer;
    }

    public NegotiationOfferAnswer getLatestOfferAnswer() {
        return latestOfferAnswer;
    }

    public void setLatestFinalization(NegotiationFinalization latestFinalization) {
        this.latestFinalization = latestFinalization;
    }

    public NegotiationFinalization getLatestFinalization() {
        return latestFinalization;
    }
}
