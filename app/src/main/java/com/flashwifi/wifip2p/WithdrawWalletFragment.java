package com.flashwifi.wifip2p;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WithdrawWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WithdrawWalletFragment extends Fragment {

    private String seed;

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
            seed = bundle.getString("seed");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_withdraw_wallet, container, false);
    }
}
