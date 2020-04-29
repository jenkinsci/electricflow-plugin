package org.jenkinsci.plugins.electricflow;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RelaxedSSLContext {

  // hostname verifier for which all hosts valid
  public static final HostnameVerifier allHostsValid = (hostname, session) -> true;

  // trust manager that does not validate certificate
  static final TrustManager[] trustAllCerts =
      new TrustManager[]{
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
              // No need to implement.
            }

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
              // No need to implement.
            }
          }
      };

  public static SSLContext getInstance() throws KeyManagementException, NoSuchAlgorithmException {
    return getInstance("SSL");
  }

  // get a 'Relaxed' SSLContext with no trust store (all certificates are valid)
  public static SSLContext getInstance(String protocol)
      throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sc = SSLContext.getInstance(protocol);
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    return sc;
  }
}
