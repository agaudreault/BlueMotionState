package bms.bmsprototype.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import bms.bmsprototype.R;
import bms.bmsprototype.activity.MainActivity;
import bms.bmsprototype.helper.CameraHelper;
import bms.bmsprototype.helper.PermissionHelper;
import bms.bmsprototype.helper.WifiDirectHelper;
import bms.bmsprototype.socket.SocketTask;

public class StreamingFragment extends BaseFragment {
    public static final String TAG = "StreamingFragment";
    private static final String WIFI_P2P_INFO = "bms.bmsprototype.fragment.StreamingFragment.wifi_p2p_info";
    private static final String DEVICES_NAME = "bms.bmsprototype.fragment.StreamingFragment.devices_name";
    private static final int CAMERA_PERMISSION_REQUEST = 1;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    public static final long TASK_MS_TIME = 33;
    public static final int BITMAP_PORT = 8889;

    private MainActivity _parentActivity;
    private TextureView _tvPreview;

    private WifiP2pInfo _info;
    private String _deviceName;
    private Socket _bitmapSocket;

    private SocketTask _socketConnectionTask;

    private CameraDevice _cameraDevice;
    private HandlerThread _backgroundThread;
    private Handler _backgroundHandler;

    private CaptureRequest.Builder _previewBuilder;
    private CameraCaptureSession _previewSession;

    private Semaphore _cameraOpenCloseLock;

    private Size _videoSize;
    private Size _previewSize;

    private VideoCapturingTask _videoCapturingTask;
    private boolean _continueCapturing;

    private TextureView.SurfaceTextureListener _surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            _continueCapturing = false;

