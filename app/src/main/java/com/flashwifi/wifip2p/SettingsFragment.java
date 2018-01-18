package com.flashwifi.wifip2p;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Toby on 1/16/2018.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }


}
