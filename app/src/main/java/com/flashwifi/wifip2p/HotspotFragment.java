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
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.net.InetAddress;

import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;


public class HotspotFragment extends Fragment {

    public final static String TAG = "HotspotFragment";

    private int numberConnectedPeers = 0;

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
        String numberAvailableDevices = Integer.toString(getmService().getArrayList().size());
        TextView text = (TextView) getActivity().findViewById(R.id.numberPeers);
        text.setVisibility(View.VISIBLE);
        text.setText(String.format("%s peers", numberAvailableDevices));
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);

        //if (intent.hasExtra("what") && intent.getExtras().getString("what", "").equals("connectivity_changed")) {

            NetworkInfo network_info = getmService().getNetwork_info();
            WifiP2pInfo p2p_info = getmService().getP2p_info();
            WifiP2pGroup wifiP2pGroup = getmService().getP2p_group();

            if (intent.hasExtra("currentDeviceConnected")) {
                //String macAddress = intent.getExtras().getString("currentDeviceConnected");
                if (network_info.getState() == NetworkInfo.State.CONNECTED) {
                    // ToDo: look for the other device and make sure we are only two

                    if (p2p_info.isGroupOwner) {
                        Snackbar.make(activity_view, "You are the group owner", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        getmService().startNegotiationServer(false, null);
                    } else {
                        InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                        Snackbar.make(activity_view, "You are only a member of the group", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        getmService().startNegotiationClient(groupOwnerAddress, false, null);
                    }

                }
            }



        //}

    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(updateUIReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        initFragment();
    }

    public WiFiDirectBroadcastService getmService() {
        MainActivity act = (MainActivity) getActivity();
        return act.getmService();
    }

    private void initFragment(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");
        filter.addAction("com.flashwifi.wifip2p.start_roaming");
        filter.addAction("com.flashwifi.wifip2p.stop_roaming");

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() == null) {
                    Log.d(TAG, "onReceive: getActivity is null");
                    return;
                }
                if (intent.getAction().equals("com.flashwifi.wifip2p.start_roaming")) {
                    String mac = intent.getStringExtra("peer_mac_address");
                    ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.startAPButton);
                    toggle.setChecked(false);
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.INVISIBLE);
                } else if (intent.getAction().equals("com.flashwifi.wifip2p.stop_roaming")) {
                    ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.startAPButton);
                    toggle.setChecked(false);
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    updateUi(intent);
                }
            }
        };

        getActivity().registerReceiver(updateUIReceiver, filter);

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


    }

    private void testInternetConnection() {
        // ToDo: Move this to a async task
        //boolean status = (isNetworkConnected() && isInternetAvailable());
    }
    
    private void startDiscovery() {
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);
        if (getmService() != null) {
            getmService().setInRoleConsumer(false);
            getmService().setInRoleHotspot(true);
            getmService().getPeerList(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // show progress wheel
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(int reasonCode) {
                    Snackbar.make(activity_view, "Aaaargh :( Searching problem!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }
    }


    private void stopDiscovery() {
        final View activity_view = getActivity().findViewById(R.id.drawer_layout);
        if (getmService() != null) {
            getmService().stopDiscovery(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    getmService().setInRoleConsumer(false);
                    getmService().setInRoleHotspot(false);
                    Snackbar.make(activity_view, "Stopped Hotspot mode", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFailure(int reasonCode) {
                    getmService().setInRoleConsumer(false);
                    getmService().setInRoleHotspot(false);
                    Snackbar.make(activity_view, "Aaaargh :( Problem stopping discovery", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progressbarAP);
                    progressBar.setVisibility(View.INVISIBLE);
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
                if (getmService() != null) {
                    if (!getmService().isSetup()) {
                        Snackbar.make(activity_view, "Please enable WiFi P2P", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        button.setChecked(false);
                        return;
                    }
                }
                if (button.isChecked()) {
                    startDiscovery();
                    Snackbar.make(activity_view, "Start Discovery Mode", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } else {
                    stopDiscovery();
                    Snackbar.make(activity_view, "Stop Discovery Mode", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }
}