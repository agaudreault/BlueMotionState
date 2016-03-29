package bms.bmsprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.net.Socket;
import java.util.ArrayList;

import bms.bmsprototype.utils.AvailableDevicesListAdapter;

public class PrototypeActivity extends WifiDirectActivity {

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
        setContentView(R.layout.old_main_activity);

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
