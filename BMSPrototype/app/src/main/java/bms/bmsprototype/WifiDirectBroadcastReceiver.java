package bms.bmsprototype;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by cara1912 on 2016-01-28.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiDirectEventListener _listener;

    private WifiP2pManager.PeerListListener _peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            for(WifiP2pDevice device : peers.getDeviceList()) {
                switch(device.status) {
                    case WifiP2pDevice.CONNECTED:
                        _listener.onConnectedDeviceFound(device);
                        break;
                    case WifiP2pDevice.INVITED:
                        _listener.onInvitedDeviceFound(device);
                        break;
                    case WifiP2pDevice.FAILED:
                        _listener.onFailedDeviceFound(device);
                        break;
                    case WifiP2pDevice.AVAILABLE:
                        _listener.onAvailableDeviceFound(device);
                        break;
                    case WifiP2pDevice.UNAVAILABLE:
                        _listener.onUnavailableDeviceFound(device);
                        break;
                }
            }
        }
    };

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectEventListener listener) {
        super();
        _manager = manager;
        _channel = channel;
        _listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction())
        {
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Connection state changed!
                NetworkInfo networkState = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                // Check if we connected or disconnected.
                _listener.onConnectedDevice(networkState.isConnected());
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                if(_manager != null && discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED)
                {
                    _manager.requestPeers(_channel, _peerListListener);
                }
                _listener.onPeerDiscoveryStateChanged(discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if(_manager != null)
                    _manager.requestPeers(_channel, _peerListListener);
                break;
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                //sticky intent!
                int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                _listener.onWifiStateChanged(wifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                //TODO : React to this event if necessary
                break;
        }
    }
}
