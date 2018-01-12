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

import com.flashwifi.wifip2p.iotaAPI.Requests.WalletAddressAndBalanceChecker;

import net.glxn.qrgen.android.QRCode;

import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FundWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FundWalletFragment extends Fragment {

    private static final int TASK_COMPLETE = 1;
    private String seed;
    private String depositAddress;
    private String balance;

    private TextView balanceTextView;
    private TextView addressTextView;
    private ImageView qrImageView;

    private Handler mHandler;

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
                            addressTextView.setText(depositAddress);
                            createAddressQRCode(depositAddress);
                            makeToastFundWalletFragment("Balance and address updated");
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
                }
            }
        };
    }

    private void makeToastFundWalletFragment(String s) {
        if(getActivity() != null){
            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
        }
    }

    private void createAddressQRCode(String address) {
        Bitmap myBitmap = QRCode.from(address).bitmap();
        qrImageView.setImageBitmap(myBitmap);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fundWalletFragmentView = inflater.inflate(R.layout.fragment_fund_wallet, container, false);

        balanceTextView = (TextView) fundWalletFragmentView.findViewById(R.id.FundWalletBalanceValue);
        addressTextView = (TextView) fundWalletFragmentView.findViewById(R.id.AddressValue);
        qrImageView = (ImageView) fundWalletFragmentView.findViewById(R.id.QRCode);

        // Set Listeners
        balanceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textCopyBalanceClick();
            }
        });

        addressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textCopyAddressClick();
            }
        });

        Toast.makeText(getActivity(), "Retrieving balance and address...",
                Toast.LENGTH_SHORT).show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                WalletAddressAndBalanceChecker addressAndBalanceChecker = new WalletAddressAndBalanceChecker(getActivity(),getActivity().getString(R.string.preference_file_key));
                List<String> addressList = addressAndBalanceChecker.getAddress(seed);

                if(addressList != null && addressList.get(0) == "Unable to resolve host"){
                    Message completeMessage = mHandler.obtainMessage(TASK_COMPLETE, "hostError");
                    completeMessage.sendToTarget();
                }
                else if(addressList != null){

                    System.out.println("|AddressListReturned:|");
                    System.out.println(addressList.size());
                    System.out.println(addressList.get(addressList.size()-1));

                    depositAddress = addressList.get(addressList.size()-1);

                    balance = addressAndBalanceChecker.getBalance(addressList);
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
        return fundWalletFragmentView;
    }

    public void textCopyBalanceClick()
    {
        String balanceValue = balanceTextView.getText().toString();
        setClipboardText(balanceValue);
        Toast.makeText(getActivity(), "Balance Copied",
                Toast.LENGTH_SHORT).show();
    }

    public void textCopyAddressClick()
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