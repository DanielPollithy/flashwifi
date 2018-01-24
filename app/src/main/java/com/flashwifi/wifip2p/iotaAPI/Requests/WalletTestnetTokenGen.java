package com.flashwifi.wifip2p.iotaAPI.Requests;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import com.flashwifi.wifip2p.AddressBalanceTransfer;
import com.flashwifi.wifip2p.R;
import com.flashwifi.wifip2p.iotaAPI.Requests.Model.TokenGenJSONResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class WalletTestnetTokenGen extends AsyncTask<Void, Void, Void> {

    private static final int TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE = 0;
    private static final int TOKEN_TESTNET_STATUS_UPDATE = 1;
    private static final int TRANSFER_TASK_COMPLETE = 2;
    private static String seed;
    private Handler settingsFragmentHandler;
    private Handler testnetTokenGenHandler;
    private String prefFile;
    static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static JsonFactory JSON_FACTORY = new JacksonFactory();
    private static Context context;

    public WalletTestnetTokenGen(Handler inMHandler, Context inContext, String inPrefFile, String inSeed){
        settingsFragmentHandler = inMHandler;
        context = inContext;
        prefFile = inPrefFile;
        seed = inSeed;

        //Handle post-asynctask activities of updating UI
        testnetTokenGenHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case TRANSFER_TASK_COMPLETE:
                        AddressBalanceTransfer addressBalanceTransfer = (AddressBalanceTransfer) inputMessage.obj;
                        String message = addressBalanceTransfer.getMessage();
                        //Send completion of send funds to settings fragment
                        Message completeMessage = settingsFragmentHandler.obtainMessage(TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE,message);
                        completeMessage.sendToTarget();
                }
            }
        };
    }

    public TokenGenJSONResponse genToken() {

        HttpRequestFactory requestFactory
                = HTTP_TRANSPORT.createRequestFactory(
                (HttpRequest request) -> {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                });
        HttpRequest request = null;
        try {

            SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean testnetPrivate = prefManager.getBoolean("pref_key_switch_testnet_private",false);

            if(testnetPrivate){
                request = requestFactory.buildGetRequest(new GenericUrl(context.getResources().getString(R.string.generatorPrivateTestnetNode)));
                //Set timeout to 10 min
                request.setConnectTimeout(300000);
                request.setReadTimeout(600000);
            }else{
                request = requestFactory.buildGetRequest(new GenericUrl(context.getResources().getString(R.string.generatorPublicTestnetNode)));
                //Set timeout to 10 min
                request.setConnectTimeout(300000);
                request.setReadTimeout(600000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: Set timeout
        /*
        ExponentialBackOff backoff = new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(300000)
                .setMaxElapsedTimeMillis(900000)
                .setMaxIntervalMillis(600000)
                .setMultiplier(1.5)
                .setRandomizationFactor(0.999)
                .build();
        request.setUnsuccessfulResponseHandler(
                new HttpBackOffUnsuccessfulResponseHandler(backoff));
                */

        Type type = new TypeToken<TokenGenJSONResponse>() {}.getType();
        TokenGenJSONResponse token = new TokenGenJSONResponse();
        try{
            token = (TokenGenJSONResponse) request
                    .execute()
                    .parseAs(type);
            token.setSuccess("Success");
        }
        catch (Exception e){
            if(e.toString().contains("Connection closed by peer")){
                //Could not reach token gen page
                token.setSuccess("Connection Error");
                e.printStackTrace();
            }
            else if(e.toString().contains("Unable to resolve host")){
                //Could not reach token gen page
                token.setSuccess("Unable to resolve host. Please try again.");
                e.printStackTrace();
            }
            else{
                //Unknown error
                token.setSuccess("Unknown Error. Please try again.");
                e.printStackTrace();
            }
        }
        return token;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        TokenGenJSONResponse token = genToken();
        if(token.getSuccess().equals("Success")){
            WalletAddressChecker addressChecker = new WalletAddressChecker(context,prefFile);
            List<String> destAddressses = addressChecker.getAddress(seed);
            transferToWallet(destAddressses, token);
        }
        else{
            Message completeMessage = settingsFragmentHandler.obtainMessage(TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE,token.getSuccess());
            completeMessage.sendToTarget();
        }
        return null;
    }

    private void transferToWallet(List<String> destAddressses, TokenGenJSONResponse token) {

        if(destAddressses != null && destAddressses.get(0).equals("Unable to resolve host")){
            //Host Error
            Message completeMessage = settingsFragmentHandler.obtainMessage(TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE, "hostError");
            completeMessage.sendToTarget();
        }
        else if(destAddressses != null){
            //No Address Retrieval Error
            Message completeMessage = settingsFragmentHandler.obtainMessage(TOKEN_TESTNET_STATUS_UPDATE, "Sending");
            completeMessage.sendToTarget();
            String destAddress = destAddressses.get(destAddressses.size()-1);

            WalletTransferRequest transferRequest = new WalletTransferRequest(destAddress,token.getSeed(),token.getAmount().toString(),"","",context,testnetTokenGenHandler);
            transferRequest.execute();
        }
        else{
            //Address Retrieval Error
            Message completeMessage = settingsFragmentHandler.obtainMessage(TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE,"addressError");
            completeMessage.sendToTarget();
        }
    }
}