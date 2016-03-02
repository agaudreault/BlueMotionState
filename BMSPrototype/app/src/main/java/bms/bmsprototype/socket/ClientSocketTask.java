package bms.bmsprototype.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import bms.bmsprototype.WifiDirectActivity;

/**
 * Created by cara1912 on 2016-02-11.
 */
public class ClientSocketTask extends AsyncTask<Void, Void, Socket> {
    private static final String LOG_TAG = "ClientSocketTask";
    public static final int CONNECTION_TIMEOUT = 5000;

    private WifiDirectActivity _activity;
    private InetAddress _serverAddress;

    public ClientSocketTask(WifiDirectActivity activity, InetAddress serverAddress) {
        _activity = activity;
        _serverAddress = serverAddress;
    }

    @Override
    protected Socket doInBackground(Void... params) {
        if(_serverAddress == null)
            return null;

        InetSocketAddress serverSocketAddress = new InetSocketAddress(_serverAddress, ServerSocketTask.PORT);
        Socket socket;

        while(true) {
            try {
                try {
                    socket = new Socket();
                    socket.bind(null);
                    socket.connect(serverSocketAddress, CONNECTION_TIMEOUT);
                    return socket;
                } catch (ConnectException e) {
                    Log.d(LOG_TAG, "Connection attempt failed", e);
                    Thread.sleep(CONNECTION_TIMEOUT);
                }
            } catch (IOException | InterruptedException e) {
                Log.d(LOG_TAG, "Error while connecting to server socket", e);
            }
        }
    }

    @Override
    protected void onPostExecute(Socket socket) {
        super.onPostExecute(socket);

        if(socket != null && socket.isConnected())
            _activity.onSocketConnected(socket);
    }
}
