package com.flashwifi.wifip2p;

/**
 * Created by Toby on 1/6/2018.
 */

import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.error.ArgumentException;

public class WalletAddressAndBalanceChecker {

    private static IotaAPI api;
    //GetNodeInfoResponse response = api.getNodeInfo();

    public WalletAddressAndBalanceChecker() {
        //Local node:
        //api = new IotaAPI.Builder().build();

        //Local node:
        api = new IotaAPI.Builder()
                .protocol("http")
                .host("node.iotawallet.info")
                .port("14265")
                .build();
    }

    public String getBalance(List<String> inAaddresses){
        try {
            GetBalancesResponse balanceResultResponse = api.getBalances(100, inAaddresses);
            String balance = balanceResultResponse.getBalances()[0];
            return balance;
        } catch (ArgumentException e) {
            e.printStackTrace();
            System.out.println("getBalance Error!");
        }
        return null;
    }

    public List<String> getAddress(String seed) {
        List<String> addressList = null;
        GetNewAddressResponse addressResponse = null;
        try {
            addressResponse = api.getNewAddress(seed, 2, 0, false, 1, true);
        } catch (ArgumentException e) {
            e.printStackTrace();
        }
        if(addressResponse != null){
            addressList = addressResponse.getAddresses();
        }
        return addressList;
    }
}