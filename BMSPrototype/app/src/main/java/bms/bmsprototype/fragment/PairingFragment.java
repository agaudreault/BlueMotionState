package bms.bmsprototype.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import bms.bmsprototype.MainActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.WifiDirectBroadcastReceiver;
import bms.bmsprototype.WifiDirectEventListener;
import bms.bmsprototype.utils.AvailableDevicesListAdapter;

public class PairingFragment extends Fragment implements WifiDirectEventListener {

    public static final String TAG = "PairingFragment";
    private MainActivity _parentActivity;


    private ArrayList<WifiP2pDevice> _availableDevices;
    private AvailableDevicesListAdapter _availableDevicesAdapter;
    private View _lastAvailableDeviceSelectedView;
    private WifiP2pDevice _lastAvailableDeviceSelected;

    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiDirectBroadcastReceiver _broadcastReceiver;
    private IntentFilter _intentFilter;

    private boolean _wifiEnabled;
    private boolean _peerDiscoveryStarted;

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
        _parentActivity = (MainActivity) getActivity();

        _availableDevices = new ArrayList<>();
        _availableDevicesAdapter = new AvailableDevicesListAdapter(_parentActivity, R.layout.available_device, _availableDevices, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectionRequested(v);
            }
        });

        _lastAvailableDeviceSelectedView = null;
        _lastAvailableDeviceSelected = null;

        ListView _lvAvailableDevices = (ListView) _parentActivity.findViewById(R.id.lvAvailableDevices);
        _lvAvailableDevices.setAdapter(_availableDevicesAdapter);
        _lvAvailableDevices.setOnItemClickListener(createItemClickListener());

        _manager = (WifiP2pManager)_parentActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        _channel = _manager.initialize(_parentActivity, _parentActivity.getMainLooper(), null);
        _broadcastReceiver = new WifiDirectBroadcastReceiver(_manager, _channel, this);

        _intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        _wifiEnabled = false;
        _peerDiscoveryStarted = false;

        _parentActivity.viewCreated();
    }

    @Override
    public void onStart() {
        super.onStart();
        _parentActivity.registerReceiver(_broadcastReceiver, _intentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        _parentActivity.unregisterReceiver(_broadcastReceiver);

        if(_peerDiscoveryStarted) {
            _manager.stopPeerDiscovery(_channel, createStopPeerDiscoveryListener());
            _wifiEnabled = false;
        }

        super.onStop();
    }


    public void onConnectionRequested(View view) {
        if(_lastAvailableDeviceSelected != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = _lastAvailableDeviceSelected.deviceAddress;

            _manager.connect(_channel, config, createConnectionListener(_lastAvailableDeviceSelected.deviceName, view));
        }
    }

    //region WifiDirectEventListener

    public void onPeerDiscoveryStateChanged(boolean started)
    {
        _peerDiscoveryStarted = started;
    }

    public void onWifiStateChanged(boolean enabled) {
        _parentActivity.addToDebug("Wifi state : " + enabled);
        _wifiEnabled = enabled;
        if(_wifiEnabled)
            getPeers();
    }

    public void onConnectedDevice(boolean success) {
        if(success) {
            _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    if (!info.groupFormed)
                        return;
                    startTransition(info);
                }
            });
        } else {
            _manager.cancelConnect(_channel, null);
        }
    }

    private void startTransition(final WifiP2pInfo info)
    {
        _manager.requestGroupInfo(_channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                Collection<WifiP2pDevice> devices = group.getClientList();
                Collection<String> devicesName = new ArrayList<String>(devices.size());
                for(WifiP2pDevice device : devices) {
                    devicesName.add(device.deviceName);
                }

                _parentActivity.moveToSelection(info, devicesName);
            }
        });
    }

    public void onConnectedDeviceFound(final WifiP2pDevice device) {
        _parentActivity.addToDebug("Connected device : " + device.deviceName);
        removeDevice(device);
    }

    public void onInvitedDeviceFound(WifiP2pDevice device) {
        _parentActivity.addToDebug("Invited device : " + device.deviceName);
        removeDevice(device);
    }

    public void onFailedDeviceFound(WifiP2pDevice device) {
        _parentActivity.addToDebug("Failed device : " + device.deviceName);
        removeDevice(device);
    }

    public void onAvailableDeviceFound(WifiP2pDevice device) {
        _parentActivity.addToDebug("Available device : " + device.deviceName);
        addDevice(device);
    }

    public void onUnavailableDeviceFound(WifiP2pDevice device) {
        _parentActivity.addToDebug("Unavailable device : " + device.deviceName);
        removeDevice(device);
    }

    //endregion

    private void getPeers() {
        _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (info.groupFormed) {
                    startTransition(info);
                } else if (!_peerDiscoveryStarted) {
                    _manager.discoverPeers(_channel, createPeerDiscoveryListener());
                }
            }
        });
    }

    private void addDevice(final WifiP2pDevice device)
    {
        if(!_availableDevices.contains(device)) {
            _availableDevices.add(device);
            _availableDevicesAdapter.notifyDataSetChanged();
            _parentActivity.findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    private void removeDevice(final WifiP2pDevice device)
    {
        if(_availableDevices.contains(device)) {
            _availableDevices.remove(device);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

        // If there are no rows remaining, show the empty view.
        if (_availableDevices.isEmpty()) {
            _parentActivity.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
    }


    //region Listener creation

    @NonNull
    public WifiP2pManager.ActionListener createPeerDiscoveryListener() {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                _parentActivity.addToDebug("Discovering peers...");
                Log.d(TAG, "Peer discovery initialization succeeded");
            }

            @Override
            public void onFailure(int reason) {
                String reasonName;

                if(reason == WifiP2pManager.P2P_UNSUPPORTED)
                    reasonName = "Wifi P2P is unsupported by this device";
                else if(reason == WifiP2pManager.BUSY)
                    reasonName = "The Wifi P2P framework is busy";
                else if(reason == WifiP2pManager.ERROR)
                    reasonName = "Internal error";
                else
                    reasonName = "Unknown error";

                _parentActivity.addToDebug("Peer discovery failed : " + reasonName);
                Log.d(TAG, "Peer discovery initialization failed : " + reasonName);
            }
        };
    }

    @NonNull
    public WifiP2pManager.ActionListener createStopPeerDiscoveryListener() {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer dicovery stopped.");
                _parentActivity.addToDebug("Peer dicovery stopped.");
            }

            @Override
            public void onFailure(int reason) {
                String reasonName;

                if(reason == WifiP2pManager.P2P_UNSUPPORTED)
                    reasonName = "Wifi P2P is unsupported by this device";
                else if(reason == WifiP2pManager.BUSY)
                    reasonName = "The Wifi P2P framework is busy";
                else if(reason == WifiP2pManager.ERROR)
                    reasonName = "Internal error";
                else
                    reasonName = "Unknown error";

                Log.d(TAG, "Peer discovery stopping failed : " + reasonName);
                _parentActivity.addToDebug("Peer discovery stopping failed : " + reasonName);
            }
        };
    }


    @NonNull
    public WifiP2pManager.ActionListener createConnectionListener(final String deviceName, final View view) {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connecting to " + deviceName + "...");
                _parentActivity.addToDebug("Connecting to " + deviceName + "...");
                view.setEnabled(false);
            }

            @Override
            public void onFailure(int reason) {
                String reasonName;

                if(reason == WifiP2pManager.P2P_UNSUPPORTED)
                    reasonName = "Wifi P2P is unsupported by this device";
                else if(reason == WifiP2pManager.BUSY)
                    reasonName = "The Wifi P2P framework is busy";
                else if(reason == WifiP2pManager.ERROR)
                    reasonName = "Internal error";
                else
                    reasonName = "Unknown error";

                Log.d(TAG, "Connection  to " + deviceName + "failed : " + reasonName);
                _parentActivity.addToDebug("Connection  to " + deviceName + "failed : " + reasonName);
                view.setEnabled(true);
            }
        };
    }

    public AdapterView.OnItemClickListener createItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(_lastAvailableDeviceSelectedView != null) {
                    Button btnConnect = (Button) _lastAvailableDeviceSelectedView.findViewById(R.id.btnConnect);
                    btnConnect.setVisibility(View.GONE);
                    _lastAvailableDeviceSelectedView = null;
                }

                Button btnConnect = (Button)view.findViewById(R.id.btnConnect);
                btnConnect.setVisibility(View.VISIBLE);
                _lastAvailableDeviceSelectedView = view;
                _lastAvailableDeviceSelected = (WifiP2pDevice)parent.getItemAtPosition(position);
            }
        };
    }

    //endregion
}
