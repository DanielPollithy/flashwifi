package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;


public class HotspotFragment extends Fragment {

    public final static String TAG = "HotspotFragment";

    private int numberConnectedPeers = 0;

    WiFiDirectBroadcastService mService;
    boolean mBound = false;
    BroadcastReceiver updateUIReceiver;

    public HotspotFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_hotspot, container, false);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_scrolling);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public static HotspotFragment newInstance()
    {
        HotspotFragment f = new HotspotFragment();
        //Bundle bdl = new Bundle(2);
        //bdl.putInt(EXTRA_TITLE, title);
        //bdl.putString(EXTRA_MESSAGE, message);
        //f.setArguments(bdl);
        return f;
    }

    private void updateUi(Intent intent) {
        Log.d(TAG, "updateUi: Got some network data into the hotspot fragment");
        String numberAvailableDevices = Integer.toString(mService.getArrayList().size());
        TextView text = (TextView) getActivity().findViewById(R.id.numberPeers);
        text.setText(String.format("%s peers", numberAvailableDevices));
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);

        if (intent.hasExtra("what") && intent.getExtras().getString("what", "").equals("connectivity_changed")) {

            NetworkInfo network_info = mService.getNetwork_info();
            WifiP2pInfo p2p_info = mService.getP2p_info();
            WifiP2pGroup wifiP2pGroup = mService.getP2p_group();

            if (intent.hasExtra("currentDeviceConnected")) {
                String macAddress = intent.getExtras().getString("currentDeviceConnected");
                if (network_info.getState() == NetworkInfo.State.CONNECTED) {
                    // ToDo: look for the other device and make sure we are only two

                    if (p2p_info.isGroupOwner) {
                        Snackbar.make(activity_view, "You are the group owner", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        mService.startNegotiationServer(false, null);
                    } else {
                        InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                        Snackbar.make(activity_view, "You are only a member of the group", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        mService.startNegotiationClient(groupOwnerAddress, false, macAddress);
                    }

                }
            }



        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //getActivity().unbindService(mConnection);
        mBound = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi(intent);
            }
        };
        getActivity().registerReceiver(updateUIReceiver, filter);

        // Bind to Service
        Intent intent = new Intent(getActivity(), WiFiDirectBroadcastService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        initUI();

        testInternetConnection();
        testIotaNodeConnection();
    }

    private void testIotaNodeConnection() {
        // ToDo: Move this to a async task
        /*IotaAPI api = new IotaAPI.Builder()
                .protocol("http")
                .host("iota.bitfinex.com")
                .port("80")
                .build();
        GetNodeInfoResponse response = api.getNodeInfo();

        String latestMilestone = response.getLatestMilestone(); */

        TextView text = (TextView) getActivity().findViewById(R.id.iotaNodeTest);
        text.setVisibility(View.VISIBLE);


    }

    private void testInternetConnection() {
        // ToDo: Move this to a async task
        //boolean status = (isNetworkConnected() && isInternetAvailable());
        boolean status = true;
        TextView text = (TextView) getActivity().findViewById(R.id.internetTest);
        if (status) {
            text.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return !ipAddr.getHostAddress().equals("");

        } catch (Exception e) {
            return false;
        }
    }
    
    private void startDiscovery() {
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);
        if (mBound) {
            mService.getPeerList(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Snackbar.make(activity_view, "Successfully searched for peers", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Snackbar.make(activity_view, "Aaaargh :( Peering problem!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            });
        }
    }

    private void initUI() {
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);
        final ToggleButton button = (ToggleButton) getActivity().findViewById(R.id.startAPButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (button.isChecked()) {
                    startDiscovery();
                    Snackbar.make(activity_view, "Start Discovery Mode", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WiFiDirectBroadcastService.LocalBinder binder = (WiFiDirectBroadcastService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}