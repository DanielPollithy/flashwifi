package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.glxn.qrgen.android.QRCode;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FundWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FundWalletFragment extends Fragment {

    private static final int TASK_COMPLETE = 1;
    private String seed;
    private String address;
    private String balance;

    TextView balanceTextView = null;
    TextView addressTextView = null;
    ImageView qrImageView = null;

    Handler mHandler;

    public FundWalletFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FundWalletFragment.
     */
    public static FundWalletFragment newInstance() {
        FundWalletFragment fragment = new FundWalletFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            seed = bundle.getString("seed");
        }

        //Handle post-asynctask activities of updating UI
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case TASK_COMPLETE:
                        String returnStatus = (String) inputMessage.obj;

                        if(returnStatus == "noError"){
                            balanceTextView.setText(balance + " i");
                            addressTextView.setText(address);
                            createAddressQRCode(address);
                            Toast.makeText(getActivity(), "Balance and address updated",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else if (returnStatus == "addressError"){
                            Toast.makeText(getActivity(), "Error getting address",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else if (returnStatus == "balanceError"){
                            Toast.makeText(getActivity(), "Error getting balance",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getActivity(), "Unknown error",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };
    }

    private void createAddressQRCode(String address) {
        Bitmap myBitmap = QRCode.from(address).bitmap();
        qrImageView.setImageBitmap(myBitmap);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View FundWalletFragmentView = inflater.inflate(R.layout.fragment_fund_wallet, container, false);

        balanceTextView = (TextView) FundWalletFragmentView.findViewById(R.id.FundWalletBalanceValue);
        addressTextView = (TextView) FundWalletFragmentView.findViewById(R.id.AddressValue);
        qrImageView = (ImageView) FundWalletFragmentView.findViewById(R.id.QRCode);

        // Set Listeners
        balanceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textCopyBalanceClick(balanceTextView);
            }
        });

        addressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textCopyAddressClick(addressTextView);
            }
        });

        Toast.makeText(getActivity(), "Retrieving balance and address...",
                Toast.LENGTH_SHORT).show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                WalletAddressAndBalanceChecker addressAndBalanceCheckerbalanceChecker = new WalletAddressAndBalanceChecker();
                List<String> addressList = addressAndBalanceCheckerbalanceChecker.getAddress(seed);
                if(addressList != null){
                    address = addressList.get(0);

                    balance = addressAndBalanceCheckerbalanceChecker.getBalance(addressList);
                    if(balance != null){
                        Message completeMessage = mHandler.obtainMessage(TASK_COMPLETE, "noError");
                        completeMessage.sendToTarget();
                    }
                    else{
                        //Balance Retrieval Error
                        Message completeMessage = mHandler.obtainMessage(TASK_COMPLETE, "balanceError");
                        completeMessage.sendToTarget();
                    }
                }
                else{
                    //Address Retrieval Error
                    Message completeMessage = mHandler.obtainMessage(TASK_COMPLETE, "addressError");
                    completeMessage.sendToTarget();
                }
            }
        });
        return FundWalletFragmentView;
    }

    public void textCopyBalanceClick(TextView balanceTextView)
    {
        String balanceValue = balanceTextView.getText().toString();
        setClipboardText(balanceValue);
        Toast.makeText(getActivity(), "Balance Copied",
                Toast.LENGTH_SHORT).show();
    }

    public void textCopyAddressClick(TextView addressTextView)
    {
        String addressValue = addressTextView.getText().toString();
        setClipboardText(addressValue);
        Toast.makeText(getActivity(), "Address Copied",
                Toast.LENGTH_SHORT).show();
    }

    public void setClipboardText(String inText){
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", inText);
        clipboard.setPrimaryClip(clip);
    }
}