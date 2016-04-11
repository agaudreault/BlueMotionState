package bms.bmsprototype.socket;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by cara1912 on 2016-02-03.
 */
public class ServerSocketTask extends SocketTask {
    private static final String LOG_TAG = "ServerSocketTask";

    private WifiDirectSocketEventListener _listener;
    private int _port;

    public ServerSocketTask(WifiDirectSocketEventListener listener, int port) {
        _listener = listener;
        _port = port;
    }

    @Override
    protected Socket doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(_port);
            serverSocket.setSoTimeout(ClientSocketTask.CONNECTION_TIMEOUT);
            Socket clientSocket = null;
            int failCount = 0;

            while(!isCancelled()) {
                try {
                    clientSocket = serverSocket.accept();
                    serverSocket.close();
                    return clientSocket;
                } catch (SocketTimeoutException e) {
                    ++failCount;

                    if(failCount > 2) {
                        _listener.onSocketTimeout();
                        return null;
                    }
                }
            }

        } catch (IOException e) {
            Log.d(LOG_TAG, "Error while opening the server socket", e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Socket socket) {
        super.onPostExecute(socket);

        if(socket != null && socket.isConnected())
            _listener.onSocketConnected(socket);
    }
}
