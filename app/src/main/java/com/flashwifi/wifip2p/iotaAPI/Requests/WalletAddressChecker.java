package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flashwifi.wifip2p.R;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetInclusionStateResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class WalletAddressChecker {

    private static IotaAPI api;
    private Context context;
    private String prefFile;

    boolean containsPendingTransaction = false;
    boolean keyIndexChanged = false;

    public WalletAddressChecker(Context inContext, String inPrefFile){

        context = inContext;
        prefFile = inPrefFile;

        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean testnet = prefManager.getBoolean("pref_key_switch_testnet",false);
        Boolean testnetPrivate = prefManager.getBoolean("pref_key_switch_testnet_private",false);

        if(testnet){
            //Testnet node:
            if(testnetPrivate){
                //Private test node
                api = new IotaAPI.Builder()
                        .protocol(context.getResources().getString(R.string.protocolPrivateTestnetNode))
                        .host(context.getResources().getString(R.string.hostPrivateTestnetNode))
                        .port(context.getResources().getString(R.string.portPrivateTestnetNode))
                        .build();
            }
            else{
                //Public test node
                api = new IotaAPI.Builder()
                        .protocol(context.getResources().getString(R.string.protocolPublicTestnetNode))
                        .host(context.getResources().getString(R.string.hostPublicTestnetNode))
                        .port(context.getResources().getString(R.string.portPublicTestnetNode))
                        .build();
            }
        }
        else{
            //Mainnet node:
            api = new IotaAPI.Builder()
                    .protocol(context.getResources().getString(R.string.protocolDefaultMainnetNode))
                    .host(context.getResources().getString(R.string.hostDefaultMainnetNode))
                    .port(context.getResources().getString(R.string.portDefaultMainnetNode))
                    .build();
        }
    }

    public List<String> getAddress(String seed) {

        Boolean foundAddress = false;
        List<String> addressList = new ArrayList<>();
        int keyIndex = getKeyIndex();
        ArrayList<String> hashStringList = new ArrayList<>();

        while(foundAddress == false){

            GetNewAddressResponse addressResponse = null;
            try {
                SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
                String security = prefManager.getString("pref_key_security","2");
                int securityInt = Integer.parseInt(security);
                addressResponse = api.getNewAddress(seed, securityInt, keyIndex, true, 1, false);
            } catch (ArgumentException e) {
                e.printStackTrace();
            }

            if(addressResponse != null) {
                addressList.add(addressResponse.getAddresses().get(0));

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
                }
                else{
                    //Found transactions, increment for new address

                    String curHash = transactionsForAddress.get(0).getHash();
                    hashStringList.add(curHash);
                    keyIndex = keyIndex + 1;
                }
            }
        }
        String[] hashStringArray = new String[hashStringList.size()];
        int hashIndex = 0;
        //Convert hash String List to String Array
        for (String curHash : hashStringList) {
            hashStringArray[hashIndex] = curHash;
            hashIndex = hashIndex + 1;
        }

        //Check whether pending transactions exist
        containsPendingTransaction = false;
        keyIndexChanged = false;
        try {
            GetInclusionStateResponse inclusionResponse = api.getLatestInclusion(hashStringArray);
            boolean[] states = inclusionResponse.getStates();

            for (boolean state : states) {
                if(!state){
                    containsPendingTransaction = true;
                }
            }
        } catch (ArgumentException e) {
            e.printStackTrace();
        }

        if(!containsPendingTransaction){
            //all confirmed transactions, ok to change keyIndex
            if(keyIndex != getKeyIndex()){
                keyIndexChanged = true;
                putKeyIndex(keyIndex);
            }
        }

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

    public boolean getContainsPendingTransaction() {
        return containsPendingTransaction;
    }

    public boolean getkeyIndexChanged() {
        return keyIndexChanged;
    }

}