package com.flashwifi.wifip2p;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;

/**
 * Created by Toby on 1/16/2018.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    String password = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        //Set change listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key){
            case "pref_key_security":
                Toast.makeText(getActivity(), "Security Changed", Toast.LENGTH_SHORT).show();
                break;
            case "pref_key_network_timeout":
                Toast.makeText(getActivity(), "Network Timeout Changed", Toast.LENGTH_SHORT).show();
                break;
            case "pref_key_units":
                Toast.makeText(getActivity(), "Units Changed", Toast.LENGTH_SHORT).show();
                break;
            case "pref_key_switch_testnet":
                Toast.makeText(getActivity(), "Testnet on/off Changed", Toast.LENGTH_SHORT).show();
                break;
            case "edit_text_sell_price":
                Toast.makeText(getActivity(), "Hotspot Sell Price Changed", Toast.LENGTH_SHORT).show();
                break;
            case "edit_text_sell_min_minutes":
                Toast.makeText(getActivity(), "Hotspot Min sell duration changed", Toast.LENGTH_SHORT).show();
                break;
            case "edit_text_sell_max_minutes":
                Toast.makeText(getActivity(), "Hotspot Max sell duration changed", Toast.LENGTH_SHORT).show();
                break;
            case "edit_text_buy_price":
                Toast.makeText(getActivity(), "Buy Price Changed", Toast.LENGTH_SHORT).show();
                break;
            case "edit_text_client_minutes":
                Toast.makeText(getActivity(), "Client duration duration changed", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "pref_key_reset_data_usage":
                Toast.makeText(getActivity(), "Reset Data Usage", Toast.LENGTH_SHORT).show();
                break;
            case "pref_key_testnet_fund_add":
                Toast.makeText(getActivity(), "Testnet fund add", Toast.LENGTH_SHORT).show();
                break;
            case "pref_key_reset_password":
                Toast.makeText(getActivity(), "Reset Password", Toast.LENGTH_SHORT).show();
                askPassword();
                break;
            case "pref_key_reset_wallet":
                Toast.makeText(getActivity(), "Reset Wallet", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void askPassword() {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter current password");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = input.getText().toString();
                updatePassword();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                password = "";
            }
        });
        builder.show();
    }

    public void updatePassword(){

        if(password.equals("")){
            //Blank Password
            Toast.makeText(getActivity(), "Current password cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = getActivity();
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(context).withEncryptionPassword(password).build();
        String seed = encryptedPreferences.getString(getString(R.string.encrypted_seed), null);

        if (seed != null && context != null) {
            //Correct password, re-store seed with new input password
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Enter new password");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newPassword = input.getText().toString();
                    if(!newPassword.equals("")){
                        EncryptedPreferences encryptedPreferencesRemove = new EncryptedPreferences.Builder(context).withEncryptionPassword(password).build();
                        encryptedPreferencesRemove.edit().remove(getString(R.string.encrypted_seed)).apply();

                        EncryptedPreferences encryptedPreferencesUpdated = new EncryptedPreferences.Builder(context).withEncryptionPassword(newPassword).build();
                        encryptedPreferencesUpdated.edit().putString(getString(R.string.encrypted_seed), seed).apply();
                        password = "";
                        Toast.makeText(getActivity(), "Password changed.", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        password = "";
                        Toast.makeText(getActivity(), "New password cannot be blank.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    password = "";
                }
            });
            builder.show();
        }
        else{
            //Wrong Password
            Toast.makeText(getActivity(), "Wrong Password. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

}
