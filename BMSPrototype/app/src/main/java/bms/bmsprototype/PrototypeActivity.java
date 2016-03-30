package bms.bmsprototype;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.net.Socket;
import java.util.ArrayList;

import bms.bmsprototype.utils.AvailableDevicesListAdapter;

public class PrototypeActivity extends WifiDirectActivity {

    private TextView _tvConnectedDeviceName;
    private EditText _etxtMessage;
    private Button _btnSendMessage;

    private TextView _tvDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.old_main_activity);

        _tvConnectedDeviceName = (TextView)findViewById(R.id.tvConnectedDeviceName);
        _etxtMessage = (EditText)findViewById(R.id.etxtMessage);
        _btnSendMessage = (Button)findViewById(R.id.btnSendMessage);

        _tvDebug = (TextView)findViewById(R.id.tvDebug);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void addToDebug(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tvDebug.setText(text + "\n" + _tvDebug.getText());
            }
        });
    }

    @Override
    public void onSocketConnected(Socket socket) {
        super.onSocketConnected(socket);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _etxtMessage.setEnabled(true);
                _btnSendMessage.setEnabled(true);
            }
        });
    }

    public void btnSendMessageOnClick(View view) {
        if(_etxtMessage.getText().length() <= 0)
            return;

        sendMessage(_etxtMessage.getText().toString());
        _etxtMessage.setText("");
    }

    @Override
    public void onMessageReceived(String message) {
        super.onMessageReceived(message);

        if(message != null)
            addToDebug("Message received : " + message);
    }
}
