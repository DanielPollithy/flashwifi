package com.flashwifi.wifip2p;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flashwifi.wifip2p.iotaAPI.Requests.WalletAddressAndBalanceChecker;
import com.flashwifi.wifip2p.iotaAPI.Requests.WalletTransferRequest;

import java.util.List;
import jota.IotaAPI;

import jota.error.ArgumentException;
import jota.utils.Checksum;

import static android.Manifest.permission.CAMERA;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WithdrawWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 * Sources: https://github.com/iotaledger/android-wallet-app
 */
public class WithdrawWalletFragment extends Fragment {

    private static final int BALANCE_RETRIEVE_TASK_COMPLETE = 1;
    private static final int TRANSFER_TASK_COMPLETE = 2;
    private String appWalletSeed;
    private String appWalletBalance;
    private ImageButton qrScannerButton;
    private EditText editTextAddress;
    private EditText editTextAmount;
    private EditText editTextMessage;
    private EditText editTextTag;

    private Button sendButton;
    private TextView balanceTextView;
    private Handler mHandler;

    public WithdrawWalletFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WithdrawWalletFragment.
     */

    public static WithdrawWalletFragment newInstance() {
        WithdrawWalletFragment fragment = new WithdrawWalletFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            appWalletSeed = bundle.getString("seed");
        }

        //Handle post-asynctask activities of updating UI
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case BALANCE_RETRIEVE_TASK_COMPLETE:
                        String returnStatus = (String) inputMessage.obj;

                        if(returnStatus == "noError"){
                            balanceTextView.setText(appWalletBalance + " i");
                            makeToastFundWalletFragment("Balance updated");
                        }
                        else if (returnStatus == "noErrorNoUpdateMessage"){
                            balanceTextView.setText(appWalletBalance + " i");
                        }
                        else if (returnStatus == "hostError"){
                            makeToastFundWalletFragment("Unable to reach host (node)");
                        }
                        else if (returnStatus == "addressError"){
                            makeToastFundWalletFragment("Error getting address");
                        }
                        else if (returnStatus == "balanceError"){
                            makeToastFundWalletFragment("Error getting balance");
                        }
                        else{
                            makeToastFundWalletFragment("Unknown error");
                        }
                        break;

                    case TRANSFER_TASK_COMPLETE:
                        String transferStatus = (String) inputMessage.obj;
                        makeToastFundWalletFragment(transferStatus);
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View withdrawWalletFragmentView = inflater.inflate(R.layout.fragment_withdraw_wallet, container, false);

