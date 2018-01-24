package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import com.flashwifi.wifip2p.AddressBalanceTransfer;
import com.flashwifi.wifip2p.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetBalancesAndFormatResponse;
import jota.dto.response.GetBalancesResponse;
import jota.error.ArgumentException;
import jota.utils.StopWatch;

public class WalletBalanceChecker extends AsyncTask<Void, Void, Void> {

    private static final int BALANCE_RETRIEVE_TASK_COMPLETE = 1;

    private static final int FUND_WALLET = 0;
    private static final int WITHDRAW_WALLET = 1;
    private static final int PREF_UPDATE = 2;

    private static IotaAPI api;
    private static Context context;
    private String prefFile;
    private String seed;
    private Handler mHandler;
    private int type;
    private Boolean updateMessage;

    public WalletBalanceChecker(Context inActivity, String inPrefFile, String inSeed, Handler inMHandler, int inType, Boolean inUpdateMessage) {
        context = inActivity;
        prefFile = inPrefFile;
        seed = inSeed;
        mHandler = inMHandler;
        type = inType;
        updateMessage = inUpdateMessage;

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

    @Override
    protected Void doInBackground(Void... voids) {

        if(context != null){

            WalletAddressChecker addressChecker = new WalletAddressChecker(context,prefFile);
            List<String> addressList = addressChecker.getAddress(seed);
            boolean containsPendingTransactions = addressChecker.getContainsPendingTransaction();
            boolean keyIndexChanged = addressChecker.getkeyIndexChanged();

            if(addressList != null && addressList.get(0).equals("Unable to resolve host")){
                AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(null,null,"hostError");
                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, addressBalanceTransfer);
                completeMessage.sendToTarget();
            }
            else if(addressList != null){

                String depositAddress = addressList.get(addressList.size()-1);
                String balance = getBalance(addressList, containsPendingTransactions, keyIndexChanged);

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

    public String getBalance(List<String> inAddresses, boolean containsPendingTransactions, boolean keyIndexChanged){

        String updatedBalanceString;
        try {
            StopWatch stopWatch = new StopWatch();

            SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
            String security = prefManager.getString("pref_key_security","2");
            int securityInt = Integer.parseInt(security);

            GetBalancesAndFormatResponse balanceResponse = api.getBalanceAndFormat(inAddresses, 0, 0, stopWatch, securityInt);
            long total = balanceResponse.getTotalBalance();
            long storedBaseBalance = Long.parseLong(getBaseSharedPrefKeyBalance());
            long updatedBaseBalance = storedBaseBalance + total;
            updatedBalanceString = Long.toString(updatedBaseBalance);

            //Pending Transaction, no new confirmed transactions
            if(containsPendingTransactions && !keyIndexChanged){
                //No action required
            }
            //Pending Transaction, new confirmed transactions
            else if(containsPendingTransactions && keyIndexChanged){
                putSharedPrefBalance(updatedBalanceString);
            }
            //No Pending Transactions, no new confirmed transactions
            else if(!containsPendingTransactions && !keyIndexChanged){
                //No action required
            }
            //No Pending Transactions, new confirmed transactions
            else if(!containsPendingTransactions && keyIndexChanged){
                putBaseSharedPrefBalance(updatedBalanceString);
                updatedBalanceString = Long.toString(updatedBaseBalance);
                putSharedPrefBalance(updatedBalanceString);
            }
        } catch (ArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return getSharedPrefKeyBalance()+"i (cached: "+getSharedPrefKeyBalanceDateUpdate()+")";
        }
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        putSharedPrefBalanceDateUpdate(currentDateTimeString);
        return updatedBalanceString+"i";
    }

    private void putSharedPrefBalanceDateUpdate(String currentDateTimeString) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                prefFile, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("balanceDateUpdate", currentDateTimeString);
        editor.apply();
    }

    private String getSharedPrefKeyBalanceDateUpdate() {
        SharedPreferences sharedPref = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        String defaultValue = "0";
        String storedBalance = sharedPref.getString("balanceDateUpdate",defaultValue);
        return storedBalance;
    }

    private String getSharedPrefKeyBalance() {
        SharedPreferences sharedPref = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        String defaultValue = "0";
        String storedBalance = sharedPref.getString("balance",defaultValue);
        return storedBalance;
    }

    private void putSharedPrefBalance(String inBalance) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                prefFile, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("balance", inBalance);
        editor.apply();
    }

    private String getBaseSharedPrefKeyBalance() {
        SharedPreferences sharedPref = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        String defaultValue = "0";
        String storedBalance = sharedPref.getString("baseBalance",defaultValue);
        return storedBalance;
    }

    private void putBaseSharedPrefBalance(String inBalance) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                prefFile, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("baseBalance", inBalance);
        editor.apply();
    }

}