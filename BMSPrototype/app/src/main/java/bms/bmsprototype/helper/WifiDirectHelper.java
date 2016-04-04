/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bms.bmsprototype.helper;

import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.Method;

import bms.bmsprototype.socket.ClientSocketTask;
import bms.bmsprototype.socket.ServerSocketTask;
import bms.bmsprototype.socket.SocketTask;

public class WifiDirectHelper {

    private static final String TAG = "WifiDirectHelper";

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

    public static boolean openSocketConnection(WifiP2pInfo info, SocketTask.WifiDirectSocketEventListener listener) {
        if (!info.groupFormed)
            return false;

        if (info.isGroupOwner)
            new ServerSocketTask(listener).execute();
        else
            new ClientSocketTask(listener, info.groupOwnerAddress).execute();

        return true;
    }
}
