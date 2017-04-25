
// ElectricFlowClient.java --
//
// ElectricFlowClient.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ElectricFlowClient
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(ElectricFlowClient.class);

    //~ Instance fields --------------------------------------------------------

    private String electricFlowUrl;
    private String userName;
    private String password;
    private String workspaceDir;

    //~ Constructors -----------------------------------------------------------

    public ElectricFlowClient(
            String url,
            String name,
            String password)
    {
        this(url, name, password, "");
    }

    public ElectricFlowClient(
            String url,
            String name,
            String password,
            String workspaceDir)
    {
        this.electricFlowUrl = url;
        this.userName        = name;
        this.password        = password;
        this.workspaceDir    = workspaceDir;

        if (userName.isEmpty() || password.isEmpty()) {
            log.warn("User name and password should not be empty.");
        }

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
        SSLContext     sc;

        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                sc.getSocketFactory());
        }
        catch (NoSuchAlgorithmException | KeyManagementException e) {

            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
        }

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override public boolean verify(
                    String     hostname,
                    SSLSession session)
            {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    //~ Methods ----------------------------------------------------------------

    public String deployApplicationPackage(
            String group,
            String key,
            String version,
            String file)
        throws IOException
    {
        String             result          = "";
        String             requestEndpoint =
            "/rest/v1.0/createApplicationFromDeploymentPackage?request=createApplicationFromDeploymentPackage";
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("POST");

            JSONObject obj = new JSONObject();

            obj.put("artifactFileName", file);
            obj.put("artifactVersion", version);
            obj.put("artifactKey", key);
            obj.put("artifactGroup", group);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            byte[]       outputInBytes = obj.toString()
                                            .getBytes("UTF-8");
            OutputStream os            = conn.getOutputStream();

            os.write(outputInBytes);
            os.close();

            InputStream       inputStream = conn.getInputStream();
            InputStreamReader in          = new InputStreamReader(inputStream,
                    "UTF-8");

            br = new BufferedReader(in);

            String        output;
            StringBuilder buf = new StringBuilder();

            while ((output = br.readLine()) != null) {
                buf.append(output);
            }

            result = buf.toString();

            if (log.isDebugEnabled()) {
                log.debug("DeployApplication response: " + result);
            }

            if (conn.getResponseCode() != 201) {
                log.warn("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    public String runPipeline(
            String projectName,
            String pipelineName)
        throws Exception
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint =
            "/rest/v1.0/pipelines?pipelineName="
                + encodeURL(pipelineName)
                + "&projectName=" + encodeURL(projectName);
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("POST");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return myString.toString();
    }

    public String runPipeline(
            String    projectName,
            String    pipelineName,
            JSONArray additionalOptions)
        throws IOException
    {
        StringBuilder myString = new StringBuilder();

        // generating json
        JSONObject obj = new JSONObject();
        JSONArray  arr = new JSONArray();

        for (Object additionalOption : additionalOptions) {
            String parName  = ((JSONObject) additionalOption).getString(
                    "parameterName");
            String parValue = ((JSONObject) additionalOption).getString(
                    "parameterValue");

            // start
            JSONObject inner = new JSONObject();

            inner.put("actualParameterName", parName);
            inner.put("value", parValue);
            arr.add(inner);
            // end
        }

        obj.put("actualParameter", arr);
        obj.put("pipelineName", pipelineName);
        obj.put("projectName", projectName);

        if (log.isDebugEnabled()) {
            log.debug("Constructed JSON is: " + obj.toString());
        }

        // end of json
        String             requestEndpoint = "/rest/v1.0/pipelines";
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("POST");

            // adding body
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            byte[]       outputInBytes = obj.toString()
                                            .getBytes("UTF-8");
            OutputStream os            = conn.getOutputStream();

            os.write(outputInBytes);
            os.close();

            if (log.isDebugEnabled()) {

                // end of adding body
                log.debug("New pipeline run...");
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return myString.toString();
    }

    public String uploadArtifact(
            String  repo,
            String  name,
            String  version,
            String  path,
            boolean uploadDirectory)
        throws IOException, KeyManagementException, NoSuchAlgorithmException
    {
        String sessionId = this.getSessionId();

        // to make it working, this file should be installed:
        // http://swarm/reviews/137432/
        String requestURL = this.electricFlowUrl
                + "/commander/cgi-bin/publishArtifactAPI.cgi";
        String charset    = "UTF-8";

        // return sessionId;
        MultipartUtility multipart = new MultipartUtility(requestURL, charset);

        multipart.addFormField("artifactName", name);
        multipart.addFormField("artifactVersionVersion", version);
        multipart.addFormField("repositoryName", repo);
        multipart.addFormField("compress", "1");
        multipart.addFormField("commanderSessionId", sessionId);

        // here we're getting files from directory using wildcard:
        List<File> fileList = FileHelper.getFilesFromDirectoryWildcard(
                this.workspaceDir, path);

        if (log.isDebugEnabled()) {
            log.debug("File path: " + path);
        }

        for (File file : fileList) {

            // File file = new File(row);
            if (file.isDirectory()) {

                if (!uploadDirectory) {
                    continue;
                }

                // logic for dir here
                List<File> dirFiles = FileHelper.getFilesFromDirectory(file);

                for (File f : dirFiles) {
                    multipart.addFilePart("files", f);
                }
            }
            else {
                multipart.addFilePart("files", file);
            }
        }

        List<String> response = multipart.finish();

        // Debug.e(TAG, "SERVER REPLIED:");
        String resultLine = "";

        for (String line : response) {

            // Debug.e(TAG, "Upload Files Response:::" + line);
            // get your server response here.
            // responseString = line;
            resultLine = resultLine + line;

            if (log.isDebugEnabled()) {
                log.debug("Response: " + line);
            }
        }

        return resultLine;
    }

    private String encodeURL(String url)
        throws UnsupportedEncodingException
    {
        return URLEncoder.encode(url, "UTF-8")
                         .replaceAll("\\+", "%20");
    }

    public List<String> getArtifactRepositories()
        throws Exception
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint = "/rest/v1.0/repositories";
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (conn != null) {
                conn.disconnect();
            }

            if (br != null) {
                br.close();
            }
        }

        JSONObject   jsonObject   = JSONObject.fromObject(myString.toString());
        JSONArray    arr          = jsonObject.getJSONArray("repository");
        List<String> repositories = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            String repositoryName = arr.getJSONObject(i)
                                       .getString("repositoryName");

            repositories.add(repositoryName);

            if (log.isDebugEnabled()) {
                log.debug("Repository name: " + repositoryName);
            }
        }

        return repositories;
    }

    public String getElectricFlowUrl() {
        return electricFlowUrl;
    }

    // https://wsphere/rest/v1.0/projects/Jenkins/pipelines/SImplePipeline
    private HttpsURLConnection getConnection(String endpoint)
        throws IOException
    {
        URL url = new URL(this.electricFlowUrl + "/" + endpoint);

        if (log.isDebugEnabled()) {
            log.debug("Endpoint: " + url.toString());
        }

        HttpsURLConnection conn       = (HttpsURLConnection)
            url.openConnection();
        String             authString = this.userName + ":" + this.password;

        // String encoded =
        // Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        // //Java 8
        byte[] encodedBytes = Base64.encodeBase64(authString.getBytes("UTF-8"));
        String encoded      = new String(encodedBytes, StandardCharsets.UTF_8);

        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setRequestProperty("Accept", "application/json");

        return conn;
    }

    public List<String> getPipelineFormalParameters(String pipelineName)
        throws Exception
    {
        StringBuilder myString         = new StringBuilder();
        List<String>  formalParameters = new ArrayList<>();
        String        pipelineId       = this.getPipelineIdByName(pipelineName);

        if (!pipelineId.isEmpty()) {
            String             requestEndpoint =
                "/rest/v1.0/objects?request=findObjects";
            HttpsURLConnection conn            = null;
            BufferedReader     br              = null;

            try {
                conn = this.getConnection(requestEndpoint);
                conn.setRequestMethod("PUT");

                JSONObject obj    = new JSONObject();
                JSONObject filter = new JSONObject();

                filter.put("operator", "equals");
                filter.put("propertyName", "container");
                filter.put("operand1", "pipeline-" + pipelineId);
                obj.put("filter", filter);
                obj.put("objectType", "formalParameter");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                if (log.isDebugEnabled()) {
                    log.debug("Formal params json: " + obj.toString());
                }

                byte[]       outputInBytes = obj.toString()
                                                .getBytes("UTF-8");
                OutputStream os            = conn.getOutputStream();

                os.write(outputInBytes);
                os.close();

                if (conn.getResponseCode() != 200) {
                    log.warn("Failed : HTTP error code : "
                            + conn.getResponseCode());

                    return formalParameters;
                }

                br = new BufferedReader(new InputStreamReader(
                            (conn.getInputStream()), "UTF-8"));

                String output;

                while ((output = br.readLine()) != null) {
                    myString.append(output);
                }
            }
            catch (IOException e) {
                throw new IOException(e);
            }
            finally {

                if (br != null) {
                    br.close();
                }

                if (conn != null) {
                    conn.disconnect();
                }
            }

            // return myString;
            JSONObject jsonObject = JSONObject.fromObject(myString.toString());
            JSONArray  arr        = jsonObject.getJSONArray("object");

            for (int i = 0; i < arr.size(); i++) {
                String parameterName = arr.getJSONObject(i)
                                          .getJSONObject("formalParameter")
                                          .getString("formalParameterName");

                if (parameterName.equals("ec_stagesToRun")) {
                    continue;
                }

                formalParameters.add(parameterName);

                if (log.isDebugEnabled()) {
                    log.debug("Formal parameter: " + parameterName);
                }
            }
        }

        return formalParameters;
    }

    private String getPipelineIdByName(String pipelineName)
        throws Exception
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint =
            "/rest/v1.0/objects?request=findObjects";
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("PUT");

            JSONObject obj    = new JSONObject();
            JSONObject filter = new JSONObject();

            filter.put("operator", "equals");
            filter.put("propertyName", "pipelineName");
            filter.put("operand1", pipelineName);
            obj.put("filter", filter);
            obj.put("objectType", "pipeline");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            if (log.isDebugEnabled()) {
                log.debug("Pipeline id parameters json: " + obj.toString());
            }

            byte[]       outputInBytes = obj.toString()
                                            .getBytes("UTF-8");
            OutputStream os            = conn.getOutputStream();

            os.write(outputInBytes);
            os.close();

            if (conn.getResponseCode() != 200) {
                log.warn("Failed : HTTP error code : "
                        + conn.getResponseCode());

                return "";
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        // return myString;
        JSONObject jsonObject = JSONObject.fromObject(myString.toString());

        if (!jsonObject.isEmpty()) {
            JSONArray arr = jsonObject.getJSONArray("object");

            for (int i = 0; i < arr.size(); i++) {
                String pipelineName2 = arr.getJSONObject(i)
                                          .getJSONObject("pipeline")
                                          .getString("pipelineName");
                String pipelineId    = arr.getJSONObject(i)
                                          .getJSONObject("pipeline")
                                          .getString("pipelineId");

                if (pipelineName.equals(pipelineName2)) {
                    return pipelineId;
                }
            }
        }

        return "";
    }

    public String getPipelines(String projectName)
        throws IOException
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint = "/rest/v1.0/projects/"
                + encodeURL(projectName) + "/pipelines";
        BufferedReader     br              = null;
        HttpsURLConnection conn            = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("GET");

            if (log.isDebugEnabled()) {
                log.debug("GetPipelines ResponseCode: "
                        + conn.getResponseCode());
            }

            if (conn.getResponseCode() != 200) {
                log.warn("Failed : HTTP error code : "
                        + conn.getResponseCode());

                return "{}";
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return myString.toString();
    }

    public String getProjects()
        throws IOException
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint = "/rest/v1.0/projects";
        BufferedReader     br              = null;
        HttpsURLConnection conn            = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                log.warn("Failed : HTTP error code : "
                        + conn.getResponseCode());

                return "{}";
            }

            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return myString.toString();
    }

    // TODO: replace this function with proper logic.
    public String getSessionId()
        throws IOException
    {
        StringBuilder      myString        = new StringBuilder();
        String             requestEndpoint = "/rest/v1.0/sessions";
        HttpsURLConnection conn            = null;
        BufferedReader     br              = null;

        try {
            conn = this.getConnection(requestEndpoint);
            conn.setRequestMethod("POST");

            JSONObject requestObject = new JSONObject();

            requestObject.put("userName", this.userName);
            requestObject.put("password", this.password);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            byte[]       outputInBytes = requestObject.toString()
                                                      .getBytes("UTF-8");
            OutputStream os            = conn.getOutputStream();

            os.write(outputInBytes);
            os.close();
            br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream()), "UTF-8"));

            String output;

            while ((output = br.readLine()) != null) {
                myString.append(output);
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }
        finally {

            if (br != null) {
                br.close();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        JSONObject jsonObject = JSONObject.fromObject(myString.toString());

        // return "8OI8QO6NDXA4EYO9";
        return jsonObject.getString("sessionId");
    }
}

