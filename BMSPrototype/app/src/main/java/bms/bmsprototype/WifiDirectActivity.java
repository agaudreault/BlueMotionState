package bms.bmsprototype;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by cara1912 on 2016-01-28.
 */
public interface WifiDirectActivity  {
    void onWifiStateChanged(boolean enabled);
    void onAvailableDevice(WifiP2pDevice device);
    void onUnavailableDevice(WifiP2pDevice device);
}
