package com.flashwifi.wifip2p;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FundWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FundWalletFragment extends Fragment {

    private String seed;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fund_wallet, container, false);
    }
}