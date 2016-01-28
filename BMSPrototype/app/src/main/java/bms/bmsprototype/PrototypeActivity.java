package bms.bmsprototype;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class PrototypeActivity extends AppCompatActivity implements WifiDirectActivity {
    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler _hideHandler = new Handler();
    private final Runnable _hidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            _tvDebug.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable _showPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private final Runnable _hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private TextView _tvDebug;

    private WifiP2pManager _wifiManager;
    private WifiP2pManager.Channel _wifiChannel;
    private BroadcastReceiver _wifiBroadcastReceiver;
    private IntentFilter _wifiIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prototype);

        _tvDebug = (TextView)findViewById(R.id.tvDebug);

        initializeWifiDirect();
    }

    private void initializeWifiDirect() {
        _wifiManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        _wifiChannel = _wifiManager.initialize(this, getMainLooper(), null);
        _wifiBroadcastReceiver = new WifiDirectBroadcastReceiver(_wifiManager, _wifiChannel, this);

        _wifiIntentFilter = new IntentFilter();
        _wifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        _wifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        _wifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        _wifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        _wifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(_wifiBroadcastReceiver, _wifiIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(_wifiBroadcastReceiver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            _wifiManager.stopPeerDiscovery(_wifiChannel, null);
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
            actionBar.hide();

        _hideHandler.removeCallbacks(_showPart2Runnable);
        _hideHandler.postDelayed(_hidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        _hideHandler.removeCallbacks(_hideRunnable);
        _hideHandler.postDelayed(_hideRunnable, delayMillis);
    }

    private void addToDebug(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tvDebug.setText(_tvDebug.getText() + text + "\n");
            }
        });
    }

    @Override
    public void onWifiStateChanged(boolean enabled) {
        addToDebug("Wifi state : " + enabled);

        if(!enabled) {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            startActivity(i);
        } else {
            _wifiManager.discoverPeers(_wifiChannel, null);
        }
    }

    @Override
    public void onAvailableDevice(WifiP2pDevice device) {
        addToDebug("Available device : " + device.deviceName);
    }

    @Override
    public void onUnavailableDevice(WifiP2pDevice device) {
        addToDebug("Unavailable device : " + device.deviceName);
    }
}
