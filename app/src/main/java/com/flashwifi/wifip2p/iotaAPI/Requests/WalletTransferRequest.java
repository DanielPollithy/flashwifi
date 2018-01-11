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

    public String sendRequest(){

        List<Transfer> transfers = new ArrayList<>();

        System.out.println(sendAddress);
        System.out.println(Long.valueOf(sendAmount));
        System.out.println("message: "+message);
        System.out.println("tag: "+tag);

        transfers.add(new Transfer(sendAddress, Long.valueOf(sendAmount), message, tag));

        SendTransferResponse sendTransferResponse = null;

        try {
            System.out.println("=====sendTransfer=====");
                //Mainnet
                //sendTransferResponse = api.sendTransfer(appWalletSeed, 2, 4, 18, transfers, null, null, false);

                //Testnet
                sendTransferResponse = api.sendTransfer(appWalletSeed, 2, 4, 9, transfers, null, null, false);

            System.out.println("=====sendTransferNextLine=====");
        } catch (ArgumentException | IllegalAccessError | IllegalStateException e) {
            //e.printStackTrace();
            System.out.println("=====Error=====");
            if (e instanceof ArgumentException) {
                if (e.getMessage().contains("Sending to a used address.") || e.getMessage().contains("Private key reuse detect!") || e.getMessage().contains("Send to inputs!")) {
                    transferResult = "Sending to a used address/Private key reuse detect. Error Occurred.";
                }
                else if(e.getMessage().contains("Failed to connect to")){
                    transferResult = "Failed to connect to";
                }
                else{
                    System.out.println("=====E=====");
                    System.out.println(e);
                    transferResult = "Network Error: Attaching new address failed or Transaction failed";
                }
            }

            if (e instanceof IllegalAccessError) {
                transferResult = "Local POW needs to be enabled";
            }
        }
        if(sendTransferResponse != null){
            Boolean[] transferSuccess = sendTransferResponse.getSuccessfully();
            //Toast.makeText(getActivity(), "TransferSuccess: "+transferSuccess.toString(), Toast.LENGTH_SHORT).show();
            if(transferSuccess.toString().equals("true")){
                transferResult = "Sent";
                System.out.println("=====Sent=====");
            }
        }
        else{
            System.out.println("=====sendResposeNull=====");
        }
        System.out.println("=====return=====");
        return transferResult;
    }

}
