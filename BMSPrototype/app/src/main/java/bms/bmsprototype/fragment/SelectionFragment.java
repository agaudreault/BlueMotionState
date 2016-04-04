package bms.bmsprototype.fragment;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import bms.bmsprototype.activity.MainActivity;
import bms.bmsprototype.R;
import bms.bmsprototype.helper.WifiDirectHelper;
import bms.bmsprototype.socket.SocketMessageReader;
import bms.bmsprototype.socket.SocketTask;

public class SelectionFragment extends BaseFragment {

    public static final String TAG = "SelectionFragment";
    private static final String WIFI_P2P_INFO = "bms.bmsprototype.fragment.SelectionFragment.wifi_p2p_info";
    private static final String DEVICES_NAME = "bms.bmsprototype.fragment.SelectionFragment.devices_name";
    private static final int PORT_MESSAGING = 8888;


    private MainActivity _parentActivity;
    private WifiP2pInfo _info;
    private String _devicesName;

    private Socket _messageSocket;

    private TextView _tvConnectedDeviceName;
    private EditText _editTextMessage;

    /**
     * Create a new instance of PairingFragment
     */
    public static SelectionFragment newInstance(WifiP2pInfo info, String devicesName) {
        SelectionFragment f = new SelectionFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(DEVICES_NAME, devicesName);
        args.putParcelable(WIFI_P2P_INFO, info);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _parentActivity = (MainActivity) getActivity();
        new TestTask().execute();

        _devicesName = getArguments().getString(DEVICES_NAME);
        _info = getArguments().getParcelable(WIFI_P2P_INFO);
        if(!WifiDirectHelper.openSocketConnection(_info, PORT_MESSAGING, new SocketTask.WifiDirectSocketEventListener() {
            @Override
            public void onSocketConnected(Socket socket) {
                _messageSocket = socket;

                new Thread(new SocketMessageReader(new SocketMessageReader.EventListener() {
                    @Override
                    public void onMessageReceived(String message) {
                        if(message != null)
                            _parentActivity.addToDebug("Message Received: " + message);
                    }
                }, _messageSocket)).start();
            }
        }))
            Log.d(TAG, "Group is not formed. Cannot connect socket");
    }

    @Override
    public void clean() {
        try {
            if(_messageSocket != null && !_messageSocket.isClosed())
                _messageSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.clean();
    }

    @Override
    public void onDestroy() {
        try {
            if(_messageSocket != null && !_messageSocket.isClosed())
                _messageSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        _tvConnectedDeviceName = (TextView)_parentActivity.findViewById(R.id.tvConnectedDeviceName);
        _tvConnectedDeviceName.setText(_devicesName);
        _editTextMessage = (EditText)_parentActivity.findViewById(R.id.etxtMessage);

        Button playbackActionButton = (Button) _parentActivity.findViewById(R.id.playbackActionButton);
        Button streamingActionButton = (Button) _parentActivity.findViewById(R.id.streamingActionButton);
        final Button sendMessageButton = (Button) _parentActivity.findViewById(R.id.btnSendMessage);

        _editTextMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                hideKeyboard(v);
                sendMessageButton.requestFocus();
                sendMessageButton.performClick();
                return true;
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(v);
                if(_editTextMessage.getText().length() <= 0)
                    return;

                sendMessage(_editTextMessage.getText().toString());
                _editTextMessage.setText("");
            }
        });

        playbackActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackActionOnClick();
            }
        });
        streamingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamingActionOnClick();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
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

    @Override
    public void onPause() {
        super.onPause();
    }

    private void streamingActionOnClick() {
        Toast.makeText(_parentActivity, "move to streaming (pairing)", Toast.LENGTH_LONG).show();
        _parentActivity.moveToStreaming();
    }

    private void playbackActionOnClick() {
        Toast.makeText(_parentActivity, "move to playback (pairing)", Toast.LENGTH_LONG).show();
        _parentActivity.moveToPlayback();
    }

    private void sendMessage(String message) {
        if(!isSocketStateValid(_messageSocket, true))
            return;

        try {
            PrintWriter printWriter = new PrintWriter(_messageSocket.getOutputStream(), true);
            printWriter.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputManager = (InputMethodManager) _parentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private boolean isSocketStateValid(Socket socket, boolean showToast)
    {
        String msg = null;

        if(socket == null){
            msg = "The socket is not created. Are you both using the application ?";
        } else if(!socket.isConnected()){
            msg = "The socket is disconnected.";
        }

        if(showToast)
            Toast.makeText(_parentActivity, msg, Toast.LENGTH_LONG).show();

        return msg == null;
    }

}
