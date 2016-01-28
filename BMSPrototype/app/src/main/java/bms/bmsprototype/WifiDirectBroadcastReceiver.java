package bms.bmsprototype;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by cara1912 on 2016-01-28.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiDirectActivity _activity;

    private WifiP2pDeviceList _previousDeviceList;
    private WifiP2pManager.PeerListListener _peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            for(WifiP2pDevice device : peers.getDeviceList()) {
                if(!_previousDeviceList.getDeviceList().contains(device)) {
                    _activity.onAvailableDevice(device);
                }
            }

            for(WifiP2pDevice previousDevice : _previousDeviceList.getDeviceList()) {
                if(!peers.getDeviceList().contains(previousDevice)) {
                    _activity.onUnavailableDevice(previousDevice);
                }
            }

            _previousDeviceList = peers;
        }
    };

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectActivity activity)
    {
        super();
        _manager = manager;
        _channel = channel;
        _activity = activity;

        _previousDeviceList = new WifiP2pDeviceList();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction())
        {
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                //TODO : Respond to new connection or disconnections
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                //TODO : Respond to peer discovery started or stopped
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if(_manager != null)
                    _manager.requestPeers(_channel, _peerListListener);
                break;
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                _activity.onWifiStateChanged(wifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                //TODO : Respond to this device's wifi state changing
                int i = 0;
                break;
        }
    }
}
