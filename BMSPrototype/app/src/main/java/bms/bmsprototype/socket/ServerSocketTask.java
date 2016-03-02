package bms.bmsprototype.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import bms.bmsprototype.WifiDirectActivity;

/**
 * Created by cara1912 on 2016-02-03.
 */
public class ServerSocketTask extends AsyncTask<Void, Void, Socket> {
    private static final String LOG_TAG = "ServerSocketTask";
    public static final int PORT = 8888;

    private WifiDirectActivity _activity;

    public ServerSocketTask(WifiDirectActivity activity) {
        _activity = activity;
    }

    @Override
    protected Socket doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            _activity.onServerSocketOpened(serverSocket);
            return serverSocket.accept();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error while opening the server socket", e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Socket socket) {
        super.onPostExecute(socket);

        if(socket != null && socket.isConnected())
            _activity.onSocketConnected(socket);
    }
}
