package bms.bmsprototype.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
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

import bms.bmsprototype.activity.MainActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.receiver.WifiDirectBroadcastReceiver;
import bms.bmsprototype.helper.WifiDirectHelper;
import bms.bmsprototype.utils.AvailableDevicesListAdapter;

/**
 * Frqagment used to create a connection with the Wifip2p API and collect the information
 * we need to create a socket connection between both devices.
 */
public class PairingFragment extends BaseFragment {

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
    private boolean _groupRemoved;

    private WifiP2pDevice _connectedDevice;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _parentActivity = (MainActivity) getActivity();
        new LoadingTask().execute();

        _manager = (WifiP2pManager)_parentActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        _channel = _manager.initialize(_parentActivity, _parentActivity.getMainLooper(), null);

        _intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        _availableDevices = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pairing_fragment, container, false);
    }

    /**
     * Called each time the fragment is restored on the UI.
     *
     * @param view the created view
     * @param savedInstanceState the saved instance
     */
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        //clear list and rebind
        _availableDevices.clear();
        _availableDevicesAdapter = new AvailableDevicesListAdapter(_parentActivity, R.layout.available_device, _availableDevices, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectionRequested();
            }
        });

        //clear selected information
        _lastAvailableDeviceSelectedView = null;
        _lastAvailableDeviceSelected = null;

        ListView _lvAvailableDevices = (ListView) _parentActivity.findViewById(R.id.lvAvailableDevices);
        _lvAvailableDevices.setAdapter(_availableDevicesAdapter);
        _lvAvailableDevices.setOnItemClickListener(createItemClickListener());

        _broadcastReceiver = new WifiDirectBroadcastReceiver(_manager, _channel, createWifiDirectEventListener());

        //delete current wifi direct connection
        _groupRemoved = false;
        _manager.removeGroup(_channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                _groupRemoved = true;
                Log.d(TAG, "removeGroup onSuccess -");
            }

            @Override
            public void onFailure(int reason) {
                //if there is not current group, it may fail but we just wanna be sure no device is paired
                _groupRemoved = true;
                Log.d(TAG, "removeGroup onFailure -" + reason);
            }
        });

        //delete current group. there is no way to do that with the API nor to create
        //non-persistant group.
        WifiDirectHelper.deletePersistentGroups(_manager, _channel);

        _wifiEnabled = false;
        _peerDiscoveryStarted = false;
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

    private class LoadingTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            //Insert long initialization code here.
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean abool) {
            _parentActivity.endLoading();
        }
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
            _peerDiscoveryStarted = false;
        }

        super.onStop();
    }

    /**
     * Action to do when the user press Connect on the UI
     */
    private void onConnectionRequested() {
        if(_lastAvailableDeviceSelected != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = _lastAvailableDeviceSelected.deviceAddress;

            //request a connection (invite device)
            _manager.connect(_channel, config, createConnectionListener(_lastAvailableDeviceSelected.deviceName));
        }
    }

    /**
     * Do the final validation before to move to {@link SelectionFragment}.
     * @param info The information to create a socket connection
     */
    private void startTransition(final WifiP2pInfo info)
    {
        _manager.requestGroupInfo(_channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group == null)
                    return;

                //call the transition again if we didn't received the connectedDevice callback
                if (_connectedDevice == null) {
                    startTransition(info);
                    return;
                }
                _parentActivity.moveToSelection(info, _connectedDevice.deviceName);
            }
        });
    }

    /**
     * Create a peer discovery (search for other devices) if one is not already
     * started.
     */
    private void getPeers() {
        _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (!_peerDiscoveryStarted) {
                    _manager.discoverPeers(_channel, createPeerDiscoveryListener());
                }
            }
        });
    }

    /**
     * Add the specified device to the list if it's not already there.
     * @param device The device to add
     */
    private void addDevice(final WifiP2pDevice device)
    {
        if(!_availableDevices.contains(device)) {
            _availableDevices.add(device);
            _availableDevicesAdapter.notifyDataSetChanged();
            _parentActivity.findViewById(R.id.empty).setVisibility(View.GONE);
        }
    }

    /**
     * Remove the specified device from the list.
     * @param device The device to remove
     */
    private void removeDevice(final WifiP2pDevice device)
    {
        if(_availableDevices.contains(device)) {
            _availableDevices.remove(device);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

        // If there are no rows remaining, show the empty view.
        if (_availableDevices.isEmpty()) {
            _parentActivity.findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }
    }

    //region Listener creation

    @NonNull
    private WifiDirectBroadcastReceiver.WifiDirectEventListener createWifiDirectEventListener()
    {
        return new WifiDirectBroadcastReceiver.WifiDirectEventListener() {

            @Override
            public void onConnectedDevice(boolean success) {
                if(success && _groupRemoved) {
                    _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            if (!info.groupFormed) {
                                return;
                            }
                            startTransition(info);
                        }
                    });
                } else {
                    _manager.cancelConnect(_channel, null);
                }
            }

            @Override
            public void onConnectedDeviceFound(WifiP2pDevice device) {
                _parentActivity.addToDebug("Connected device : " + device.deviceName);
                _connectedDevice = device;
                addDevice(device);
            }

            @Override
            public void onInvitedDeviceFound(WifiP2pDevice device) {
                _parentActivity.addToDebug("Invited device : " + device.deviceName);
                if(_connectedDevice != null && _connectedDevice.equals(device))
                    _connectedDevice = null;
                addDevice(device);
            }

            @Override
            public void onFailedDeviceFound(WifiP2pDevice device) {
                _parentActivity.addToDebug("Failed device : " + device.deviceName);
                if(_connectedDevice != null && _connectedDevice.equals(device))
                    _connectedDevice = null;
                removeDevice(device);
            }

            @Override
            public void onAvailableDeviceFound(WifiP2pDevice device) {
                _parentActivity.addToDebug("Available device : " + device.deviceName);
                if(_connectedDevice != null && _connectedDevice.equals(device))
                    _connectedDevice = null;
                addDevice(device);
            }

            @Override
            public void onUnavailableDeviceFound(WifiP2pDevice device) {
                _parentActivity.addToDebug("Unavailable device : " + device.deviceName);
                if(_connectedDevice != null && _connectedDevice.equals(device))
                    _connectedDevice = null;
                removeDevice(device);
            }

            @Override
            public void onPeerDiscoveryStateChanged(boolean started) {
                _peerDiscoveryStarted = started;
            }

            @Override
            public void onWifiStateChanged(boolean enabled) {
                _parentActivity.addToDebug("Wifi state : " + enabled);
                _wifiEnabled = enabled;
                if(_wifiEnabled)
                    getPeers();
            }
        };
    }

    @NonNull
    private WifiP2pManager.ActionListener createPeerDiscoveryListener() {
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
    private WifiP2pManager.ActionListener createStopPeerDiscoveryListener() {
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
    private WifiP2pManager.ActionListener createConnectionListener(final String deviceName) {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connecting to " + deviceName + "...");
                _parentActivity.addToDebug("Connecting to " + deviceName + "...");
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

                Log.d(TAG, "Connection to " + deviceName + " failed : " + reasonName);
                _parentActivity.addToDebug("Connection  to " + deviceName + "failed : " + reasonName);
            }
        };
    }

    private AdapterView.OnItemClickListener createItemClickListener() {
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
