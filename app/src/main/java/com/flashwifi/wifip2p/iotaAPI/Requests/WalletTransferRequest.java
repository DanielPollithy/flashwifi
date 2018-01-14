package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.flashwifi.wifip2p.AddressBalanceTransfer;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transfer;

public class WalletTransferRequest extends AsyncTask<Void, Void, Void> {

    private static final int TRANSFER_TASK_COMPLETE = 2;

    private static IotaAPI api;
    private static Context context;
    private String appWalletSeed;
    private String sendAddress;
    private String sendAmount;
    private String message;
    private String tag;
    private String transferResult;
    private Handler mHandler;

    public WalletTransferRequest(String inSendAddress, String inAppWalletSeed, String inSendAmount, String inMessage, String inTag, Context inContext, Handler inMHandler) {

        sendAddress = inSendAddress;
        appWalletSeed = inAppWalletSeed;
        sendAmount = inSendAmount;
        message = inMessage;
        tag = inTag;
        context = inContext;
        mHandler = inMHandler;

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
        sendRequest();

        String result = null;

        if(context != null){
            result = sendRequest();
        }

        AddressBalanceTransfer addressBalanceTransfer = new AddressBalanceTransfer(null,null,null);
        addressBalanceTransfer.setMessage(result);

        Message completeMessage = mHandler.obtainMessage(TRANSFER_TASK_COMPLETE, addressBalanceTransfer);
        completeMessage.sendToTarget();
        return null;
    }

    public String sendRequest(){

        List<Transfer> transfers = new ArrayList<>();
        transfers.add(new Transfer(sendAddress, Long.valueOf(sendAmount), message, tag));

        SendTransferResponse sendTransferResponse = null;

        try {
                //Mainnet
                //sendTransferResponse = api.sendTransfer(appWalletSeed, 2, 4, 18, transfers, null, null, false);

                //Testnet
                sendTransferResponse = api.sendTransfer(appWalletSeed, 2, 4, 9, transfers, null, null, false);

        } catch (ArgumentException | IllegalAccessError | IllegalStateException e) {
            if (e instanceof ArgumentException) {
                if (e.getMessage().contains("Sending to a used address.") || e.getMessage().contains("Private key reuse detect!") || e.getMessage().contains("Send to inputs!")) {
                    transferResult = "Sending to a used address/Private key reuse detect. Error Occurred.";
                }
                else if(e.getMessage().contains("Failed to connect to")){
                    transferResult = "Failed to connect to";
                }
                else{
                    transferResult = "Network Error: Attaching new address failed or Transaction failed";
                }
            }

            if (e instanceof IllegalAccessError) {
                transferResult = "Local POW needs to be enabled";
            }
        }
        if(sendTransferResponse != null){
            Boolean[] transferSuccess = sendTransferResponse.getSuccessfully();

            Boolean success = true;
            for (Boolean status : transferSuccess) {
                if(status.equals(false)){
                    success = false;
                    break;
                }
            }

            if(success){
                transferResult = "Sent";
            }
            else{
                transferResult = "Transfer not successful, check transfer status via Tangle explorer";
            }

        }
        else{
            transferResult = "Transfer error: Send Response not received";
        }
        return transferResult;
    }
}
