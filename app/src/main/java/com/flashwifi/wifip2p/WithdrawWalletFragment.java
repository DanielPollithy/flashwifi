package com.flashwifi.wifip2p;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flashwifi.wifip2p.iotaAPI.Requests.WalletBalanceChecker;
import com.flashwifi.wifip2p.iotaAPI.Requests.WalletTransferRequest;

import jota.error.ArgumentException;
import jota.utils.Checksum;
import pl.droidsonroids.gif.GifImageView;

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
    private static final int WITHDRAW_WALLET = 1;
    private String appWalletSeed;
    private String appWalletBalance;
    private ImageButton qrScannerButton;
    private EditText editTextAddress;
    private EditText editTextAmount;
    private EditText editTextMessage;
    private EditText editTextTag;
    private WalletBalanceChecker addressAndBalanceChecker;

    private GifImageView loadingGifImageView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Button sendButton;
    private TextView balanceTextView;
    private Handler mHandler;

    private static Boolean transactionInProgress = false;

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
                AddressBalanceTransfer addressBalanceTransfer = (AddressBalanceTransfer) inputMessage.obj;
                switch (inputMessage.what) {
                    case BALANCE_RETRIEVE_TASK_COMPLETE:
                        appWalletBalance = addressBalanceTransfer.getBalance();
                        String returnStatus = addressBalanceTransfer.getMessage();
                        hideLoadingGIF();
                        transactionInProgress = false;

                        if(returnStatus.equals("noError")){
                            balanceTextView.setText(appWalletBalance);
                            makeToastFundWalletFragment("Balance updated");
                        }
                        else if (returnStatus.equals("noErrorNoUpdateMessage")){
                            balanceTextView.setText(appWalletBalance);
                            clearAllTransferValues();
                            makeFieldsEditable();
                        }
                        else if (returnStatus.equals("hostError")){
                            makeToastFundWalletFragment("Unable to reach host (node)");
                        }
                        else if (returnStatus.equals("addressError")){
                            makeToastFundWalletFragment("Error getting address");
                        }
                        else if (returnStatus.equals("balanceError")){
                            makeToastFundWalletFragment("Error getting balance");
                        }
                        else{
                            makeToastFundWalletFragment("Unknown error");
                        }
                        break;

                    case TRANSFER_TASK_COMPLETE:
                        String transferStatus = addressBalanceTransfer.getMessage();
                        makeToastFundWalletFragment(transferStatus);
                        getBalance(false);
                        Toast.makeText(getActivity(), "Updating balance...", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        addressAndBalanceChecker.cancel(true);
        transactionInProgress = false;
        super.onDestroy();
    }

    private void clearAllTransferValues() {
        editTextAddress.setText("");
        editTextAmount.setText("");
        editTextMessage.setText("");
        editTextTag.setText("");
    }

    private void hideLoadingGIF() {
        loadingGifImageView.setVisibility(View.GONE);
    }

    private void showLoadingGIF() {
        loadingGifImageView.setVisibility(View.VISIBLE);
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
        loadingGifImageView = (GifImageView) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletLoadingGIF);
        balanceTextView = (TextView) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletBalanceValue);
        mSwipeRefreshLayout = (SwipeRefreshLayout) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletSwipeRefresh);

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

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });

        showLoadingGIF();
        Toast.makeText(getActivity(), "Retrieving balance...",
                Toast.LENGTH_SHORT).show();
        
        getBalance(true);
        return withdrawWalletFragmentView;
    }

    private void getBalance(Boolean updateMessage) {
        transactionInProgress = true;
        addressAndBalanceChecker = new WalletBalanceChecker(getActivity(),getActivity().getString(R.string.preference_file_key),appWalletSeed, mHandler,WITHDRAW_WALLET,updateMessage);
        addressAndBalanceChecker.execute();
    }

    private void initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);

        if(transactionInProgress == false){
            balanceTextView.setText("");
            showLoadingGIF();
            Toast.makeText(getActivity(), "Retrieving balance...", Toast.LENGTH_SHORT).show();
            getBalance(true);
        }
    }

    private void makeToastFundWalletFragment(String s) {
        if(getActivity() != null){
            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
        }
    }

    private void qrScannerButtonClick() {
        if(transactionInProgress == false) {
            if (!hasCameraPermission(getActivity())) {
                Toast.makeText(getActivity(), "Camera permission not granted", Toast.LENGTH_SHORT).show();
            /* TODO: Ask for permission. Currently depends on manifest for permission. Should ask at runtime */
            } else {
                launchQRScanner();
            }
        }
        else{
            Toast.makeText(getActivity(), "Please wait for balance to be retrieved", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendButtonClick() {

        final String sendAddress = editTextAddress.getText().toString();
        final String sendAmount = editTextAmount.getText().toString();
        final String message = editTextMessage.getText().toString();
        final String tag = editTextTag.getText().toString();

        if(appWalletBalance == null || appWalletBalance.isEmpty() || transactionInProgress){
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
                            showLoadingGIF();
                            makeFieldsNonEditable();
                            Toast.makeText(getActivity(), "Sending... (It is assumed that the balance is up to date)", Toast.LENGTH_SHORT).show();
                            //Send transfer
                            sendTransfer(sendAddress,sendAmount,message,tag);
                        }
            });
            alertDialog.show();
        }
    }

    private void sendTransfer(String sendAddress, String sendAmount, String message, String tag) {
        String finalSendAmount;
        if(sendAmount.isEmpty()){
            finalSendAmount = "0";
        }else{
            finalSendAmount = sendAmount;
        }
        WalletTransferRequest transferRequest = new WalletTransferRequest(sendAddress,appWalletSeed,finalSendAmount,message,tag,getActivity(),mHandler);
        transactionInProgress = true;
        transferRequest.execute();
    }

    private void makeFieldsNonEditable() {
        editTextAddress.setEnabled(false);
        editTextAmount.setEnabled(false);
        editTextMessage.setEnabled(false);
        editTextTag.setEnabled(false);
    }

    private void makeFieldsEditable() {
        editTextAddress.setText("");
        editTextAmount.setText("");
        editTextMessage.setText("");
        editTextTag.setText("");

        editTextAddress.setEnabled(true);
        editTextAmount.setEnabled(true);
        editTextMessage.setEnabled(true);
        editTextTag.setEnabled(true);
    }

    public static boolean hasCameraPermission(Context context) {
        int result = context.checkCallingOrSelfPermission(CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void launchQRScanner() {
        Fragment fragment = new QRScannerFragment();
        Bundle bundle = new Bundle();

        AddressEditTextTransfer addressEditText = new AddressEditTextTransfer();
        addressEditText.setEditTextAddress(editTextAddress);

        bundle.putParcelable("addressEditTextRef",addressEditText);
        fragment.setArguments(bundle);

        getActivity().getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(fragment, null)
                .addToBackStack(null)
                .commit();
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
