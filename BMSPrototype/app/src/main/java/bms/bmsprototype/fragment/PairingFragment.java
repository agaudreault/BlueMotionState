package bms.bmsprototype.fragment;

import android.app.Fragment;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bms.bmsprototype.MainActivity;
import bms.bmsprototype.PrototypeActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.listener.GyroscopeListener;

public class PairingFragment extends Fragment {

    public static final String TAG = "PairingFragment";
    private MainActivity _parentActivity;

    /**
     * Create a new instance of PairingFragment
     */
    public static PairingFragment newInstance() {
        PairingFragment f = new PairingFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pairing_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
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
