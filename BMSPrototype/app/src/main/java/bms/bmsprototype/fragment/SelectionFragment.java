package bms.bmsprototype.fragment;

import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import bms.bmsprototype.MainActivity;
import bms.bmsprototype.R;

public class SelectionFragment extends Fragment {

    public static final String TAG = "PairingFragment";
    private static final String WIFI_P2P_INFO = "bms.bmsprototype.fragment.SelectionFragment.wifi_p2p_info";
    private static final String DEVICES_NAME = "bms.bmsprototype.fragment.SelectionFragment.devices_name";

    private MainActivity _parentActivity;
    private WifiP2pInfo _info;
    private Collection<String> _devicesName;

    /**
     * Create a new instance of PairingFragment
     */
    public static SelectionFragment newInstance(WifiP2pInfo info, Collection<String> devicesName) {
        SelectionFragment f = new SelectionFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putStringArray(DEVICES_NAME, devicesName.toArray(new String[devicesName.size()]));
        args.putParcelable(WIFI_P2P_INFO, info);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (savedInstanceState != null){
            _devicesName = Arrays.asList(savedInstanceState.getStringArray(DEVICES_NAME));
            _info = savedInstanceState.getParcelable(WIFI_P2P_INFO);
        }
        return inflater.inflate(R.layout.selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        _parentActivity = (MainActivity) getActivity();

        _parentActivity.viewCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
