package bms.bmsprototype.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cara1912 on 2016-02-03.
 */
public abstract class SocketTask extends AsyncTask<Void, Void, Socket> {
    public interface WifiDirectSocketEventListener {
        void onSocketConnected(Socket socket);
        void onSocketTimeout();
    }
}
