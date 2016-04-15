package bms.bmsprototype.socket;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import bms.bmsprototype.helper.WifiDirectHelper;

/**
 * Read bitmaps through a socket output stream and return each bitmap through
 * a callback.
 */
public class SocketBitmapReader implements Runnable {
    private static final String TAG = "SocketBitmapReader";
    private static final int LENGTH_BUFFER_SIZE = 4;
    private static final int BYTES_BUFFER_SIZE = 32768;
    private static final int READING_SIZE = 1024;

    /**
     * The callback
     */
    public interface EventListener{
        void onEncodedBitmapReceived(byte[] encodedBitmap);
    }

    private EventListener _listener;
    private Socket _socket;

    private int _lengthBufferOffset;
    private byte[] _lengthBuffer;

    private ByteBuffer _lengthConverter;
    private int _bitmapLength;

    private int _bytesBufferOffset;
    private byte[] _bytesBuffer;

    /**
     * Create the task.
     *
     * @param listener The callback.
     * @param socket The socket to get the inputStream of.
     */
    public SocketBitmapReader(EventListener listener, Socket socket) {
        _listener = listener;
        _socket = socket;

        _lengthBufferOffset = 0;
        _lengthBuffer = new byte[LENGTH_BUFFER_SIZE];

        _lengthConverter = ByteBuffer.allocate(LENGTH_BUFFER_SIZE);
        _bitmapLength = 0;

        _bytesBufferOffset = 0;
        _bytesBuffer = new byte[BYTES_BUFFER_SIZE];
    }

    @Override
    public void run() {
        if(!WifiDirectHelper.isSocketValid(_socket))
            return;

        try {
            InputStream input = _socket.getInputStream();

            while(true) {
                _lengthConverter.clear();
                _lengthBufferOffset = 0;
                _bytesBufferOffset = 0;

                while(_lengthBufferOffset < LENGTH_BUFFER_SIZE)
                    _lengthBufferOffset += input.read(_lengthBuffer, _lengthBufferOffset, LENGTH_BUFFER_SIZE - _lengthBufferOffset);

                _lengthConverter = _lengthConverter.wrap(_lengthBuffer);
                _bitmapLength = _lengthConverter.getInt();

                while(_bytesBufferOffset < _bitmapLength)
                    _bytesBufferOffset += input.read(_bytesBuffer, _bytesBufferOffset, _bitmapLength - _bytesBufferOffset);

                byte[] copy = new byte[_bitmapLength];
                System.arraycopy(_bytesBuffer, 0, copy, 0, _bitmapLength);

                _listener.onEncodedBitmapReceived(copy);
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, "Error while reading a bitmap", e);
        }
    }
}
