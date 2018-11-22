package com.example.willieg.testserversocket;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

// Ref: https://www.jianshu.com/p/2c9820bed794
class ServerSocketHelper {
    private static final String TAG = "ServerSocketHelper";
    private static final int PORT_NUMBER = 7777;

    public static SSLServerSocket createServerSocket(Context ctx) {
        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        KeyManager[] keyManagers = new KeyManager[0];
        TrustManager[] trustManagers = new TrustManager[0];
        try {
            keyManagers = createKeyManagers(ctx, "kserver.p12",
                    "qqqqqqqq", "server");
            trustManagers = createTrustManagers();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
        }
        try {
            context.init(keyManagers, trustManagers, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        SSLServerSocketFactory serverSocketFactory = context.getServerSocketFactory();

        SSLServerSocket serverSocket = null;
        try {
            serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(PORT_NUMBER, 3);
            serverSocket.setWantClientAuth(true); // Server wants a certificate, but deals with not getting one
        } catch (IOException e) {
            Log.e(TAG, "createServerSocket: Cannot create server socket IOException: " + e);
        }

        if (serverSocket == null) {
            Log.e(TAG, "run: Cannot createServerSocket");
        }

        Log.d(TAG, "mServerSocket created");
        return serverSocket;
    }

    private static KeyManager[] createKeyManagers(Context ctx, String keyStoreFileName,
                                                  String keyStorePassword, String alias)
            throws IOException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, java.security.cert.CertificateException {
        final String KEY_STORE_TYPE_P12 = "PKCS12";
        InputStream inputStream = ctx.getResources().getAssets().open(keyStoreFileName);
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_P12);
        keyStore.load(inputStream, keyStorePassword.toCharArray());

        KeyManager[] managers;
        if (alias != null) {
            managers = new KeyManager[]{new AliasKeyManager(keyStore, alias, keyStorePassword)};
        } else {
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword == null ?
                    null : keyStorePassword.toCharArray());
            managers = keyManagerFactory.getKeyManagers();
        }
        return managers;
    }

    private static TrustManager[] createTrustManagers() {
        return new TrustManager[]{new PermissiveTrustManager()};
    }

    private static class PermissiveTrustManager implements X509TrustManager {
        private static final String TAG = "PermissiveTrustManager";

        public PermissiveTrustManager() {
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public void checkClientTrusted(X509Certificate[] certificates, String string)
                throws java.security.cert.CertificateException {
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String string)
                throws java.security.cert.CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}

// Ref: https://www.jianshu.com/p/2c9820bed794
class AliasKeyManager implements X509KeyManager {
    private static final String TAG = "AliasKeyManager";

    private KeyStore _ks;
    private String _alias;
    private String _password;

    public AliasKeyManager(KeyStore ks, String alias, String password) {
        _ks = ks;
        _alias = alias;
        _password = password;
    }

    @Override
    public String chooseClientAlias(String[] str, Principal[] principal, Socket socket) {
        return _alias;
    }

    @Override
    public String chooseServerAlias(String str, Principal[] principal, Socket socket) {
        return _alias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            java.security.cert.Certificate[] certificates = this._ks.getCertificateChain(alias);
            if (certificates == null) {
                throw new FileNotFoundException("no certificate found for alias:" + alias);
            }
            X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
            System.arraycopy(certificates, 0, x509Certificates, 0, certificates.length);
            return x509Certificates;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String[] getClientAliases(String str, Principal[] principal) {
        return new String[]{_alias};
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        try {
            return (PrivateKey) _ks.getKey(alias, _password == null ? null : _password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String[] getServerAliases(String str, Principal[] principal) {
        return new String[]{_alias};
    }
}

