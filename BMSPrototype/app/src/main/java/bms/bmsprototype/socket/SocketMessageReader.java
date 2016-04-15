package bms.bmsprototype.socket;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Task to read line from a socket inputStream and return each of them through a
 * callback.
 */
public class SocketMessageReader implements Runnable {
    private static final String TAG = "SocketMessageReader";

    /**
     * The callback.
     */
    public interface EventListener{
        void onMessageReceived(String message);
    }

    private EventListener _listener;
    private Socket _socket;

    /**
     * Create the task.
     *
     * @param listener The callback.
     * @param socket The socket to get the inputStream of.
     */
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