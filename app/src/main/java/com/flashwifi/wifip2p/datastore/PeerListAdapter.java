package com.flashwifi.wifip2p.datastore;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import com.flashwifi.wifip2p.R;
import com.flashwifi.wifip2p.protocol.NegotiationOffer;

import java.util.List;

public class PeerListAdapter extends ArrayAdapter<PeerInformation> {

    public PeerListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public PeerListAdapter(Context context, int resource, List<PeerInformation> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.itemlistview, null);
        }

        PeerInformation p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.deviceName);
            TextView tt2 = (TextView) v.findViewById(R.id.categoryId);
            TextView tt3 = (TextView) v.findViewById(R.id.description);
            TextView tt4 = (TextView) v.findViewById(R.id.ipAddr);
            TextView tt5 = (TextView) v.findViewById(R.id.iotaPrice);
            TableRow row = (TableRow) v.findViewById(R.id.talkingRow);

            if (row != null) {
                if (p.isSelected()) {
                    row.setVisibility(View.VISIBLE);
                } else {
                    row.setVisibility(View.INVISIBLE);
                }
            }

            WifiP2pDevice device = p.getWifiP2pDevice();
            if (device != null) {
                if (tt1 != null) {
                    tt1.setText(p.getWifiP2pDevice().deviceName);
                }

                if (tt2 != null) {
                    tt2.setText(p.getWifiP2pDevice().deviceAddress);
                }
            }

            NegotiationOffer offer = p.getLatestNegotiationOffer();
            if (offer != null) {
                tt5.setText(Integer.toString(offer.getIotaPerMegabyte()));
            }

            if (p.getIpAddress() != null) {
                tt4.setText(p.getIpAddress());
            }


            if (tt3 != null)
                tt3.setText(String.format("%s", Integer.toString(p.getAge())));
            }




        return v;
    }

}