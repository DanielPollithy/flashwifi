package com.flashwifi.wifip2p;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by Jenny on 25.01.2018.
 */

public class StartFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_fragment, container, false);

        MainActivity activity = (MainActivity) getActivity();

        RelativeLayout search = (RelativeLayout) view.findViewById(R.id.search_hotspot);

        search.setOnClickListener(v -> {
            MenuItem item = activity.getMenuItem(0);
            activity.onNavigationItemSelected(item);
        });

        RelativeLayout open = (RelativeLayout) view.findViewById(R.id.open_hotspot);

        open.setOnClickListener(v -> {
            MenuItem item = activity.getMenuItem(1);
            activity.onNavigationItemSelected(item);
        });

        return view;
    }
}