        sendButton = (Button) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletSend);
        qrScannerButton = (ImageButton) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletQRScannerButton);
        editTextAddress = (EditText) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletAddress);
        editTextAmount = (EditText) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletAmount);
        editTextMessage = (EditText) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletMessage);
        editTextTag = (EditText) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletTag);

        balanceTextView = (TextView) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletBalanceValue);

        // Set Listeners
        qrScannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScannerButtonClick();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButtonClick();
            }
        });

        Toast.makeText(getActivity(), "Retrieving balance...",
                Toast.LENGTH_SHORT).show();

        getBalance(true,false);
        return withdrawWalletFragmentView;
    }

    private void getBalance(Boolean async, Boolean inNoUpdateMessage) {

        final Boolean noUpdateMessageFinal = inNoUpdateMessage;

        if(async){
            //Get balance with new async call
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    WalletAddressAndBalanceChecker addressAndBalanceChecker = new WalletAddressAndBalanceChecker(getActivity(),getActivity().getString(R.string.preference_file_key));
                    List<String> addressList = addressAndBalanceChecker.getAddress(appWalletSeed);

                    if(addressList != null && addressList.get(0) == "Unable to resolve host"){
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "hostError");
                        completeMessage.sendToTarget();
                    }
                    else if(addressList != null){
                        appWalletBalance = addressAndBalanceChecker.getBalance(addressList);
                        if(appWalletBalance != null){
                            if(noUpdateMessageFinal){
                                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "noErrorNoUpdateMessage");
                                completeMessage.sendToTarget();
                            }
                            else{
                                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "noError");
                                completeMessage.sendToTarget();
                            }
                        }
                        else{
                            //Balance Retrieval Error
                            Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "balanceError");
                            completeMessage.sendToTarget();
                        }
                    }
                    else{
                        //Address Retrieval Error
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "addressError");
                        completeMessage.sendToTarget();
                    }
                }
            });
        }
        else{
            //Get balance without async call (perform on called thread)
            WalletAddressAndBalanceChecker addressAndBalanceChecker = new WalletAddressAndBalanceChecker(getActivity(),getActivity().getString(R.string.preference_file_key));
            List<String> addressList = addressAndBalanceChecker.getAddress(appWalletSeed);

            if(addressList != null && addressList.get(0) == "Unable to resolve host"){
                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "hostError");
                completeMessage.sendToTarget();
            }
            else if(addressList != null){
                appWalletBalance = addressAndBalanceChecker.getBalance(addressList);
                if(appWalletBalance != null){
                    if(noUpdateMessageFinal){
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "noErrorNoUpdateMessage");
                        completeMessage.sendToTarget();
                    }
                    else{
                        Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "noError");
                        completeMessage.sendToTarget();
                    }
                }
                else{
                    //Balance Retrieval Error
                    Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "balanceError");
                    completeMessage.sendToTarget();
                }
            }
            else{
                //Address Retrieval Error
                Message completeMessage = mHandler.obtainMessage(BALANCE_RETRIEVE_TASK_COMPLETE, "addressError");
                completeMessage.sendToTarget();
            }
        }
    }

    private void makeToastFundWalletFragment(String s) {
        if(getActivity() != null){
            System.out.println("makeToastFundWalletFragment: "+s);
            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
        }
    }

    private void qrScannerButtonClick() {
        if (!hasCameraPermission(getActivity())) {
            Toast.makeText(getActivity(), "Camera permission not granted", Toast.LENGTH_SHORT).show();
            /* TODO: Ask for permission. Currently depends on manifest for permission. Should ask at runtime */
        } else {
            launchQRScanner();
        }
    }

    private void sendButtonClick() {

        final String sendAddress = editTextAddress.getText().toString();
        final String sendAmount = editTextAmount.getText().toString();
        final String message = editTextMessage.getText().toString();
        final String tag = editTextTag.getText().toString();

        if(appWalletBalance == null || appWalletBalance.isEmpty()){
            Toast.makeText(getActivity(), "Please wait for balance to be retrieved", Toast.LENGTH_SHORT).show();
        }
        else if(isValidAddress() == false){
            Toast.makeText(getActivity(), "Please enter a valid recipient address", Toast.LENGTH_SHORT).show();
        }
        else if (sendAmount.isEmpty() == false  && (Long.parseLong(appWalletBalance) < Long.parseLong(editTextAmount.getText().toString()))) {
            Toast.makeText(getActivity(), "Not enough credit in wallet", Toast.LENGTH_SHORT).show();
        }
        else{
            if (sendAmount.isEmpty() || sendAmount.equals("0")) {
                Toast.makeText(getActivity(), "0 send amount will be used", Toast.LENGTH_SHORT).show();
            }
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setMessage("Are you sure you want to send the transfer?")
                    .setCancelable(false)
                    .setPositiveButton("OK",null)
                    .setNegativeButton("Cancel",null)
                    .create();

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Send transfer
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    String result;
                                    if(sendAmount.isEmpty()){
                                        String zeroSendAmount = "0";
                                        WalletTransferRequest transferRequest = new WalletTransferRequest(sendAddress,appWalletSeed,zeroSendAmount,message,tag,getActivity());
                                        result = transferRequest.sendRequest();
                                        System.out.println("sendButtonClick: "+result);
                                    }
                                    else{
                                        WalletTransferRequest transferRequest = new WalletTransferRequest(sendAddress,appWalletSeed,sendAmount,message,tag,getActivity());
                                        result = transferRequest.sendRequest();
                                        System.out.println("sendButtonClick: "+result);
                                    }
                                    getBalance(false,true);
                                    Message completeMessage = mHandler.obtainMessage(TRANSFER_TASK_COMPLETE, result);
                                    completeMessage.sendToTarget();
                                }
                            });
                            //TODO: Empty all fields
                        }
            });
            alertDialog.show();
        }
    }

    public static boolean hasCameraPermission(Context context) {
        int result = context.checkCallingOrSelfPermission(CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void launchQRScanner() {
        /*
        TODO:Uncomment to re-enable scanner
        Fragment fragment = new QRScannerFragment();
        Bundle bundle = new Bundle();

        AddressEditTextTransfer addressEditText = new AddressEditTextTransfer();
        addressEditText.setEditTextAddress(editTextAddress);

        bundle.putParcelable("addressEditTextRef",addressEditText);
        fragment.setArguments(bundle);

        getActivity().getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(fragment, null)
                //.add(R.id.content_frame, fragment, null)
                .addToBackStack(null)
                .commit();
        //fragment.setRetainInstance(true);
        */

        editTextAddress.setText("ULRSUDTQLEQLXUMXEOWPEUIHRZUJFPUZRHVBZC9XYIVZJWGFOJNLDHQNQAZPPVOSTVBUT9T9EJRNMGGO9");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchQRScanner();
                } else {
                    Toast.makeText(getActivity(), "Permission denied to use Camera", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private boolean isValidAddress() {
        String address = editTextAddress.getText().toString();
        try {
            //noinspection StatementWithEmptyBody
            if (Checksum.isAddressWithoutChecksum(address)) {
            }
        } catch (ArgumentException e) {
            return false;
        }
        return true;
    }

}
