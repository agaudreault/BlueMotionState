package bms.bmsprototype;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import bms.bmsprototype.socket.ClientSocketTask;
import bms.bmsprototype.socket.ServerSocketTask;
import bms.bmsprototype.socket.SocketInputRunnable;
import bms.bmsprototype.utils.TransferThread;

/**
 * Created by cara1912 on 2016-01-28.
 */
public class WifiDirectActivity extends AppCompatActivity {
    private static final String LOG_TAG = "WifiDirectActivity";

    private WifiP2pManager.ActionListener _peerDiscoveryListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.d(LOG_TAG, "Peer discovery initialization succeeded");
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

            Log.d(LOG_TAG, "Peer discovery initialization failed : " + reasonName);
        }
    };
    private WifiP2pManager.PeerListListener _peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            for(WifiP2pDevice device : peers.getDeviceList()) {
                switch(device.status) {
                    case WifiP2pDevice.CONNECTED:
                        onConnectedDevice(device);
                        break;
                    case WifiP2pDevice.INVITED:
                        onInvitedDevice(device);
                        break;
                    case WifiP2pDevice.FAILED:
                        onFailedDevice(device);
                        break;
                    case WifiP2pDevice.AVAILABLE:
                        onAvailableDevice(device);
                        break;
                    case WifiP2pDevice.UNAVAILABLE:
                        onUnavailableDevice(device);
                        break;
                }
            }
        }
    };
    private WifiP2pManager.ActionListener _stopPeerDiscoveryListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.d(LOG_TAG, "Peer discovery stopping succeeded");
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

            Log.d(LOG_TAG, "Peer discovery stopping failed : " + reasonName);
        }
    };
    private WifiP2pManager.ActionListener _connectionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.d(LOG_TAG, "Connection succeeded");
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

            Log.d(LOG_TAG, "Connection failed : " + reasonName);
        }
    };

    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiDirectBroadcastReceiver _broadcastReceiver;
    private IntentFilter _intentFilter;

    private boolean _wifiEnabled;
    private boolean _peerDiscoveryStarted;

    private Socket _socket;
    private ServerSocket _serverSocket;
    private ClientSocketTask _clientSocketTask;
    private ServerSocketTask _serverSocketTask;
    private Thread _messageReadingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _manager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        _channel = _manager.initialize(this, getMainLooper(), null);
        _broadcastReceiver = new WifiDirectBroadcastReceiver(_manager, _channel, this);

        _intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        _wifiEnabled = false;
        _peerDiscoveryStarted = false;

        _socket = null;
        _serverSocket = null;
        _clientSocketTask = null;
        _serverSocketTask = null;
        _messageReadingThread = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(_broadcastReceiver, _intentFilter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(_broadcastReceiver);

        if(_peerDiscoveryStarted) {
            _manager.stopPeerDiscovery(_channel, _stopPeerDiscoveryListener);
            _wifiEnabled = false;
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(_clientSocketTask != null)
            _clientSocketTask.cancel(true);
        else if(_serverSocketTask != null)
            _serverSocketTask.cancel(true);

        if(_messageReadingThread != null && _messageReadingThread.isAlive())
            _messageReadingThread.interrupt();

        try {
            if(isSocketConnected())
                _socket.close();

            if(_serverSocket != null)
                _serverSocket.close();
        } catch (IOException e) { }

        super.onDestroy();
    }

    final public boolean isWifiEnabled() { return _wifiEnabled; }
    final public boolean isPeerDiscoveryStarted() { return  _peerDiscoveryStarted; }

    final public boolean isSocketConnected() {
        return _socket != null && _socket.isConnected();
    }

    final public FileDescriptor getSocketFileDescriptor() {
        if (!isSocketConnected())
            return null;
        return ParcelFileDescriptor.fromSocket(_socket).getFileDescriptor();
    }



    protected void onWifiStateChanged(boolean enabled) { _wifiEnabled = enabled; }
    protected void onPeerDiscoveryStateChanged(boolean started) { _peerDiscoveryStarted = started; }

    protected void onConnectedDevice(WifiP2pDevice device) { }
    protected void onInvitedDevice(WifiP2pDevice device) { }
    protected void onFailedDevice(WifiP2pDevice device) { }
    protected void onAvailableDevice(WifiP2pDevice device) { }
    protected void onUnavailableDevice(WifiP2pDevice device) { }

    public void onMessageReceived(String message) { }

    final protected void getPeers() {
        _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (!info.groupFormed)
                    _manager.discoverPeers(_channel, _peerDiscoveryListener);
                else
                    _manager.requestPeers(_channel, _peerListListener);
            }
        });
    }

    final protected void connectToPeer(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        _manager.connect(_channel, config, _connectionListener);
    }

    final protected void connectSocket() {
        try {
            Thread.sleep(5000);                 //If we try to get the connection info immediately after connecting,
        } catch (InterruptedException e) { }    // it's not available and the client never tries to connect to the server socket

        _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (!info.groupFormed)
                    return;

                if (info.isGroupOwner)
                    openServerSocket();
                else
                    connectToServerSocket(info.groupOwnerAddress);
            }
        });
    }

    private void openServerSocket() {
        _serverSocketTask = new ServerSocketTask(this);
        _serverSocketTask.execute();
    }

    private void connectToServerSocket(InetAddress serverAddress) {
        _clientSocketTask = new ClientSocketTask(this, serverAddress);
        _clientSocketTask.execute();
    }

    public void onServerSocketOpened(ServerSocket serverSocket) { _serverSocket = serverSocket; }

    public void onSocketConnected(Socket socket) {
        _messageReadingThread = new Thread(new SocketInputRunnable(this, socket));
        _messageReadingThread.start();

        _socket = socket;
    }

    final protected void sendMessage(String message) {
        if(!isSocketConnected())
            return;

        try {
            PrintWriter printWriter = new PrintWriter(_socket.getOutputStream(), true);
            printWriter.println(message);
        } catch (IOException e) {
            Log.d(LOG_TAG, "Exception while sending a message", e);
        }
    }

    final public void sendStream(InputStream inputStream) {
        try {
            new TransferThread(inputStream, _socket.getOutputStream()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
