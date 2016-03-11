package bms.bmsprototype.utils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by gaua2616 on 2016-03-10.
 */
public class TransferThread extends Thread {
    InputStream in;
    OutputStream out;

    public TransferThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        //Linux kernel (at least the version which is baked into Android) has 64k buffer limit for a pipe.
        byte[] buf = new byte[8192];
        int len;

        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),
                    "Exception transferring Stream", e);
        }
    }
}