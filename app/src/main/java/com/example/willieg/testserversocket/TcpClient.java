
package com.example.willieg.testserversocket;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;

public class TcpClient {
    private static final String TAG = TcpClient.class.getSimpleName();
    private static final boolean DEBUG = true;
    private boolean mKeepRunning = true;
    private Thread mReadingThread = null;
    private final SSLSocket mSocket;
    private final InputStream mInputStream;
    private final byte[] mInputBuffer = new byte[10000];

    public TcpClient(SSLSocket socket) throws IOException {
        mSocket = socket;
        synchronized (mSocket) {
            mInputStream = mSocket.getInputStream();
        }
    }

    public void start() {
        mReadingThread = new Thread(() -> {
            while (mKeepRunning) {
                int len = 0;
                try {
                    len = mInputStream.read(mInputBuffer);
                    if (len < 0) {
                        break;
                    }
                    byte[] byteArray = Arrays.copyOfRange(mInputBuffer, 0, len);
                    Log.d(TAG, "receive bytes len: " + len);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            if (DEBUG) Log.d(TAG, "closing the mSocket");
            try {
                synchronized (mSocket) {
                    mSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Cannot close mSocket IOException: " + e);
            }
            if (DEBUG) Log.d(TAG, "returning");
        });
        mReadingThread.start();
    }
}