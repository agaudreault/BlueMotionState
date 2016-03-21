package bms.bmsprototype.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import bms.bmsprototype.PrototypeActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.dialog.ErrorDialog;
import bms.bmsprototype.helper.CameraHelper;
import bms.bmsprototype.helper.PermissionHelper;

public class StreamSurfaceFragment extends Fragment {

    public static final String TAG = "StreamSurfaceFragment";

    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_PERMISSIONS = 1;
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private PrototypeActivity mParentActivity;

    private TextureView mTextureView;
    private Button mButtonVideo;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private String mCurrentFileName = null;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };


    private Size mPreviewSize;
    private Size mVideoSize;

    private CaptureRequest.Builder mPreviewBuilder;
    //private MediaRecorder mMediaRecorder;

    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();

            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    public static StreamSurfaceFragment newInstance() {
        return new StreamSurfaceFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.streamed_activity, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mParentActivity = (PrototypeActivity) getActivity();
        mTextureView = (TextureView) view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.video: {
                        if (mIsRecordingVideo) {
                            stopRecordingVideo();
                        } else {
                            startRecordingVideo();
                        }
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private Handler handler = new Handler();
    MediaRecorder streamRecorder = new MediaRecorder();


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_PERMISSIONS) {
            if (!PermissionHelper.validateRequest(grantResults, VIDEO_PERMISSIONS)) {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height) {
        for (String permission : VIDEO_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                PermissionHelper.requestVideoPermissions(this, REQUEST_PERMISSIONS, VIDEO_PERMISSIONS, FRAGMENT_DIALOG);
                return;
            }
        }

        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = CameraHelper.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = CameraHelper.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

//            int orientation = getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            } else {
//                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
//            }
            configureTransform(width, height);
            //mMediaRecorder = new MediaRecorder();

            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
//            if (null != mMediaRecorder) {
//                mMediaRecorder.release();
//                mMediaRecorder = null;
//            }

            if(!mIsRecordingVideo && mCurrentFileName != null) {
                File emptyPreview = new File(mCurrentFileName);
                if(emptyPreview.exists())
                    emptyPreview.delete();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            //setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);


            if(mParentActivity.isSocketConnected() && setUpStreamRecorder()) {
                Surface recorderSurface = streamRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewBuilder.addTarget(recorderSurface);
            }

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();

            // Auto focus should be continuous for camera preview.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Flash is automatically enabled when necessary.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

//    private void setUpMediaRecorder() throws IOException {
//        final Activity activity = getActivity();
//        if (null == activity) {
//            return;
//        }
//
//        mCurrentFileName = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString();
//
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
//        mMediaRecorder.setOutputFile(mCurrentFileName);
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//        mMediaRecorder.setOrientationHint(90);
//        mMediaRecorder.prepare();
//    }

    private boolean setUpStreamRecorder() {
        streamRecorder.reset();
        boolean isPrepared = false;

        try {
            // Begin video communication

            ParcelFileDescriptor[] parcelFileDescriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(parcelFileDescriptors[0]);
            ParcelFileDescriptor parcelWrite  = new ParcelFileDescriptor(parcelFileDescriptors[1]);
            InputStream readStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);

            mParentActivity.sendStream(readStream);

            //streamRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            streamRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            streamRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
            streamRecorder.setVideoFrameRate(5);
            streamRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            streamRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
            //streamRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);
            streamRecorder.setOrientationHint(90);

            streamRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    Log.e("MEDIA_RECORDER_ON_ERROR", "(" + what + ", " + extra + ")");
                }
            });
            streamRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {

                    Log.i("MEDIA_RECORDER_ON_INFO", "(" + what + ", " + extra + ")");
                }
            });

            //Uncomment this line to test the mediaRecorder in local
            //streamRecorder.setOutputFile(CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString());
            streamRecorder.setOutputFile(parcelWrite.getFileDescriptor());

            streamRecorder.prepare();
            isPrepared = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isPrepared;
    }


    private void startRecordingVideo() {
        try {

            // Start recording
            if(mParentActivity.isSocketConnected() && !mIsRecordingVideo) {
                // UI
                mButtonVideo.setText(R.string.stop);

                //call this method to add our streamRecorder to surfaces list receiving camera informations
                startPreview();

                streamRecorder.start();
                Toast.makeText(getActivity(), "Start streaming", Toast.LENGTH_LONG).show();
                mIsRecordingVideo = true;
            }
            else
            {
                Toast.makeText(getActivity(), "Nobody to stream yet.", Toast.LENGTH_LONG).show();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Something went wrong.", Toast.LENGTH_LONG).show();
        }catch (Exception e) {
            Log.e("DAMN", e.toString());
        }
    }

    private void stopRecordingVideo() {

        if(mIsRecordingVideo) {
            // UI
            mIsRecordingVideo = false;
            mButtonVideo.setText(R.string.record);
            // Stop recording
            streamRecorder.stop();

            startPreview();

//            Activity activity = getActivity();
//            if (null != activity) {
//
//                Toast.makeText(activity, "Video saved: " + "to samples",
//                        Toast.LENGTH_SHORT).show();
//
//            }
        }
    }
}
