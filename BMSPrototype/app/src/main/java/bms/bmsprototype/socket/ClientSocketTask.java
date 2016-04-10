package bms.bmsprototype.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by cara1912 on 2016-02-11.
 */
public class ClientSocketTask extends SocketTask {
    private static final String LOG_TAG = "ClientSocketTask";
    public static final int CONNECTION_TIMEOUT = 3000;

    private WifiDirectSocketEventListener _listener;
    private InetAddress _serverAddress;
    private int _port;

    public ClientSocketTask(WifiDirectSocketEventListener listener, int port, InetAddress serverAddress) {
        _listener = listener;
        _serverAddress = serverAddress;
        _port = port;
    }

    @Override
    protected Socket doInBackground(Void... params) {
        if(_serverAddress == null)
            return null;

        InetSocketAddress serverSocketAddress = new InetSocketAddress(_serverAddress, _port);
        Socket socket;
        int failCount = 0;

        while(!isCancelled()) {
            try {
                socket = new Socket();
                socket.bind(null);
                socket.connect(serverSocketAddress, CONNECTION_TIMEOUT);
                return socket;

            } catch (SocketTimeoutException e){
                Log.d(LOG_TAG, "Socket timeout", e);
            }  catch (IOException e) {
                Log.d(LOG_TAG, "Error while connecting to server socket", e);
                try {
                    Thread.sleep(CONNECTION_TIMEOUT);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            ++failCount;

            if(failCount > 2) {
                _listener.onSocketTimeout();
                return null;
            }
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
