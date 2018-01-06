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
import android.widget.ListView;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Fragment that appears in the "content_frame", shows a planet
 */
public class SearchFragment extends Fragment {

    public final static String TAG = "SearchActivity";

    WiFiDirectBroadcastService mService;
    boolean mBound = false;
    BroadcastReceiver updateUIReceiver;

    ArrayList<String> arrayList;
    ArrayAdapter<String> listAdapter;

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
        listAdapter.notifyDataSetInvalidated();
        listAdapter.clear();
        listAdapter.addAll(mService.getArrayList());
        listAdapter.notifyDataSetChanged();

        String what = intent.getStringExtra("what");
        Log.d(">>>>>>>>>>>>", "updateUi: " + what);

        if (what == null) {
            what = "";
        }

        if (what.equals("connectivity_changed")) {

            NetworkInfo network_info = mService.getNetwork_info();
            WifiP2pInfo p2p_info = mService.getP2p_info();

            if (network_info.getState() == NetworkInfo.State.CONNECTED) {
                if (p2p_info.isGroupOwner) {
                    Log.d(TAG, "You are the group owner");
                    mService.startNegotiationServer(true);
                    Log.d(TAG, "SocketServer started");
                } else {
                    Log.d(TAG, "You are a member of the group");
                    // groupOwnerAddress = p2p_info.groupOwnerAddress;
                    InetAddress groupOwnerAddress = p2p_info.groupOwnerAddress;
                    Log.d(TAG, "Group owner address: " + p2p_info.groupOwnerAddress.getHostAddress());
                    mService.startNegotiationClient(groupOwnerAddress, true);
                    Log.d(TAG, "Client Socket started");
                }
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
        filter.addAction("jenny.daniel.wifip2p.update_ui");

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

        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String[] splittedLine = arrayList.get(i).split(" : ");
                String address = splittedLine[1];
                String name = splittedLine[0];

                startChat(address, name);
            }
        });
    }

    public void startChat(String address, String name) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("address", address);
        intent.putExtra("name", name);
        startActivity(intent);
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