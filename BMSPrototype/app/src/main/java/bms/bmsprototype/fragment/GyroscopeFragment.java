package bms.bmsprototype.fragment;

import android.app.Fragment;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bms.bmsprototype.R;
import bms.bmsprototype.listener.GyroscopeListener;

public class GyroscopeFragment extends Fragment {

    public static final String TAG = "GyroscopeFragment";

    private TextView azimuthTextView;
    private TextView pitchTextView;
    private TextView rollTextView;
    private TextView inclinationTextView;

    GyroscopeListener gyroscopeListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gyroscope_activity, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        gyroscopeListener = new GyroscopeListener((SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE));

        azimuthTextView = (TextView) view.findViewById(R.id.azimuthTxt);
        pitchTextView = (TextView) view.findViewById(R.id.pitchTxt);
        rollTextView = (TextView) view.findViewById(R.id.rollTxt);
        inclinationTextView = (TextView) view.findViewById(R.id.inclinationTxt);
    }

    @Override
    public void onResume() {
        super.onResume();

        gyroscopeListener.registerListener(new GyroscopeListener.GyroscopeCallback() {
            @Override
            public void call(final int azimuth, final int pitch, final int roll, final int inclination) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        azimuthTextView.setText(getString(R.string.azimuth) + azimuth);
                        pitchTextView.setText(getString(R.string.pitch) + pitch);
                        rollTextView.setText(getString(R.string.roll) + roll);
                        inclinationTextView.setText(getString(R.string.inclination) + inclination);
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        gyroscopeListener.unregisterListener();
    }
}
