package com.flashwifi.wifip2p;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DesktopActivity extends AppCompatActivity {

    private String password;
    private String seed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_desktop);

        // get the secrets from the login screen
        Intent intent = getIntent();
        password = intent.getStringExtra("password");
        seed = intent.getStringExtra("seed");
    }
}
