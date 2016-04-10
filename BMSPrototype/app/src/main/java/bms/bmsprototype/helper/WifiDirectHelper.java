package bms.bmsprototype.helper;

import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.Method;
import java.net.Socket;

import bms.bmsprototype.socket.ClientSocketTask;
import bms.bmsprototype.socket.ServerSocketTask;
import bms.bmsprototype.socket.SocketTask;

public class WifiDirectHelper {

    private static final String TAG = "WifiDirectHelper";

    /**
     * Delete all persistent groups on the device
     * @param manager
     * @param channel
     */
    public static void deletePersistentGroups(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(manager, channel, netid, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open a socket on the specified port with the Wifi Direct group info.
     * If the device is owner of the group, it starts a ServerSocket, a Socket otherwise.
     * @param info Informations about the group of the device
     * @param port Port to use for the socket connection
     * @param listener Listener to call when the socket connection throws events
     * @return The task created to open the socket connection
     */
    public static SocketTask openSocketConnection(WifiP2pInfo info, int port, SocketTask.WifiDirectSocketEventListener listener) {
        if (!info.groupFormed)
            return null;

        SocketTask task;

        if (info.isGroupOwner)
            task = new ServerSocketTask(listener, port);
        else
            task = new ClientSocketTask(listener, port, info.groupOwnerAddress);

        task.execute();
        return task;
    }

    /**
     * Checks if a socket in non-null and connected
     * @param socket The socket to validate
     * @return If the socket is valid or not
     */
    public static boolean isSocketValid(Socket socket) {
        return socket != null && socket.isConnected();
    }
}
