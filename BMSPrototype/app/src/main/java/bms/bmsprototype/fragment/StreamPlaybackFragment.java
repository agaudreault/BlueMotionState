package bms.bmsprototype.fragment;

import android.app.Fragment;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import bms.bmsprototype.R;


/**
 * Created by gaua2616 on 2016-02-02.
 */
public class StreamPlaybackFragment extends Fragment {

    private MediaPlayer mediaPlayer;
    private SurfaceHolder videoHolder;
    private SurfaceView videoSurface;
    String videoAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";

    public static StreamPlaybackFragment newInstance() {
        return new StreamPlaybackFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_activity, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        //setup ui elements
        videoSurface = (SurfaceView) view.findViewById(R.id.surfView);
        videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initializeMediaPlayer();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mediaPlayer.release();
            }
        });
    }


    public void initializeMediaPlayer() {
        //prepare for playback
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDisplay(videoHolder);
            mediaPlayer.setDataSource(videoAddress);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    mp.start();
                }
            });
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }




}
