package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flashwifi.wifip2p.datastore.PeerInformation;
import com.flashwifi.wifip2p.datastore.PeerListAdapter;
import com.flashwifi.wifip2p.datastore.PeerStore;

import java.net.InetAddress;
import java.util.ArrayList;

import com.flashwifi.wifip2p.broadcast.WiFiDirectBroadcastService;

/**
 * Fragment that appears in the "content_frame", shows a planet
 */
public class SearchFragment extends Fragment {

    public final static String TAG = "SearchActivity";

    BroadcastReceiver updateUIReceiver = null;

    ArrayList<String> arrayList;
    PeerListAdapter peerListAdapter;

    View view;
    private boolean busy = false;


    public SearchFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        return rootView;
    }

    public WiFiDirectBroadcastService getmService() {
        MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            return act.getmService();
        }
        return null;
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

    public static SearchFragment newInstance()
    {
        SearchFragment f = new SearchFragment();
        //Bundle bdl = new Bundle(2);
        //bdl.putInt(EXTRA_TITLE, title);
        //bdl.putString(EXTRA_MESSAGE, message);
        //f.setArguments(bdl);
        return f;
    }

    private void updateList() {
        peerListAdapter.clear();
        // peerListAdapter.notifyDataSetInvalidated();
        peerListAdapter.addAll(PeerStore.getInstance().getPeerArrayList());
        peerListAdapter.notifyDataSetChanged();
    }

    private void updateUi(Intent intent) {
        updateList();

        String what = intent.getStringExtra("what");
        String message = intent.getStringExtra("message");

        if (what != null && what.equals("connectivity_changed")) {
            String currentDeviceConnected = intent.getStringExtra("currentDeviceConnected");
            Log.d(TAG, "updateUi: ------------------------_> I was wrong");
            //startNegotiationProtocol(currentDeviceConnected);
        }

        if (message != null && message.equals("error")) {
            String snd_message = intent.getStringExtra("snd_message");
            Snackbar.make(view, snd_message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            updateList();
        }
    }



    @Override
    public void onPause() {
        if (updateUIReceiver != null) {
            getActivity().unregisterReceiver(updateUIReceiver);
            updateUIReceiver = null;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (updateUIReceiver != null) {
            getActivity().unregisterReceiver(updateUIReceiver);
            updateUIReceiver = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (updateUIReceiver != null) {
            getActivity().unregisterReceiver(updateUIReceiver);
            updateUIReceiver = null;
        }
        super.onDestroy();
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

    private void initFragment() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.flashwifi.wifip2p.update_ui");
        filter.addAction("com.flashwifi.wifip2p.start_roaming");
        filter.addAction("com.flashwifi.wifip2p.stop_roaming");

        if (updateUIReceiver == null) {

            updateUIReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getActivity() == null) {
                        Log.d(TAG, "onReceive: getActivity is null");
                        return;
                    }
                    Log.d("", "onReceive: FRAGMENT HAT WAS");
                    if (intent.getAction().equals("com.flashwifi.wifip2p.start_roaming")) {
                        String mac = intent.getStringExtra("peer_mac_address");
                        ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.startSearchButton);
                        toggle.setChecked(false);
                    } else if (intent.getAction().equals("com.flashwifi.wifip2p.stop_roaming")) {
                        PeerStore.getInstance().clear();
                        busy = false;
                        ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.startSearchButton);
                        toggle.setChecked(false);
                    } else {
                        updateUi(intent);
                    }
                }
            };
            getActivity().registerReceiver(updateUIReceiver, filter);

        }

        view = getActivity().findViewById(R.id.fragment_view);

        initUI();
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Discover Peers");

        final ToggleButton toggle = (ToggleButton) getActivity().findViewById(R.id.startSearchButton);

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (!getmService().isSetup()) {
                    Snackbar.make(view, "Please enable WiFi P2P", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    toggle.setChecked(false);
                    return;
                }
                updateList();
                if (toggle.isChecked()) {
                    getmService().setInRoleHotspot(false);
                    getmService().setInRoleConsumer(true);
                    startSearching();
                } else {
                    getmService().setInRoleHotspot(false);
                    getmService().setInRoleConsumer(false);
                    stopSearching();
                }
            }
        });

        ListView listView = (ListView) getActivity().findViewById(R.id.peer_list);
        arrayList = new ArrayList<>();

        peerListAdapter = new PeerListAdapter(
                getActivity(),
                R.layout.itemlistview,
                PeerStore.getInstance().getPeerArrayList());

        listView.setAdapter(peerListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!busy) {
                    busy = true;

                    // show progress bar
                    ProgressBar progressConnection = (ProgressBar) getActivity().findViewById(R.id.progressConnection);
                    progressConnection.setVisibility(View.VISIBLE);

                    PeerInformation peer = PeerStore.getInstance().getPeerArrayList().get(i);

                    peer.setSelected(true);
                    updateList();

                    String address = peer.getWifiP2pDevice().deviceAddress;
                    String name = peer.getWifiP2pDevice().deviceName;

                    startChat(address, name);
                } else {
                    Toast.makeText(view.getContext(), "Busy", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void startChat(final String address, String name) {
        getmService().connect(address, null);
    }

    private void stopSearching() {
        getmService().stopDiscovery(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(view, "Stopped search", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Snackbar.make(view, "Aaaargh :( problem stopping search!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    public void startSearching() {
        getmService().getPeerList(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(view, "Successfully searched for peers", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Snackbar.make(view, "Aaaargh :( Peering problem!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

    }
}