package bms.bmsprototype.socket;

import android.inputmethodservice.InputMethodService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;

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
            //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            InputStream is = _socket.getInputStream();
            byte[] buf = new byte[8192];
            int len;

            while(true) {
                while ((len = is.read(buf, 0, buf.length)) != -1) {

                //String message = bufferedReader.readLine();
                String message = new String(buf, 0, len);

                if(message != null)
                    _activity.onMessageReceived(message);
            }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Error while reading a message", e);
        }
    }
}
