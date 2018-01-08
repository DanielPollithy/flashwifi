package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRScannerFragment extends Fragment implements ZXingScannerView.ResultHandler {

    private ZXingScannerView scannerView;
    private EditText editTextAddress;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            AddressEditTextTransfer addressEditTextTransfer = bundle.getParcelable("addressEditTextRef");
            editTextAddress = addressEditTextTransfer.getEditTextAddress();
        }

        scannerView = new ZXingScannerView(getActivity());

        ViewGroup contentFrame = (ViewGroup) getActivity().findViewById(R.id.content_frame);
        contentFrame.addView(scannerView);

        //getActivity().setContentView(scannerView);
    }

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        scannerView = new ZXingScannerView(getActivity());
        temp = getActivity().findViewById(android.R.id.content);
        getActivity().setContentView(scannerView);
        return scannerView;
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        String resultText = result.getText();

        //Edit via reference (shallow copy)
        editTextAddress.setText(resultText);

        ViewGroup contentFrame = (ViewGroup) getActivity().findViewById(R.id.content_frame);
        contentFrame.removeView(scannerView);
    }
}
