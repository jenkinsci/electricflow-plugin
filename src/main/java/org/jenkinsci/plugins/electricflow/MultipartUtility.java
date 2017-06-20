
// MultipartUtility.java --
//
// MultipartUtility.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MultipartUtility
{

    //~ Static fields/initializers ---------------------------------------------

    private static final String LINE_FEED = "\r\n";

    //~ Instance fields --------------------------------------------------------

    private final String       boundary;
    private HttpsURLConnection httpConn;
    private final String       charset;
    private OutputStream       outputStream;
    private PrintWriter        writer;

    //~ Constructors -----------------------------------------------------------

    /**
     * This constructor initializes a new HTTP POST request with content type is
     * set to multipart/form-data.
     *
     * @param   requestURL  URL for request
     * @param   charset     name of encodings
     *
     * @throws  NoSuchAlgorithmException  Exception
     * @throws  KeyManagementException    Exception
     * @throws  IOException               Exception
     */
    public MultipartUtility(
            String requestURL,
            String charset)
        throws NoSuchAlgorithmException, KeyManagementException, IOException
    {
        this.charset = charset;

        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }

                @Override public void checkClientTrusted(
                        X509Certificate[] certs,
                        String            authType) { }

                @Override public void checkServerTrusted(
                        X509Certificate[] certs,
                        String            authType) { }
            }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");

        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL(requestURL);

        httpConn = (HttpsURLConnection) url.openConnection();

        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type",
            "multipart/form-data; boundary=" + boundary);
        outputStream = httpConn.getOutputStream();
        writer       = new PrintWriter(new OutputStreamWriter(outputStream,
                    charset), true);
        // Create a trust manager that does not validate certificate chains
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Adds a upload file section to the request.
     *
     * @param   fieldName     name attribute in input type="file" name="..."
     * @param   uploadFile    a File to be uploaded
     * @param   workspaceDir  workspace dir
     *
     * @throws  IOException  exception
     */
    public void addFilePart(
            String fieldName,
            File   uploadFile,
            String workspaceDir)
        throws IOException
    {
        String absolutePath = uploadFile.getAbsolutePath();
        String fileName     = absolutePath.substring(workspaceDir.length(),
                absolutePath.length());

        fileName = fileName.replaceAll("\\\\", "/");

        writer.append("--")
              .append(boundary)
              .append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"")
              .append(fieldName)
              .append("\"; filename=\"")
              .append(fileName)
              .append("\"")
              .append(LINE_FEED);
        writer.append("Content-Type: ")
              .append(URLConnection.guessContentTypeFromName(fileName))
              .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary")
              .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        try(FileInputStream inputStream = new FileInputStream(uploadFile)) {
            byte[] buffer    = new byte[4096];
            int    bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            inputStream.close();
            writer.append(LINE_FEED);
            writer.flush();
        }
        catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Adds a form field to the request.
     *
     * @param  name   field name
     * @param  value  field value
     */
    public void addFormField(
            String name,
            String value)
    {
        writer.append("--")
              .append(boundary)
              .append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"")
              .append(name)
              .append("\"")
              .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=")
              .append(charset)
              .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value)
              .append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     *
     * @param  name   - name of the header field
     * @param  value  - value of the header field
     */
    public void addHeaderField(
            String name,
            String value)
    {
        writer.append(name)
              .append(": ")
              .append(value)
              .append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return  a list of Strings as response in case the server returned status
     *          OK, otherwise an exception is thrown.
     *
     * @throws  IOException  exception
     */
    public List<String> finish()
        throws IOException
    {
        List<String> response = new ArrayList<>();

        writer.append(LINE_FEED)
              .flush();
        writer.append("--")
              .append(boundary)
              .append("--")
              .append(LINE_FEED);
        writer.close();

        try {

            // checks server's status code first
            int status = httpConn.getResponseCode();

            if (status == HttpsURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpConn.getInputStream(),
                            "UTF-8"));
                String         line;

                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }

                reader.close();
                httpConn.disconnect();
            }
            else {
                throw new IOException("Server returned non-OK status: "
                        + status);
            }
        }
        finally {

            if (writer != null) {
                writer.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }
        }

        return response;
    }
}
