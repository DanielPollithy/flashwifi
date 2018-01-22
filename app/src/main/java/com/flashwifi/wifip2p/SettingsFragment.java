package com.flashwifi.wifip2p;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.flashwifi.wifip2p.iotaAPI.Requests.WalletTestnetTokenGen;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String password = "";
    private String seed = "";

    private static final int TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE = 0;
    private static final int TOKEN_TESTNET_STATUS_UPDATE = 1;

    private Handler mHandler;
    private Preference testnetFundAddPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            seed = bundle.getString("seed");
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        //Set change listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        //Handle post-asynctask activities of updating UI
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                String result = (String) inputMessage.obj;
                switch (inputMessage.what) {
                    case TOKEN_TESTNET_STATUS_UPDATE:
                        if(result.equals("Sending")){
                            testnetFundAddPref.setSummary("Sending...");
                        }
                        break;
                    case TOKEN_TESTNET_RETRIEVE_TASK_COMPLETE:
                        if(result.equals("Sent")){
                            makeToastSettingsFragment("2000i generated and added to testnet wallet. Check balance.");
                            testnetFundAddPref.setSummary("2000i added");
                        }
                        else{
                            makeToastSettingsFragment(result);
                            testnetFundAddPref.setSummary(result);
                        }
                        break;
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if(testnetFundAddPref != null){
            testnetFundAddPref.setSummary("");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key){
            case "pref_key_security":
                makeToastSettingsFragment("Security Changed");
                break;
            case "pref_key_network_timeout":
                makeToastSettingsFragment("Network Timeout Changed");
                break;
            case "pref_key_units":
                makeToastSettingsFragment("Units Changed");
                break;
            case "pref_key_switch_testnet":
                makeToastSettingsFragment("Testnet on/off Changed");
                break;
            case "edit_text_sell_price":
                makeToastSettingsFragment("Hotspot Sell Price Changed");
                break;
            case "edit_text_sell_min_minutes":
                makeToastSettingsFragment("Hotspot Min sell duration changed");
                break;
            case "edit_text_sell_max_minutes":
                makeToastSettingsFragment("Hotspot Max sell duration changed");
                break;
            case "edit_text_buy_price":
                makeToastSettingsFragment("Buy Price Changed");
                break;
            case "edit_text_client_minutes":
                makeToastSettingsFragment("Client duration duration changed");
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "pref_key_reset_data_usage":
                makeToastSettingsFragment( "Reset Data Usage");
                break;
            case "pref_key_testnet_fund_add":
                SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Boolean testnet = prefManager.getBoolean("pref_key_switch_testnet",false);

                if(testnet){
                    testnetFundAddPref = preference;
                    testnetFundAddPref.setSummary("Generating...");
                    makeToastSettingsFragment("Testnet wallet token generation request sent.");
                    WalletTestnetTokenGen tokenGen = new WalletTestnetTokenGen(mHandler, getActivity(), getString(R.string.preference_file_key), seed);
                    tokenGen.execute();
                }
                else{
                    makeToastSettingsFragment("Please enable testnet first.");
                }
                break;
            case "pref_key_reset_password":
                makeToastSettingsFragment( "Reset Password");
                askPassword();
                break;
            case "pref_key_reset_wallet":
                makeToastSettingsFragment("Reset Wallet.");
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
            makeToastSettingsFragment("Current password cannot be blank.");
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
                        makeToastSettingsFragment("Password changed.");
                    }
                    else{
                        password = "";
                        makeToastSettingsFragment("New password cannot be blank.");
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
            makeToastSettingsFragment("Wrong Password. Please try again.");
        }
    }

    private void makeToastSettingsFragment(String s) {
        if(getActivity() != null){
            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
        }
    }

}
