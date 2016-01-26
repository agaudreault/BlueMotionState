package bms.bmsprototype;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class PrototypeActivity extends AppCompatActivity {
    private static final int UI_ANIMATION_DELAY = 300;

    private static final int REQUEST_ENABLE_BT = 1;

    private final Handler _hideHandler = new Handler();
    private final Runnable _hidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            _tvDebug.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable _showPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };

    private final Runnable _hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private TextView _tvDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prototype);

        _tvDebug = (TextView)findViewById(R.id.tvDebug);

        startBluetooth();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.hide();
        }

        _hideHandler.removeCallbacks(_showPart2Runnable);
        _hideHandler.postDelayed(_hidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        _hideHandler.removeCallbacks(_hideRunnable);
        _hideHandler.postDelayed(_hideRunnable, delayMillis);
    }

    private void startBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(adapter == null) {
            addToDebug("Device doesn't support Bluetooth");
            return;
        }

        if(!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            addToDebug("Bluetooth is already enabled");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK)
                addToDebug("Bluetooth has been enabled");
            else if(resultCode == RESULT_CANCELED)
                addToDebug("Bluetooth enabling canceled");
        }
    }

    private void addToDebug(String text) {
        _tvDebug.setText(_tvDebug.getText() + text + "\n");
    }
}
