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

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;

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

    private MainActivity _parentActivity;
    private WifiP2pInfo _info;
    private Socket _bitmapSocket;
    private ArrayBlockingQueue<byte[]> _encodedBitmaps;
    private SurfaceView _svPlayback;
    private SurfaceHolder _shPlayback;
    private VideoPlaybackTask _videoPlaybackTask;
    private boolean _continuePlayback;

    private SocketTask.WifiDirectSocketEventListener _bitmapSocketEventListener = new SocketTask.WifiDirectSocketEventListener() {
        @Override
        public void onSocketConnected(Socket socket) {
            _bitmapSocket = socket;
            startVideoPlaybackTask();

            new Thread(new SocketBitmapReader(new SocketBitmapReader.EventListener() {
                @Override
                public void onEncodedBitmapReceived(byte[] encodedBitmap) {
                    try {
                        _encodedBitmaps.put(encodedBitmap);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Error while putting encodedBitmap");
                    }
                }
            }, _bitmapSocket)).start();
        }
    };

    /**
     * Create a new instance of PlaybackFragment
     */
    public static PlaybackFragment newInstance(WifiP2pInfo info) {
        Bundle args = new Bundle();
        args.putParcelable(WIFI_P2P_INFO, info);

        PlaybackFragment f = new PlaybackFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _parentActivity = (MainActivity) getActivity();
        new TestTask().execute();

        _info = getArguments().getParcelable(WIFI_P2P_INFO);

        _encodedBitmaps = new ArrayBlockingQueue<>(30);
        _videoPlaybackTask = null;
        _continuePlayback = false;

        if(!WifiDirectHelper.openSocketConnection(_info, StreamingFragment.BITMAP_PORT, _bitmapSocketEventListener))
            Log.d(TAG, "Group is not formed. Cannot connect message socket");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        _svPlayback = (SurfaceView)_parentActivity.findViewById(R.id.svPlayback);
        _shPlayback = _svPlayback.getHolder();
    }

    @Override
    public void clean() {
        try {
            if(_bitmapSocket != null && !_bitmapSocket.isClosed())
                _bitmapSocket.close();

            stopVideoPlaybackTask();
        } catch (IOException e) {
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

        if(_videoPlaybackTask != null && _videoPlaybackTask.getStatus() == AsyncTask.Status.RUNNING) {
            try {
                _videoPlaybackTask.get();
            } catch (InterruptedException | ExecutionException e) { }
        }
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
            Bitmap scaledBitmap;
            Matrix matrix = new Matrix();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;

            try {
                synchronized (lockCanvasRunnable) {
                    _parentActivity.runOnUiThread(lockCanvasRunnable);
                    lockCanvasRunnable.wait();
                }

                canvas = lockCanvasRunnable.getDestinationCanvas();
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();

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

                    byte[] encodedBitmap = _encodedBitmaps.take();
                    decodedBitmap = BitmapFactory.decodeByteArray(encodedBitmap, 0, encodedBitmap.length, options);
                    scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, canvasWidth, canvasHeight, false);
                    canvas.drawBitmap(scaledBitmap, matrix, null);

                    unlockCanvasRunnable.setCanvas(canvas);

                    synchronized (unlockCanvasRunnable) {
                        _parentActivity.runOnUiThread(unlockCanvasRunnable);
                        unlockCanvasRunnable.wait();
                    }

                    long sleepTime = StreamingFragment.TASK_MS_TIME - (System.currentTimeMillis() - startTime);
                    Log.d(TAG, Long.toString(sleepTime));

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

    private class TestTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean abool) {
            _parentActivity.endLoading();
        }
    }
}