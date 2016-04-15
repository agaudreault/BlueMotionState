package bms.bmsprototype.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Base class for a client or a server socket task.
 */
public abstract class SocketTask extends AsyncTask<Void, Void, Socket> {

    /**
     * Connection Callback to use
     */
    public interface WifiDirectSocketEventListener {
        void onSocketConnected(Socket socket);
        void onSocketTimeout();
    }
}
