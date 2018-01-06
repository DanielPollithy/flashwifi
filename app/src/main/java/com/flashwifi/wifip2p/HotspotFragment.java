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

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;


public class HotspotFragment extends Fragment {

    public final static String TAG = "HotspotFragment";

    private int numberConnectedPeers = 0;

    WiFiDirectBroadcastService mService;
    boolean mBound = false;
    BroadcastReceiver updateUIReceiver;

    Messenger xService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean xIsBound;

    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessengerService.MSG_SET_VALUE:
                    Log.d(TAG, "Received from service: " + msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger xMessenger = new Messenger(new SearchActivity.IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection xConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            xService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        MessengerService.MSG_REGISTER_CLIENT);
                msg.replyTo = xMessenger;
                xService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null,
                        MessengerService.MSG_SET_VALUE, this.hashCode(), 0);
                xService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            xService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        getActivity().bindService(new Intent(getActivity(), MessengerService.class), xConnection, Context.BIND_AUTO_CREATE);
        xIsBound = true;
    }

    void doUnbindService() {
        if (xIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (xService != null) {
                try {
                    Message msg = Message.obtain(null,
                            MessengerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = xMessenger;
                    xService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            getActivity().unbindService(xConnection);
            xIsBound = false;
        }
    }

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

            if (network_info.getState() == NetworkInfo.State.CONNECTED) {
                if (p2p_info.isGroupOwner) {
                    Snackbar.make(activity_view, "You are the group owner", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    mService.startNegotiationServer(false);
                } else {
                    InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                    Snackbar.make(activity_view, "You are only a member of the group", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    mService.startNegotiationClient(groupOwnerAddress, false);
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // getActivity().unbindService(mConnection);
        mBound = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction("jenny.daniel.wifip2p.update_ui");

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