package bms.bmsprototype.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import bms.bmsprototype.activity.MainActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.helper.WifiDirectHelper;
import bms.bmsprototype.socket.SocketBitmapReader;
import bms.bmsprototype.socket.SocketTask;


/**
 * Created by gaua2616 on 2016-02-02.
 */
public class PlaybackFragment extends BaseFragment {
    public static final String TAG = "PlaybackFragment";
    private static final String WIFI_P2P_INFO = "bms.bmsprototype.fragment.PlaybackFragment.wifi_p2p_info";
    private static final String DEVICES_NAME = "bms.bmsprototype.fragment.PlaybackFragment.devices_name";

    private MainActivity _parentActivity;
    private WifiP2pInfo _info;
    private String _deviceName;

    private SocketTask _socketConnectionTask;

    private Socket _bitmapSocket;
    private Thread _bitmapReaderThread;
    private ArrayBlockingQueue<byte[]> _encodedBitmaps;

    private TextView _txtMessage;
    private SurfaceView _svPlayback;
    private SurfaceHolder _shPlayback;
    private VideoPlaybackTask _videoPlaybackTask;
    private boolean _continuePlayback;

    private int _bitmapWidth;
    private int _bitmapHeight;

    private SocketTask.WifiDirectSocketEventListener _bitmapSocketEventListener = new SocketTask.WifiDirectSocketEventListener() {
        @Override
        public void onSocketConnected(Socket socket) {
            _bitmapSocket = socket;

            _bitmapReaderThread = new Thread(new SocketBitmapReader(new SocketBitmapReader.EventListener() {
                @Override
                public void onEncodedBitmapReceived(byte[] encodedBitmap) {
                    try {
                        _encodedBitmaps.put(encodedBitmap);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Error while putting encodedBitmap");
                    }
                }
            }, _bitmapSocket));
            _bitmapReaderThread.start();

            startVideoPlaybackTask();

            _parentActivity.endLoading();
        }

        @Override
        public void onSocketTimeout() {
            _parentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _parentActivity.onBackPressed();
                }
            });
        }
    };

    /**
     * Create a new instance of PlaybackFragment
     */
    public static PlaybackFragment newInstance(WifiP2pInfo info, String devicesName) {
        Bundle args = new Bundle();
        args.putParcelable(WIFI_P2P_INFO, info);
        args.putString(DEVICES_NAME, devicesName);

        PlaybackFragment f = new PlaybackFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _parentActivity = (MainActivity) getActivity();
        _info = getArguments().getParcelable(WIFI_P2P_INFO);
        _deviceName = getArguments().getString(DEVICES_NAME);

        _bitmapSocket = null;
        _bitmapReaderThread = null;
        _encodedBitmaps = new ArrayBlockingQueue<>(30);

        _svPlayback = null;
        _shPlayback = null;
        _videoPlaybackTask = null;
        _continuePlayback = false;

        new LoadingTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        _svPlayback = (SurfaceView)_parentActivity.findViewById(R.id.svPlayback);
        _shPlayback = _svPlayback.getHolder();

        _txtMessage = (TextView)_parentActivity.findViewById(R.id.txtMessage);
        _txtMessage.setText("Receiving stream from " + _deviceName);
    }

    @Override
    public void clean() {
        try {
            if(_bitmapSocket != null && !_bitmapSocket.isClosed())
                _bitmapSocket.close();

            if(_bitmapReaderThread != null && _bitmapReaderThread.isAlive())
                _bitmapReaderThread.join();

            if(_socketConnectionTask != null && _socketConnectionTask.getStatus() == AsyncTask.Status.RUNNING)
                _socketConnectionTask.cancel(true);

            stopVideoPlaybackTask();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        super.clean();
    }

    @Override
    public void onPause() {
        stopVideoPlaybackTask();
        super.onPause();
    }

    private void startVideoPlaybackTask() {
        _continuePlayback = true;

        if(_videoPlaybackTask == null)
            _videoPlaybackTask = new VideoPlaybackTask(_shPlayback);

        if(_videoPlaybackTask.getStatus() != AsyncTask.Status.RUNNING)
            _videoPlaybackTask.execute(null, null, null);
    }

    private void stopVideoPlaybackTask() {
        _continuePlayback = false;
    }

    private class VideoPlaybackTask extends AsyncTask<Void, Void, Void> {

        private SurfaceHolder _surfaceHolder;

        public VideoPlaybackTask(SurfaceHolder surfaceHolder) {
            _surfaceHolder = surfaceHolder;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(_surfaceHolder == null)
                return null;

            long startTime;

            LockCanvasRunnable lockCanvasRunnable = new LockCanvasRunnable(_surfaceHolder);
            UnlockCanvasRunnable unlockCanvasRunnable = new UnlockCanvasRunnable(_surfaceHolder);

            Canvas canvas;
            Bitmap decodedBitmap;
            Matrix matrix = new Matrix();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;

            try {
                synchronized (lockCanvasRunnable) {
                    _parentActivity.runOnUiThread(lockCanvasRunnable);
                    lockCanvasRunnable.wait();
                }

                canvas = lockCanvasRunnable.getDestinationCanvas();

                if(canvas == null) {
                    Log.d(TAG, "Null canvas. Can't get its size");
                    return null;
                }

                matrix.postScale((float)canvas.getWidth() / _bitmapWidth, (float)canvas.getHeight() / _bitmapHeight);
                Log.d(TAG, matrix.toString());

                unlockCanvasRunnable.setCanvas(canvas);

                synchronized (unlockCanvasRunnable) {
                    _parentActivity.runOnUiThread(unlockCanvasRunnable);
                    unlockCanvasRunnable.wait();
                }

                while (_continuePlayback) {
                    startTime = System.currentTimeMillis();

                    synchronized (lockCanvasRunnable) {
                        _parentActivity.runOnUiThread(lockCanvasRunnable);
                        lockCanvasRunnable.wait();
                    }

                    canvas = lockCanvasRunnable.getDestinationCanvas();

                    if (canvas == null)
                        continue;

                    if(!_encodedBitmaps.isEmpty()) {
                        byte[] encodedBitmap = _encodedBitmaps.take();
                        decodedBitmap = BitmapFactory.decodeByteArray(encodedBitmap, 0, encodedBitmap.length, options);
                        canvas.drawBitmap(decodedBitmap, matrix, null);
                    }

                    unlockCanvasRunnable.setCanvas(canvas);

                    synchronized (unlockCanvasRunnable) {
                        _parentActivity.runOnUiThread(unlockCanvasRunnable);
                        unlockCanvasRunnable.wait();
                    }

                    long sleepTime = StreamingFragment.TASK_MS_TIME - (System.currentTimeMillis() - startTime);
                    //Log.d(TAG, Long.toString(sleepTime));

                    if (sleepTime > 0)
                        Thread.sleep(sleepTime);
                }
            } catch(InterruptedException e) {
                Log.d(TAG, "Error while displaying a bitmap");
            }

            return null;
        }
    }

    private class LockCanvasRunnable implements Runnable {

        private SurfaceHolder _destination;
        private Canvas _destinationCanvas;

        public LockCanvasRunnable(SurfaceHolder destination) {
            _destination = destination;
            _destinationCanvas = null;
        }
        public Canvas getDestinationCanvas() { return _destinationCanvas; }

        @Override
        public void run() {
            if(_destination != null && _destination.getSurface().isValid())
                _destinationCanvas = _destination.lockCanvas(null);

            synchronized (this) {
                notify();
            }
        }
    }

    private class UnlockCanvasRunnable implements Runnable {

        private SurfaceHolder _destination;
        private Canvas _destinationCanvas;

        public UnlockCanvasRunnable(SurfaceHolder destination) {
            _destination = destination;
        }

        public void setCanvas(Canvas destinationCanvas) { _destinationCanvas = destinationCanvas; }

        @Override
        public void run() {
            if(_destination != null && _destination.getSurface().isValid())
                _destination.unlockCanvasAndPost(_destinationCanvas);

            synchronized (this) {
                notify();
            }
        }
    }

    private class LoadingTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            _bitmapWidth = getResources().getDimensionPixelOffset(R.dimen.bitmap_width);
            _bitmapHeight = getResources().getDimensionPixelOffset(R.dimen.bitmap_height);

            _socketConnectionTask = WifiDirectHelper.openSocketConnection(_info, StreamingFragment.BITMAP_PORT, _bitmapSocketEventListener);

            if(_socketConnectionTask == null) {
                Log.d(TAG, "Group is not formed. Cannot connect message socket");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean abool) {
            if(!abool)
                _parentActivity.onBackPressed();
        }
    }
}