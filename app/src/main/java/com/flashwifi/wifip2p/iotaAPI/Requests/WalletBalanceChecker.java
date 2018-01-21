package com.flashwifi.wifip2p.iotaAPI.Requests;

/**
 * Created by Toby on 1/6/2018.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import com.flashwifi.wifip2p.AddressBalanceTransfer;

import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.error.ArgumentException;

public class WalletBalanceChecker extends AsyncTask<Void, Void, Void> {

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

    public WalletBalanceChecker(Context inActivity, String inPrefFile, String inSeed, Handler inMHandler, int inType, Boolean inUpdateMessage) {
        context = inActivity;
        prefFile = inPrefFile;
        seed = inSeed;
        mHandler = inMHandler;
        type = inType;
        updateMessage = inUpdateMessage;

        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean testnet = prefManager.getBoolean("pref_key_switch_testnet",false);

        if(testnet){
            //Testnet node:
            api = new IotaAPI.Builder()
                    .protocol("https")
                    .host("testnet140.tangle.works")
                    .port("443")
                    .build();
        }
        else{
            //Mainnet node:
            api = new IotaAPI.Builder()
                    .protocol("http")
                    .host("node.iotawallet.info")
                    .port("14265")
                    .build();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if(context != null){

            WalletAddressChecker addressChecker = new WalletAddressChecker(context,prefFile);
            List<String> addressList = addressChecker.getAddress(seed);

            if(addressList != null && addressList.get(0).equals("Unable to resolve host")){
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

}