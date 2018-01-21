package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flashwifi.wifip2p.iotaAPI.Requests.WalletBalanceChecker;

import net.glxn.qrgen.android.QRCode;

import pl.droidsonroids.gif.GifImageView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FundWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FundWalletFragment extends Fragment {

    private static final int FUND_WALLET = 0;
    private static final int BALANCE_RETRIEVE_TASK_COMPLETE = 1;
    private String seed;
    private String depositAddress;
    private String balance;

    private TextView balanceTextView;
    private TextView addressTextView;
    private ImageView qrImageView;

    private GifImageView loadingGifImageView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Handler mHandler;

    private static Boolean transactionInProgress = false;
    private WalletBalanceChecker addressAndBalanceChecker;

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
                    case BALANCE_RETRIEVE_TASK_COMPLETE:
                        AddressBalanceTransfer addressBalanceTransfer = (AddressBalanceTransfer) inputMessage.obj;
                        balance = addressBalanceTransfer.getBalance();
                        depositAddress = addressBalanceTransfer.getDepositAddress();
                        String returnStatus = addressBalanceTransfer.getMessage();

                        hideLoadingGIF();
                        transactionInProgress = false;

                        if(returnStatus.equals("noError")){
                            balanceTextView.setText(balance + " i");
                            addressTextView.setText(depositAddress);
                            createAddressQRCode(depositAddress);
                            makeToastFundWalletFragment("Balance and address updated");
                        }
                        else if (returnStatus.equals("hostError")){
                            makeToastFundWalletFragment("Unable to reach host (node)");
                        }
                        else if (returnStatus.equals("addressError")){
                            makeToastFundWalletFragment("Error getting address");
                        }
                        else if (returnStatus.equals("balanceError")){
                            makeToastFundWalletFragment("Error getting balance. May not be able to resolve host/node");
                        }
                        else{
                            makeToastFundWalletFragment("Unknown error");
                        }
                        break;
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

    private void hideLoadingGIF() {
        loadingGifImageView.setVisibility(View.GONE);
        qrImageView.setVisibility(View.VISIBLE);
    }

    private void showLoadingGIF() {
        loadingGifImageView.setVisibility(View.VISIBLE);
        qrImageView.setVisibility(View.GONE);
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
        loadingGifImageView = (GifImageView) fundWalletFragmentView.findViewById(R.id.FundWalletLoadingGIF);
        mSwipeRefreshLayout = (SwipeRefreshLayout) fundWalletFragmentView.findViewById(R.id.FundWalletSwipeRefresh);

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

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });

        showLoadingGIF();
        Toast.makeText(getActivity(), "Retrieving balance and address...", Toast.LENGTH_SHORT).show();
        getBalance();

        return fundWalletFragmentView;
    }

    private void initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);

        if(transactionInProgress == false){
            balanceTextView.setText("");
            addressTextView.setText("");
            showLoadingGIF();
            Toast.makeText(getActivity(), "Retrieving balance and address...", Toast.LENGTH_SHORT).show();
            getBalance();
        }
    }

    private void getBalance(){
        transactionInProgress = true;
        addressAndBalanceChecker = new WalletBalanceChecker(getActivity(),getActivity().getString(R.string.preference_file_key),seed, mHandler,FUND_WALLET,true);
        addressAndBalanceChecker.execute();
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