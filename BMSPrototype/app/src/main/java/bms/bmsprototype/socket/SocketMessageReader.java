package bms.bmsprototype.socket;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import bms.bmsprototype.WifiDirectConnectionManager;

/**
 * Created by cara1912 on 2016-02-18.
 */
public class SocketMessageReader implements Runnable {
    private static final String TAG = "SocketMessageReader";

    public interface EventListener{
        void onMessageReceived(String message);
    }

    private EventListener _listener;
    private Socket _socket;

    public SocketMessageReader(EventListener listener, Socket socket) {
        _listener = listener;
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
                    _listener.onMessageReceived(message);
                else {
                    Log.d(TAG, "readLine() returns null. Socket has been closed.");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error while reading a message", e);
        }
    }
}


//    @Override
//    public void run() {
//        if(_socket == null || !_socket.isConnected())
//            return;
//
//        try {
//            //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
//            InputStream is = _socket.getInputStream();
//            byte[] buf = new byte[8192];
//            int len;
//
//            while(true) {
//                while ((len = is.read(buf, 0, buf.length)) != -1) {
//
//                    //String message = bufferedReader.readLine();
//                    String message = new String(buf, 0, len);
//
//                    if(message != null)
//                        _activity.onMessageReceived(message);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(LOG_TAG, "Error while reading a message", e);
//        }
//    }
