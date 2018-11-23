package com.example.willieg.testserversocket;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class MainActivity extends AppCompatActivity {
    private static final boolean DEBUG = true;
    private static final String TAG = "testSocket";

    private SSLServerSocket mServerSocket;
    private Thread mThread;
    private boolean mKeepRunning = true;

    private TextView mIpView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mIpView = (TextView) findViewById(R.id.ip_addr);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startServer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startServer() {
        mThread = new Thread(() -> {
            try {
                mServerSocket = ServerSocketHelper.createServerSocket(this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setIpTextOnView();
                    }
                });

                while (mKeepRunning) {
                    Socket socket;
                    try {
                        socket = mServerSocket.accept();
                    } catch (IOException e) {
                        Log.e(TAG, "cannot accept IOException: " + e);
                        continue;
                    }
                    try {
                        if (DEBUG) Log.d(TAG, "run: accept returns");
                        if (socket == null) {
                            Log.e(TAG, "accepted socket is null");
                            continue;
                        }
                        if (!(socket instanceof SSLSocket)) {
                            Log.e(TAG, "socket is not SSLSocket " + socket);
                            continue;
                        }
                        SSLSocket sslSocket = (SSLSocket) socket;
                        try {
                            sslSocket.startHandshake();
                        } catch (IOException e) {
                            Log.e(TAG, "cannot startHandshake IOException: " + e);
                            continue;
                        }

                        TcpClient tcpClient;
                        try {
                            tcpClient = new TcpClient(sslSocket);
                        } catch (IOException e) {
                            Log.e(TAG, "cannot create TcpClient");
                            continue;
                        }
                        tcpClient.start(); // tcpClient owns sslSocket now
                        socket = null; // socket handed off to tcpClient, do not close
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "cannot close socket: IOException: " + e);
                            }
                        }
                    }
                }
            } finally {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "cannot close IOException: " + e);
                }
            }
        });
        mThread.start();
    }

    private void setIpTextOnView() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        mIpView.setText(ip);
    }
}