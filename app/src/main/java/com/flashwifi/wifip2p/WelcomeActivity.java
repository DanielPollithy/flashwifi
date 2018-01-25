package com.flashwifi.wifip2p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;

import jota.utils.SeedRandomGenerator;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private int[] layouts;
    private Button btnNext;
    private EditText passwordInput;
    private PrefManager prefManager;
    private TextView seedView;

    private String seed, password;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Checking for first time launch - before calling setContentView()
        prefManager = new PrefManager(this);

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_welcome);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        btnNext = (Button) findViewById(R.id.btn_next);

        if (hasSeed()) {
            layouts = new int[]{
                    R.layout.login_screen,
                    R.layout.welcome_screen3
            };

            btnNext.setVisibility(View.GONE);
            btnNext.setText("LOGIN");

        } else {

            layouts = new int[]{
                    R.layout.welcome_screen1,
                    R.layout.welcome_screen2,
                    R.layout.welcome_screen3,
                    R.layout.welcome_screen4,
                    R.layout.welcome_screen5,
                    R.layout.welcome_screen6
            };
        }

        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        viewPager.beginFakeDrag();

        boolean firstTimeNoSeed = hasSeed();

        btnNext.setOnClickListener(v -> {
            // checking for last page
            // if last page home screen will be launched
            int current = getItem(+1);
            if(firstTimeNoSeed && current == 2 && !btnNext.getText().equals("DONE")) {
                requestPermissions();
            } else if (firstTimeNoSeed && current == 1) {
                EditText pw = (EditText) findViewById(R.id.password);
                String pass = pw.getText().toString();
                if(decryptSeed(pass)) {
                    viewPager.setCurrentItem(current);
                }
            } else if(current == 3 && !btnNext.getText().equals("DONE")) {
                requestPermissions();
            } else if (current < layouts.length) {
                // move to next screen
                viewPager.setCurrentItem(current);
            } else if(!firstTimeNoSeed) {
                launchHomeScreen();
            } else {
                launchHomeScreenNormal();
                //startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        });
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[layouts.length];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen() {
        prefManager.setFirstTimeLaunch(false);
        storeNewSeed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("password", password);
        startActivity(intent);
        finish();
    }

    private void launchHomeScreenNormal() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("password", password);
        startActivity(intent);
        finish();
    }

    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {



        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            Log.e("########", "onPageSelected: " + position);

            // changing the next button text 'NEXT' / 'GOT IT'
            if(hasSeed()) {
                 if (position == 1) {
                    // Screen3 -> Ask for Permissions
                    btnNext.setText("OK");
                }
            } else {
                if (position == layouts.length - 1) {
                    // last page. make button text to GOT IT
                    btnNext.setText(getString(R.string.start));
                } else if (position == 2) {
                    // Screen3 -> Ask for Permissions
                    btnNext.setText("OK");
                } else if(position == 3) {
                    //Screen4 -> Generate Seed
                    btnNext.setText(getString(R.string.generateSeed));
                    seed = SeedRandomGenerator.generateNewSeed();
                } else if(position == 4) {
                    //Screen5 -> Save Password
                    btnNext.setText(getString(R.string.save));
                    btnNext.setVisibility(View.GONE);

                    seedView = (TextView) findViewById(R.id.seed_view);
                    seedView.setText(seed);

                    passwordInput = (EditText) findViewById(R.id.password);
                    passwordInput.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if(editable.length() != 0) {
                                btnNext.setVisibility(View.VISIBLE);
                            } else {
                                btnNext.setVisibility(View.GONE);
                            }
                            password = passwordInput.getText().toString();
                        }
                    });

                } else {
                    // still pages are left
                    btnNext.setText(getString(R.string.next));
                }
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * Request Permissions
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
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
    }

    private void storeNewSeed() {
        // store the seed encrypted
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(password).build();
        encryptedPreferences.edit()
                .putString(getString(R.string.encrypted_seed), seed)
                .apply();
        // store the status in plain mode
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.active_seed), true);
        editor.apply();
    }

   private boolean hasSeed() {
       SharedPreferences sharedPref = this.getSharedPreferences(
               getString(R.string.preference_file_key), Context.MODE_PRIVATE);
       return sharedPref.getBoolean(getString(R.string.active_seed), false);
   }

    private boolean decryptSeed(String password) {
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(password).build();
        seed = encryptedPreferences.getString(getString(R.string.encrypted_seed), null);

        if (seed != null) {
            this.password = password;
            return true;
        } else {
            final EditText field = (EditText) findViewById(R.id.password);
            field.setText("");
            Toast toast= Toast.makeText(getApplicationContext(),
                    getString(R.string.wrong_password), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            return false;
        }
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
                            Log.d("Permissions", "onRequestPermissionsResult: denied: " + permissions[i]);
                        }
                        i++;
                    }

                    AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                    int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), getPackageName());
                    if (mode != AppOpsManager.MODE_ALLOWED) {
                        ok = false;
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                    }

                    int mode2 = appOps.checkOpNoThrow(AppOpsManager.OPSTR_WRITE_SETTINGS,
                            android.os.Process.myUid(), getPackageName());
                    if (mode2 != AppOpsManager.MODE_ALLOWED) {
                        ok = false;
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        startActivity(intent);
                    }
                    if (ok) {
                        Toast.makeText(WelcomeActivity.this, "Permissions granted", Toast.LENGTH_SHORT).show();
                        btnNext.setText("DONE");
                    } else {
                        Toast.makeText(WelcomeActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
                        btnNext.setText("OK");
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WelcomeActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);

            if(hasSeed() && position == 0) {
                EditText pw = (EditText) findViewById(R.id.password);
                pw.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        Log.d("####", "onTextChanged: ");
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if( pw.getText().toString().length() != 0) {
                            btnNext.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}