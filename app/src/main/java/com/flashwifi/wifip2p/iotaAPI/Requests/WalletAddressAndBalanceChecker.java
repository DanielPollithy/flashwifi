package com.flashwifi.wifip2p.iotaAPI.Requests;

/**
 * Created by Toby on 1/6/2018.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.flashwifi.wifip2p.AddressBalanceTransfer;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.dto.response.GetNewAddressResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class WalletAddressAndBalanceChecker extends AsyncTask<Void, Void, Void> {

    private static final int BALANCE_RETRIEVE_TASK_COMPLETE = 1;

    private static final int FUND_WALLET = 0;
    private static final int WITHDRAW_WALLET = 1;

    private static IotaAPI api;
    private static Context context;
    private String prefFile;
    private String seed;
    private Handler mHandler;
    private int type;
    private Boolean updateMessage;

    //GetNodeInfoResponse response = api.getNodeInfo();

    public WalletAddressAndBalanceChecker(Context inActivity, String inPrefFile, String inSeed, Handler inMHandler, int inType, Boolean inUpdateMessage) {
        context = inActivity;
        prefFile = inPrefFile;
        seed = inSeed;
        mHandler = inMHandler;
        type = inType;
        updateMessage = inUpdateMessage;

        //Mainnet node:
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

    @Override
    protected Void doInBackground(Void... voids) {
        if(context != null){
            List<String> addressList = getAddress(seed);

            if(addressList != null && addressList.get(0) == "Unable to resolve host"){
                AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(null,null,"hostError");
                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, addressBalanceTransfer);
                completeMessage.sendToTarget();
            }
            else if(addressList != null){

                String depositAddress = addressList.get(addressList.size()-1);
                String balance = getBalance(addressList);

                if(balance != null){
                    if(type == WITHDRAW_WALLET && updateMessage == false){
                        AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(depositAddress,balance,"noErrorNoUpdateMessage");
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE,addressBalanceTransfer);
                        completeMessage.sendToTarget();
                    }
                    else{
                        AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(depositAddress,balance,"noError");
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE,addressBalanceTransfer);
                        completeMessage.sendToTarget();
                    }
                }
                else{
                    //Balance Retrieval Error
                    AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(null,null,"balanceError");
                    Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, addressBalanceTransfer);
                    completeMessage.sendToTarget();
                }
            }
            else{
                //Address Retrieval Error
                AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(null,null,"addressError");
                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, addressBalanceTransfer);
                completeMessage.sendToTarget();
            }
        }
        return null;
    }

    public String getBalance(List<String> inAddresses){
        String[] balanceArray;
        try {
            GetBalancesResponse balanceResultResponse = api.getBalances(100, inAddresses);
            balanceArray = balanceResultResponse.getBalances();
        } catch (ArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return null;
        }

        if(balanceArray.length>1){
            return balanceArray[balanceArray.length-2];
        }
        else{
            return balanceArray[balanceArray.length-1];
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