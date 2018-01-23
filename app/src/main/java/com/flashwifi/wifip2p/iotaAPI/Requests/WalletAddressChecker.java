package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flashwifi.wifip2p.R;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetNewAddressResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class WalletAddressChecker {

    private static IotaAPI api;
    private Context context;
    private String prefFile;

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

        while(foundAddress == false){

            GetNewAddressResponse addressResponse = null;
            try {
                addressResponse = api.getNewAddress(seed, 2, keyIndex, false, 1, false);
            } catch (ArgumentException e) {
                e.printStackTrace();
            }

            if(addressResponse != null) {
                System.out.println("WalletAddressChecker - Address: "+addressResponse.getAddresses().get(0));
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

                    System.out.println("WalletAddressChecker value: "+transactionsForAddress.get(0).getValue());
                    System.out.println("WalletAddressChecker time: "+transactionsForAddress.get(0).getAttachmentTimestamp());
                    System.out.println("WalletAddressChecker address: "+transactionsForAddress.get(0).getAddress());

                    keyIndex = keyIndex + 1;
                }
            }
        }

        if(keyIndex == 0){
            //Put the initial address to search. No transactions for the seed yet.
            putKeyIndex(keyIndex);
        }
        else{
            //Put the second last address to search
            putKeyIndex(keyIndex-1);
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

}