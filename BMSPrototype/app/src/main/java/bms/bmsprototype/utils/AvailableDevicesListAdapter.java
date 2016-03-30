package bms.bmsprototype.utils;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import bms.bmsprototype.R;

/**
 * Created by cara1912 on 2016-02-12.
 */
public class AvailableDevicesListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private int _resource;
    private View.OnClickListener _action;

    public AvailableDevicesListAdapter(Context context, int resource, List<WifiP2pDevice> objects, View.OnClickListener action) {
        super(context, resource, objects);
        _resource = resource;
        _action = action;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = LayoutInflater.from(getContext()).inflate(_resource, parent, false);

        TextView tvName = (TextView)itemView.findViewById(R.id.tvName);
        tvName.setText(getItem(position).deviceName);

        // Set a click listener for the "X" button in the row that will remove the row.
        itemView.findViewById(R.id.btnConnect).setOnClickListener(_action);

        return itemView;
    }
}
