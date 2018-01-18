package com.flashwifi.wifip2p;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flashwifi.wifip2p.iotaFlashWrapper.IotaFlashBridge;
import com.flashwifi.wifip2p.iotaFlashWrapper.Main;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jota.utils.SeedRandomGenerator;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    public String convertStreamToString(InputStream p_is) throws IOException {
    /*
     * To convert the InputStream to String we use the
     * BufferedReader.readLine() method. We iterate until the BufferedReader
     * return null which means there's no more data to read. Each line will
     * appended to a StringBuilder and returned as String.
     */
        if (p_is != null) {
            StringBuilder m_sb = new StringBuilder();
            String m_line;
            try {
                BufferedReader m_reader = new BufferedReader(
                        new InputStreamReader(p_is));
                while ((m_line = m_reader.readLine()) != null) {
                    m_sb.append(m_line).append("\n");
                }
            } finally {
                p_is.close();
            }
            Log.e("TAG", m_sb.toString());
            return m_sb.toString();
        } else {
            return "";
        }
    }

    private String readFile(String name) {
        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier(name,
                        "raw", getPackageName()));
        try {
            return convertStreamToString(ins);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        ActivityCompat.requestPermissions(HomeActivity.this,
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                1);

        // iotalibflash
        String iotaflash = readFile("iotaflash");
        String iotaflashhelper = readFile("iotaflashhelper");

        /*try {
            IotaFlashBridge.boot(iotaflash, iotaflashhelper);
            Main.runExample();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                int i = 0;
                if (grantResults.length > 0) {
                    boolean ok = true;
                    for (int grantResult: grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            ok = false;
                            Log.d(TAG, "onRequestPermissionsResult: denied: " + permissions[i]);
                        }
                        i++;
                    }
                    if (ok) {
                        Toast.makeText(HomeActivity.this, "Permissions granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HomeActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
                    }
                    AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                    int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), getPackageName());
                    if (mode != AppOpsManager.MODE_ALLOWED) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                    }

                    int mode2 = appOps.checkOpNoThrow(AppOpsManager.OPSTR_WRITE_SETTINGS,
                            android.os.Process.myUid(), getPackageName());
                    if (mode2 != AppOpsManager.MODE_ALLOWED) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        startActivity(intent);
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(HomeActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void setProgressBar(int percentage) {
        ProgressBar prog = (ProgressBar) findViewById(R.id.progressbar);
        prog.setProgress(percentage);
    }

    private void showPasswordField() {
        // show the password field
        final EditText field = (EditText) findViewById(R.id.password);
        field.setVisibility(View.VISIBLE);

        // show the submit button
        Button button = (Button) findViewById(R.id.decryptTheSeedButton);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (field.getText().length() > 0) {
                    String password = field.getText().toString();
                    decryptSeed(password);
                }
            }
        });
    }

    private void startDesktop(String seed, String password) {
        // the security of this:
        // https://stackoverflow.com/questions/24141480/securi ty-of-sending-sensitive-intent-extras-within-my-own-app
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("password", password);
        startActivity(intent);

        // disable back navigation by calling finish
        // https://stackoverflow.com/questions/6376708/how-can-i-disable-go-back-to-some-activity
        finish();

    }

    private void decryptSeed(String password) {
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(password).build();
        String seed = encryptedPreferences.getString(getString(R.string.encrypted_seed), null);
        View view = findViewById(R.id.home_view);

        if (seed != null) {
            Snackbar.make(view, getString(R.string.seed_decrypted), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            setProgressBar(90);
            startDesktop(seed, password);
        } else {
            final EditText field = (EditText) findViewById(R.id.password);
            field.setText("");
            Snackbar.make(view, getString(R.string.wrong_password), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    private void showGenerateSeedButton() {
        Button button = (Button) findViewById(R.id.generateSeedButton);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                generateNewSeed();
            }
        });
    }

    private void generateNewSeed() {
        // generate the seed
        final String seed = SeedRandomGenerator.generateNewSeed();

        TextView seedText = (TextView) findViewById(R.id.seedTextView);
        seedText.setVisibility(View.VISIBLE);
        seedText.setText(seed);
        setProgressBar(25);

        // show password comment string
        TextView seedComment = (TextView) findViewById(R.id.seedComment);
        seedComment.setVisibility(View.VISIBLE);

        // show new password field
        final EditText new_password_field = (EditText) findViewById(R.id.new_password);
        new_password_field.setVisibility(View.VISIBLE);

        // show the button
        Button button = (Button) findViewById(R.id.new_password_button);
        button.setVisibility(View.VISIBLE);
        setProgressBar(35);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                setProgressBar(50);
                String password = new_password_field.getText().toString();
                storeNewSeed(seed, password);
            }
        });
    }

    private void storeNewSeed(String seed, String password) {
        // store the seed encrypted
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(password).build();
        encryptedPreferences.edit()
                .putString(getString(R.string.encrypted_seed), seed)
                .apply();
        setProgressBar(60);
        // store the status in plain mode
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.active_seed), true);
        editor.apply();
        // display a message to the user
        View view = findViewById(R.id.home_view);
        Snackbar.make(view, getString(R.string.seed_stored), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        setProgressBar(90);

        // start the next activity
        startDesktop(seed, password);
    }

    private void checkStorage() {
        setProgressBar(10);
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        setProgressBar(12);
        // how many times has the app been opened?
        int numberOfUsages = sharedPref.getInt(getString(R.string.number_of_usages), 0);
        setProgressBar(14);
        // update the number
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.number_of_usages), numberOfUsages + 1);
        editor.apply();

        // get the current view
        final View view = findViewById(R.id.home_view);

        if (numberOfUsages == 0) {
            // change the view to the tutorial and settings page
            // setProgressBar(90);
        }

        // is there an active seed
        boolean activeSeed = sharedPref.getBoolean(getString(R.string.active_seed), false);
        setProgressBar(16);

        if (activeSeed) {
            // show password field
            showPasswordField();
            setProgressBar(20);
        } else {
            Snackbar.make(view, getString(R.string.no_seed_present), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            setProgressBar(20);
            showGenerateSeedButton();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkStorage();
    }
}
