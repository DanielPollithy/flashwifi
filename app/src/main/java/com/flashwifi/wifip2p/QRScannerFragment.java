package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
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

        if(getActivity() != null){
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            scannerView = new ZXingScannerView(getActivity());

            ViewGroup contentFrame = (ViewGroup) getActivity().findViewById(R.id.content_frame);
            if(contentFrame != null && scannerView != null){
                contentFrame.addView(scannerView);
            }
        }
    }

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
        scannerView.stopCameraPreview();
    }

    public void onDestroy() {
        scannerView.stopCamera();
        scannerView.stopCameraPreview();
        super.onDestroy();
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
