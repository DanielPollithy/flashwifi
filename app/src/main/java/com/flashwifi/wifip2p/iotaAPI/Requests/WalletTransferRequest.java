package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;

public class WalletTransferRequest {

    private static IotaAPI api;
    private static Activity activity;
    private String appWalletSeed;
    private String sendAddress;
    private String sendAmount;
    private String message;
    private String tag;
    private String transferResult;


    public WalletTransferRequest(String inSendAddress, String inAppWalletSeed, String inSendAmount, String inMessage, String inTag, Activity inActivity) {

        sendAddress = inSendAddress;
        appWalletSeed = inAppWalletSeed;
        sendAmount = inSendAmount;
        message = inMessage;
        tag = inTag;
        activity = inActivity;

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
