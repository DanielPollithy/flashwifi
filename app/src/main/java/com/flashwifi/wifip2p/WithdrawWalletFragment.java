package com.flashwifi.wifip2p;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import static android.Manifest.permission.CAMERA;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WithdrawWalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WithdrawWalletFragment extends Fragment {

    private String seed;
    ImageButton qrScannerButton;
    EditText editTextAddress;

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

        if(savedInstanceState != null){
            QRCodeResult qrResult = savedInstanceState.getParcelable("result");
            if(qrResult != null){
                String address = qrResult.getAddress();
                Toast.makeText(getActivity(), "Address: "+address, Toast.LENGTH_SHORT).show();
            }
        }

        Toast.makeText(getActivity(), "Make View", Toast.LENGTH_SHORT).show();

        View withdrawWalletFragmentView = inflater.inflate(R.layout.fragment_withdraw_wallet, container, false);

        qrScannerButton = (ImageButton) withdrawWalletFragmentView.findViewById(R.id.WithdrawWalletQRScannerButton);
        editTextAddress = (EditText) withdrawWalletFragmentView.findViewById(R.id.editTextAddress);

        // Set Listeners
        qrScannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScannerButtonClick();
            }
        });

        return withdrawWalletFragmentView;
    }

    private void qrScannerButtonClick() {
        if (!hasCameraPermission(getActivity())) {
            Toast.makeText(getActivity(), "Camera permission not granted", Toast.LENGTH_SHORT).show();
            /* TODO: Ask for permission. Currently depends on manifest for permission. Should ask at runtime */
        } else {
            launchQRScanner();
        }
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
                //.add(R.id.content_frame, fragment, null)
                .addToBackStack(null)
                .commit();
        //fragment.setRetainInstance(true);
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

}
