package bms.bmsprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

public interface WifiDirectEventListener {
    void onConnectedDevice(boolean success);
    void onConnectedDeviceFound(WifiP2pDevice device);
    void onInvitedDeviceFound(WifiP2pDevice device);
    void onFailedDeviceFound(WifiP2pDevice device);
    void onAvailableDeviceFound(WifiP2pDevice device);
    void onUnavailableDeviceFound(WifiP2pDevice device);
    void onPeerDiscoveryStateChanged(boolean started);
    void onWifiStateChanged(boolean enabled);
}