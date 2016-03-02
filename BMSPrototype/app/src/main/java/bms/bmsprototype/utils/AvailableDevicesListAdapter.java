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

    public AvailableDevicesListAdapter(Context context, int resource, int textViewResourceId, List<WifiP2pDevice> objects) {
        super(context, resource, textViewResourceId, objects);
        _resource = resource;
    }

    public AvailableDevicesListAdapter(Context context, int resource) {
        super(context, resource);
        _resource = resource;
    }

    public AvailableDevicesListAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        _resource = resource;
    }

    public AvailableDevicesListAdapter(Context context, int resource, WifiP2pDevice[] objects) {
        super(context, resource, objects);
        _resource = resource;
    }

    public AvailableDevicesListAdapter(Context context, int resource, int textViewResourceId, WifiP2pDevice[] objects) {
        super(context, resource, textViewResourceId, objects);
        _resource = resource;
    }

    public AvailableDevicesListAdapter(Context context, int resource, List<WifiP2pDevice> objects) {
        super(context, resource, objects);
        _resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = LayoutInflater.from(getContext()).inflate(_resource, parent, false);

        TextView tvName = (TextView)itemView.findViewById(R.id.tvName);
        tvName.setText(getItem(position).deviceName);

        return itemView;
    }
}
