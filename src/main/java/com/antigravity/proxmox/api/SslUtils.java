package com.antigravity.proxmox.api;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SslUtils {

    private static SSLSocketFactory trustAllSocketFactory;
    private static HostnameVerifier trustAllHostnameVerifier;

    public static synchronized SSLSocketFactory getTrustAllSocketFactory() {
        if (trustAllSocketFactory == null) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                trustAllSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("Failed to initialize trust-all SSL context", e);
            }
        }
        return trustAllSocketFactory;
    }

    public static synchronized HostnameVerifier getTrustAllHostnameVerifier() {
        if (trustAllHostnameVerifier == null) {
            trustAllHostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
        }
        return trustAllHostnameVerifier;
    }

    public static void applySslConfig(HttpsURLConnection connection, boolean trustAll) {
        if (trustAll) {
            connection.setSSLSocketFactory(getTrustAllSocketFactory());
            connection.setHostnameVerifier(getTrustAllHostnameVerifier());
        }
    }
}