class DummyX509TrustManager
    implements X509TrustManager
{

    //~ Methods ----------------------------------------------------------------

    @Override public void checkClientTrusted(
            X509Certificate[] paramArrayOfX509Certificate,
            String            paramString)
        throws CertificateException { }

    @Override public void checkServerTrusted(
            X509Certificate[] paramArrayOfX509Certificate,
            String            paramString)
        throws CertificateException { }

    @Override public X509Certificate[] getAcceptedIssuers()
    {
        return null;
    }
}

class MultipartUtility
{

    //~ Static fields/initializers ---------------------------------------------

    private static final String LINE_FEED = "\r\n";

    //~ Instance fields --------------------------------------------------------

    private final Log          log          = LogFactory.getLog(this
                .getClass());
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
     * @param   requestURL
     * @param   charset
     *
     * @throws  NoSuchAlgorithmException  Exception
     * @throws  KeyManagementException
     * @throws  IOException
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
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override public boolean verify(
                    String     hostname,
                    SSLSession session)
            {
                return true;
            }
        };

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
     * @param   fieldName   name attribute in <input type="file" name="..." />
     * @param   uploadFile  a File to be uploaded
     *
     * @throws  IOException
     */
    public void addFilePart(
            String fieldName,
            File   uploadFile)
        throws IOException
    {
        String fileName = uploadFile.getName();

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
     * @throws  IOException
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

        // checks server's status code first
        int status = httpConn.getResponseCode();

        if (status == HttpsURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream(), "UTF-8"));
            String         line;

            while ((line = reader.readLine()) != null) {
                response.add(line);
            }

            reader.close();
            httpConn.disconnect();
        }
        else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }
}
