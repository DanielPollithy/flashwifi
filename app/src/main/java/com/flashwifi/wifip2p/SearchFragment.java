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

    WiFiDirectBroadcastService mService;
    boolean mBound = false;
    BroadcastReceiver updateUIReceiver;

    ArrayList<String> arrayList;
    PeerListAdapter peerListAdapter;


    public SearchFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        //int i = getArguments().getInt(ARG_PLANET_NUMBER);
        //String planet = getResources().getStringArray(R.array.planets_array)[i];

        //int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()),
        //        "drawable", getActivity().getPackageName());
        //((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
        //getActivity().setTitle(planet);

        // initUI();

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

    public static SearchFragment newInstance()
    {
        SearchFragment f = new SearchFragment();
        //Bundle bdl = new Bundle(2);
        //bdl.putInt(EXTRA_TITLE, title);
        //bdl.putString(EXTRA_MESSAGE, message);
        //f.setArguments(bdl);
        return f;
    }

    private void updateUi(Intent intent) {
        peerListAdapter.notifyDataSetInvalidated();
        peerListAdapter.clear();
        peerListAdapter.addAll(PeerStore.getInstance().getPeerArrayList());
        peerListAdapter.notifyDataSetChanged();

        String what = intent.getStringExtra("what");
        Log.d(">>>>>>>>>>>>", "updateUi: " + what);

        if (what == null) {
            what = "";
        }

        // ToDo: do we need this???
        if (what.equals("connectivity_changed")) {
            // startNegotiationProtocol();
        }
    }

    private void startNegotiationProtocol(String macAddress){
        WifiP2pInfo p2p_info = mService.getP2p_info();

        if (p2p_info.isGroupOwner) {
            startNegotiationProtocolServer(macAddress);
        } else {
            startNegotiationProtocolClient(macAddress);
        }
    }

    private void startNegotiationProtocolServer(String macAddress) {
        // starts the server if necessary
        WifiP2pInfo p2p_info = mService.getP2p_info();
        NetworkInfo network_info = mService.getNetwork_info();

        if (network_info.getState() == NetworkInfo.State.CONNECTED) {
            if (p2p_info.isGroupOwner) {
                Log.d(TAG, "You are the group owner");
                mService.startNegotiationServer(true, macAddress);
                Log.d(TAG, "SocketServer started");
            }
        }
    }

    private void startNegotiationProtocolClient(String macAddress){
        // starts the server if necessary
        WifiP2pInfo p2p_info = mService.getP2p_info();
        NetworkInfo network_info = mService.getNetwork_info();

        if (network_info.getState() == NetworkInfo.State.CONNECTED) {
            if (!p2p_info.isGroupOwner) {
                Log.d(TAG, "You are a member of the group");
                // groupOwnerAddress = p2p_info.groupOwnerAddress;
                InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                Log.d(TAG, "Group owner address: " + p2p_info.groupOwnerAddress.getHostAddress());
                mService.startNegotiationClient(groupOwnerAddress, true, macAddress);
                Log.d(TAG, "Client Socket started");
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(mConnection);
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
                Log.d("", "onReceive: FRAGMENT HAT WAS");
                updateUi(intent);
            }
        };
        getActivity().registerReceiver(updateUIReceiver, filter);

        // Bind to Service
        Intent intent = new Intent(getActivity(), WiFiDirectBroadcastService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        initUI();
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Discover Peers");
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onRefreshButtonClick();
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
                PeerInformation peer = PeerStore.getInstance().getPeerArrayList().get(i);

                String address = peer.getWifiP2pDevice().deviceAddress;
                String name = peer.getWifiP2pDevice().deviceName;

                startChat(address, name);
            }
        });
    }

    public void startChat(String address, String name) {
        // start the socket for the negotiation
        startNegotiationProtocol(address);


        /*Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("address", address);
        intent.putExtra("name", name);
        startActivity(intent);*/
    }

    public void onRefreshButtonClick() {
        final View view = getActivity().findViewById(R.id.main_view);
        if (mBound) {
            mService.getPeerList(new WifiP2pManager.ActionListener() {
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