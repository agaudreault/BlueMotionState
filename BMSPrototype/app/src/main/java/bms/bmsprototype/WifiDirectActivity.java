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


    private Socket _socket;
    private ServerSocket _serverSocket;
    private ClientSocketTask _clientSocketTask;
    private ServerSocketTask _serverSocketTask;
    private Thread _messageReadingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _socket = null;
        _serverSocket = null;
        _clientSocketTask = null;
        _serverSocketTask = null;
        _messageReadingThread = null;
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

    final public boolean isSocketConnected() {
        return _socket != null && _socket.isConnected();
    }

    final public FileDescriptor getSocketFileDescriptor() {
        if (!isSocketConnected())
            return null;
        return ParcelFileDescriptor.fromSocket(_socket).getFileDescriptor();
    }


    public void onMessageReceived(String message) { }

    final protected void connectSocket() {
        try {
            Thread.sleep(1500);                 //If we try to get the connection info immediately after connecting,
        } catch (InterruptedException e) { }    // it's not available and the client never tries to connect to the server socket

//        _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
//            @Override
//            public void onConnectionInfoAvailable(WifiP2pInfo info) {
//
//                //        runOnUiThread(new Runnable() {
////            @Override
////            public void run() {
////                _tvConnectedDeviceName.setText(device.deviceName);
////            }
////        });
//                //connectSocket();
//
//                if (!info.groupFormed)
//                    return;
//
//                if (info.isGroupOwner)
//                    openServerSocket();
//                else
//                    connectToServerSocket(info.groupOwnerAddress);
//            }
//        });
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
