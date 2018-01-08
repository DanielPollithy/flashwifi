package com.flashwifi.wifip2p;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.EditText;

/**
 * Created by Toby on 1/7/2018.
 */

public class AddressEditTextTransfer implements Parcelable {

    EditText editTextAddress;

    public EditText getEditTextAddress() {
        return editTextAddress;
    }

    public void setEditTextAddress(EditText editTextAddress) {
        this.editTextAddress = editTextAddress;
    }

    protected AddressEditTextTransfer() {
    }

    protected AddressEditTextTransfer(Parcel in) {
    }

    public static final Creator<AddressEditTextTransfer> CREATOR = new Creator<AddressEditTextTransfer>() {
        @Override
        public AddressEditTextTransfer createFromParcel(Parcel in) {
            return new AddressEditTextTransfer(in);
        }

        @Override
        public AddressEditTextTransfer[] newArray(int size) {
            return new AddressEditTextTransfer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
