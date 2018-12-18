package org.jenkinsci.plugins.electricflow;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class RelaxedSSLContext {
    // trust manager that does not validate certificate
    final static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    //No need to implement.
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    //No need to implement.
                }
            }
    };

    // hostname verifier for which all hosts valid
    final public static HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static SSLContext getInstance() throws KeyManagementException, NoSuchAlgorithmException {
        return getInstance("SSL");
    }

    // get a 'Relaxed' SSLContext with no trust store (all certificates are valid)
    public static SSLContext getInstance(String protocol) throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sc = SSLContext.getInstance(protocol);
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc;
    }
}