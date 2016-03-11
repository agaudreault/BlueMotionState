package bms.bmsprototype;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import bms.bmsprototype.utils.AvailableDevicesListAdapter;

public class PrototypeActivity extends WifiDirectActivity {
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

    private TextView _tvConnectedDeviceName;
    private EditText _etxtMessage;
    private Button _btnSendMessage;

    private ArrayList<WifiP2pDevice> _availableDevices;
    private AvailableDevicesListAdapter _availableDevicesAdapter;

    private View _lastAvailableDeviceSelectedView;
    private WifiP2pDevice _lastAvailableDeviceSelected;
    private AdapterView.OnItemClickListener _availableDevicesClickListener = new AdapterView.OnItemClickListener() {
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

    private ListView _lvAvailableDevices;
    private TextView _tvDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prototype);

        _tvConnectedDeviceName = (TextView)findViewById(R.id.tvConnectedDeviceName);
        _etxtMessage = (EditText)findViewById(R.id.etxtMessage);
        _btnSendMessage = (Button)findViewById(R.id.btnSendMessage);

        _availableDevices = new ArrayList<>();
        _availableDevicesAdapter = new AvailableDevicesListAdapter(this, R.layout.available_device, _availableDevices);

        _lastAvailableDeviceSelectedView = null;
        _lastAvailableDeviceSelected = null;

        _lvAvailableDevices = (ListView)findViewById(R.id.lvAvailableDevices);
        _lvAvailableDevices.setAdapter(_availableDevicesAdapter);
        _lvAvailableDevices.setOnItemClickListener(_availableDevicesClickListener);

        _tvDebug = (TextView)findViewById(R.id.tvDebug);
    }

    @Override
    protected void onResume() {
        super.onResume();
        delayedHide(100);
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
                _tvDebug.setText(text + "\n" + _tvDebug.getText());
            }
        });
    }

    @Override
    protected void onWifiStateChanged(boolean enabled) {
        super.onWifiStateChanged(enabled);
        addToDebug("Wifi state : " + enabled);

        if(enabled && !isSocketConnected())
            getPeers();
    }

    public void btnConnectOnClick(View view) {
        if(_lastAvailableDeviceSelected != null) {
            connectToPeer(_lastAvailableDeviceSelected);
            view.setEnabled(false);
        }
    }

    @Override
    protected void onConnectedDevice(final WifiP2pDevice device) {
        super.onConnectedDevice(device);

        if(device == null || isSocketConnected())
            return;

        addToDebug("Connected device : " + device.deviceName);

        if(_availableDevices.contains(device)) {
            _availableDevices.remove(device);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tvConnectedDeviceName.setText(device.deviceName);
            }
        });

        connectSocket();
    }

    @Override
    public void onSocketConnected(Socket socket) {
        super.onSocketConnected(socket);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _etxtMessage.setEnabled(true);
                _btnSendMessage.setEnabled(true);
            }
        });
    }

    @Override
    protected void onInvitedDevice(WifiP2pDevice device) {
        super.onInvitedDevice(device);
        addToDebug("Invited device : " + device.deviceName);
    }

    @Override
    protected void onFailedDevice(WifiP2pDevice device) {
        super.onFailedDevice(device);
        addToDebug("Failed device : " + device.deviceName);

        String name = device.deviceName;

        if(_availableDevices.contains(name)) {
            _availableDevices.remove(name);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

    }

    @Override
    protected void onAvailableDevice(WifiP2pDevice device) {
        super.onAvailableDevice(device);
        addToDebug("Available device : " + device.deviceName);

        if(!_availableDevices.contains(device)) {
            _availableDevices.add(device);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

    }

    @Override
    protected void onUnavailableDevice(WifiP2pDevice device) {
        super.onUnavailableDevice(device);
        addToDebug("Unavailable device : " + device.deviceName);

        String name = device.deviceName;

        if(_availableDevices.contains(name)) {
            _availableDevices.remove(name);
            _availableDevicesAdapter.notifyDataSetChanged();
        }

    }

    public void btnSendMessageOnClick(View view) {
        if(_etxtMessage.getText().length() <= 0)
            return;

        sendMessage(_etxtMessage.getText().toString());
        _etxtMessage.setText("");
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);

        if(message != null)
            addToDebug("Message received : " + message);
    }
}