            if(_videoCapturingTask != null){
                try {
                    _videoCapturingTask.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };

    private CameraDevice.StateCallback _stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            _cameraDevice = cameraDevice;
            startPreview();

            _cameraOpenCloseLock.release();

            if (_tvPreview != null)
                configureTransform(_tvPreview.getWidth(), _tvPreview.getHeight());
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            _cameraOpenCloseLock.release();
            cameraDevice.close();
            _cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            _cameraOpenCloseLock.release();
            cameraDevice.close();
            _cameraDevice = null;

            Activity activity = getActivity();

            if(activity != null)
                activity.finish();
        }
    };

    private SocketTask.WifiDirectSocketEventListener _bitmapSocketEventListener = new SocketTask.WifiDirectSocketEventListener() {
        @Override
        public void onSocketConnected(Socket socket) {
            _bitmapSocket = socket;
            startVideoCapturingTask();
            _parentActivity.endLoading();
        }
    };

    public static StreamingFragment newInstance(WifiP2pInfo info, String devicesName) {
        Bundle args = new Bundle();
        args.putParcelable(WIFI_P2P_INFO, info);
        args.putString(DEVICES_NAME, devicesName);

        StreamingFragment f = new StreamingFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _parentActivity = (MainActivity)getActivity();

        _cameraDevice = null;
        _backgroundThread = null;
        _backgroundHandler = null;
        _previewBuilder = null;
        _previewSession = null;
        _videoSize = null;
        _previewSize = null;
        _cameraOpenCloseLock = new Semaphore(1);
        _videoCapturingTask = null;

        _continueCapturing = false;

        _info = getArguments().getParcelable(WIFI_P2P_INFO);
        _deviceName = getArguments().getString(DEVICES_NAME);

        new LoadingTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.streaming_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        _tvPreview = (TextureView)_parentActivity.findViewById(R.id.tvPreview);
        TextView _txtMessage = (TextView) _parentActivity.findViewById(R.id.txtMessage);
        _txtMessage.setText(getString(R.string.streaming_to) + _deviceName);

    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        if (_tvPreview.isAvailable()) {
            openCamera(_tvPreview.getWidth(), _tvPreview.getHeight());
        } else {
            _tvPreview.setSurfaceTextureListener(_surfaceTextureListener);
        }
    }

    @Override
    public void clean() {
        try {
            if(_bitmapSocket != null && !_bitmapSocket.isClosed())
                _bitmapSocket.close();

            if(_socketConnectionTask != null && _socketConnectionTask.getStatus() == AsyncTask.Status.RUNNING)
                _socketConnectionTask.cancel(true);

            stopVideoCapturingTask();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.clean();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        stopVideoCapturingTask();
        super.onPause();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        _backgroundThread = new HandlerThread("CameraBackground");
        _backgroundThread.start();
        _backgroundHandler = new Handler(_backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        _backgroundThread.quitSafely();

        try {
            _backgroundThread.join();
            _backgroundThread = null;
            _backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (!PermissionHelper.validateRequest(grantResults, new String[] { CAMERA_PERMISSION })) {
                Log.d(TAG, "Camera permission hasn't been granted");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height) {
        if (ActivityCompat.checkSelfPermission(getActivity(), CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            FragmentCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        if (_parentActivity == null || _parentActivity.isFinishing())
            return;

        CameraManager manager = (CameraManager)_parentActivity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!_cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            _videoSize = CameraHelper.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            _previewSize = CameraHelper.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, _videoSize);

            configureTransform(width, height);

            manager.openCamera(cameraId, _stateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(_parentActivity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            _parentActivity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.d(TAG, "Camera2 API isn't suported on this device");
            _parentActivity.finish();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            _cameraOpenCloseLock.acquire();

            if (_cameraDevice != null) {
                _cameraDevice.close();
                _cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            _cameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (_cameraDevice == null || !_tvPreview.isAvailable() || _previewSize == null)
            return;

        try {
            SurfaceTexture texture = _tvPreview.getSurfaceTexture();

            texture.setDefaultBufferSize(_previewSize.getWidth(), _previewSize.getHeight());
            _previewBuilder = _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            _previewBuilder.addTarget(previewSurface);

            _cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    _previewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (_parentActivity != null)
                        Toast.makeText(_parentActivity, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, _backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (_cameraDevice == null)
            return;

        try {
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();

            _previewSession.setRepeatingRequest(_previewBuilder.build(), null, _backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `_tvPreview`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `_tvPreview` is fixed.
     *
     * @param viewWidth  The width of `_tvPreview`
     * @param viewHeight The height of `_tvPreview`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (_tvPreview == null || _previewSize == null  || _parentActivity == null)
            return;

        int rotation = _parentActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, _previewSize.getHeight(), _previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / _previewSize.getHeight(),
                    (float) viewWidth / _previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        _tvPreview.setTransform(matrix);
    }

    private void startVideoCapturingTask() {
        _continueCapturing = true;

        if(_videoCapturingTask == null)
            _videoCapturingTask = new VideoCapturingTask(_tvPreview);

        if(_videoCapturingTask.getStatus() != AsyncTask.Status.RUNNING)
            _videoCapturingTask.execute();
    }

    private void stopVideoCapturingTask() {
        _continueCapturing = false;
    }

    class VideoCapturingTask extends AsyncTask<Void, Void, Void> {

        private TextureView _source;

        public VideoCapturingTask(TextureView source) {
            _source = source;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(_source == null || !WifiDirectHelper.isSocketValid(_bitmapSocket))
                return null;

            long startTime;

            BitmapRunnable bitmapRunnable = new BitmapRunnable(_source);
            Bitmap sourceBitmap;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);

            try {
                OutputStream socketOutput = _bitmapSocket.getOutputStream();

                while(_continueCapturing) {
                    startTime = System.currentTimeMillis();

                    synchronized (bitmapRunnable) {
                        _parentActivity.runOnUiThread(bitmapRunnable);
                        bitmapRunnable.wait();
                    }

                    sourceBitmap = bitmapRunnable.getSourceBitmap();

                    if(sourceBitmap == null)
                        continue;

                    os.reset();
                    byteBuffer.clear();

                    if(sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 50, os)) {
                        byte[] bytes = os.toByteArray();
                        byte[] length = byteBuffer.putInt(bytes.length).array();

                        socketOutput.write(length, 0, length.length);
                        socketOutput.write(bytes, 0, bytes.length);
                        socketOutput.flush();
                    }

                    long sleepTime = TASK_MS_TIME - (System.currentTimeMillis() - startTime);
                    //Log.d(TAG, Long.toString(sleepTime));

                    if(sleepTime > 0)
                        Thread.sleep(sleepTime);
                }
            }catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }

            return null;
        }
    }

    private class BitmapRunnable implements Runnable {

        private TextureView _source;
        private Bitmap _sourceBitmap;

        public BitmapRunnable(TextureView source) {
            _source = source;
            _sourceBitmap = null;
        }

        public Bitmap getSourceBitmap() { return _sourceBitmap; }

        @Override
        public void run() {
            if(_source != null)
                _sourceBitmap = _source.getBitmap();

            synchronized (this) {
                notify();
            }
        }
    }

    private class LoadingTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            _socketConnectionTask = WifiDirectHelper.openSocketConnection(_info, BITMAP_PORT, _bitmapSocketEventListener);

            if(_socketConnectionTask == null) {
                Log.d(TAG, "Group is not formed. Cannot connect message socket");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean abool) {
            //if we cant create a connection, go back to previous fragment
            if(!abool)
                _parentActivity.onBackPressed();
        }
    }
}
