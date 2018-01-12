package com.flashwifi.wifip2p.iotaAPI.Requests;

/**
 * Created by Toby on 1/6/2018.
 */

import android.content.Context;
import android.content.SharedPreferences;

import com.flashwifi.wifip2p.R;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class WalletAddressAndBalanceChecker {

    private static IotaAPI api;
    private static Context context;
    private String prefFile;
    //GetNodeInfoResponse response = api.getNodeInfo();

    public WalletAddressAndBalanceChecker(Context inActivity, String inPrefFile) {
        context = inActivity;
        prefFile = inPrefFile;
        //Local node:
        //api = new IotaAPI.Builder().build();

        //Live node:
        /*
        api = new IotaAPI.Builder()
                .protocol("http")
                .host("node.iotawallet.info")
                .port("14265")
                .build();
        */

        //Testnet node:
        api = new IotaAPI.Builder()
                .protocol("https")
                .host("testnet140.tangle.works")
                .port("443")
                .build();

    }

    public String getBalance(List<String> inAddresses){
        try {
            GetBalancesResponse balanceResultResponse = api.getBalances(100, inAddresses);
            String[] balanceArray = balanceResultResponse.getBalances();
            return balanceArray[balanceArray.length-2];
        } catch (ArgumentException | IllegalStateException e) {
            e.printStackTrace();
            System.out.println("getBalance Error!");
            if(e.getMessage().contains("Unable to resolve host")){
                System.out.println("Unable to resolve host");
            }
        }
        return null;
    }

    public List<String> getAddress(String seed) {

        Boolean foundAddress = false;
        List<String> addressList = new ArrayList<>();
        int keyIndex = getKeyIndex();

        System.out.println("GetAddress");

        while(foundAddress == false){

            System.out.println(keyIndex);

            GetNewAddressResponse addressResponse = null;
            try {
                addressResponse = api.getNewAddress(seed, 2, keyIndex, false, 1, false);
            } catch (ArgumentException e) {
                e.printStackTrace();
            }

            if(addressResponse != null) {
                addressList.add(addressResponse.getAddresses().get(0));

                System.out.println("CurAddress");
                System.out.println(addressList.get(0));

                String[] addressesCheckArray = new String[1];
                addressesCheckArray[0] = addressResponse.getAddresses().get(0);

                List<Transaction> transactionsForAddress = null;
                try {
                    transactionsForAddress = api.findTransactionObjectsByAddresses(addressesCheckArray);
                } catch (ArgumentException | IllegalStateException | IllegalAccessError e) {
                    e.printStackTrace();
                    if(e.getMessage().contains("Unable to resolve host")){
                        List<String> errorStringList = new ArrayList<>();
                        errorStringList.add("Unable to resolve host");
                        return errorStringList;
                    }
                }

                if(transactionsForAddress.isEmpty() || (transactionsForAddress.size() == 0 || transactionsForAddress.equals(null))){
                    //Transactions not found, use this address
                    foundAddress = true;
                    System.out.println("No transactions found");
                }
                else{
                    //Found transactions, increment for new address
                    keyIndex+=1;
                    System.out.println("Found transactions");
                }
            }
        }
        //Put the second last address to search
        putKeyIndex(keyIndex-1);
        return addressList;
    }

    private int getKeyIndex() {
        SharedPreferences sharedPref = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        int keyIndex = sharedPref.getInt("keyIndex",0);
        return keyIndex;
    }

    private void putKeyIndex(int inKeyIndex) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                prefFile, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("keyIndex", inKeyIndex);
        editor.apply();
    }

}