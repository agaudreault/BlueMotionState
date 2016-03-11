package bms.bmsprototype.socket;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import bms.bmsprototype.WifiDirectActivity;

/**
 * Created by cara1912 on 2016-02-18.
 */
public class SocketInputRunnable implements Runnable {
    private static final String LOG_TAG = "SocketInputRunnable";

    private WifiDirectActivity _activity;
    private Socket _socket;

    public SocketInputRunnable(WifiDirectActivity activity, Socket socket) {
        _activity = activity;
        _socket = socket;
    }

    @Override
    public void run() {
        if(_socket == null || !_socket.isConnected())
            return;

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));

            while(true) {
                String message = bufferedReader.readLine();

                if(message != null)
                    _activity.onMessageReceived(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Error while reading a message", e);
        }
    }
}
