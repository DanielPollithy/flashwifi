package com.flashwifi.wifip2p;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;

import jota.utils.SeedRandomGenerator;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
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
                String password = field.getText().toString();
                decryptSeed(password);
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
