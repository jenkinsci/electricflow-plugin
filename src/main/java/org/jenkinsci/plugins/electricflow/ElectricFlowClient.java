package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.FileHelper.getPublishArtifactWorkspaceOnMaster;
import static org.jenkinsci.plugins.electricflow.HttpMethod.GET;
import static org.jenkinsci.plugins.electricflow.HttpMethod.POST;
import static org.jenkinsci.plugins.electricflow.HttpMethod.PUT;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.istack.NotNull;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetJobStatusResponseData;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetPipelineRuntimeDetailsResponseData;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.StageResponseData;

public class ElectricFlowClient {

    // ~ Static fields/initializers ---------------------------------------------
    private static final Log log = LogFactory.getLog(ElectricFlowClient.class);
    private static final String CHARSET = "UTF-8";

    // ~ Instance fields --------------------------------------------------------
    private String electricFlowUrl;
    private String userName;
    private String password;
    private String secret;
    private String apiVersion;
    private boolean ignoreSslConnectionErrors;
    private List<Release> releasesList = new ArrayList<>();
    private EnvReplacer envReplacer;

    public ElectricFlowClient(
            String url, String name, String password, String apiVersion, boolean ignoreSslConnectionErrors) {
        this.electricFlowUrl = url;
        this.userName = name;
        this.password = password;
        this.apiVersion = apiVersion;
        this.ignoreSslConnectionErrors = ignoreSslConnectionErrors;

        if (userName.isEmpty() || password.isEmpty()) {
            log.warn("User name and password should not be empty.");
        }
    }

    public ElectricFlowClient(String url, String secret, String apiVersion, boolean ignoreSslConnectionErrors) {
        this.electricFlowUrl = url;
        this.secret = secret;
        this.apiVersion = apiVersion;
        this.ignoreSslConnectionErrors = ignoreSslConnectionErrors;

        if (secret.isEmpty()) {
            log.warn("Secret should not be empty.");
        }
    }

    // ~ Methods ----------------------------------------------------------------

    public String deployApplicationPackage(String group, String key, String version, String file) throws IOException {
        String requestEndpoint =
                "/createApplicationFromDeploymentPackage?request=createApplicationFromDeploymentPackage";
        JSONObject obj = new JSONObject();

        obj.put("artifactFileName", file);
        obj.put("artifactVersion", version);
        obj.put("artifactKey", key);
        obj.put("artifactGroup", group);

        return runRestAPI(requestEndpoint, POST, obj.toString());
    }

    public String runPipeline(
            String projectName,
            String pipelineName,
            String stageOption,
            String startingStage,
            List<String> stagesToRun,
            JSONArray additionalOptions)
            throws IOException, PluginException {
        JSONObject obj = new JSONObject();
        JSONArray parameters =
                getParameters(additionalOptions, "actualParameterName", "parameterName", "parameterValue");

        obj.put("actualParameter", parameters);
        obj.put("pipelineName", pipelineName);
        obj.put("projectName", projectName);

        if (stageOption == null || stageOption.isEmpty()) {
            log.info("Running pipeline with all stages selected (default option)");
        } else {
            switch (stageOption) {
                case "runAllStages":
                    log.info("Running pipeline with all stages selected");
                    break;
                case "startingStage":
                    log.info("Running pipeline with starting stage selected: " + startingStage);
                    obj.put("startingStage", startingStage);
                    break;
                case "stagesToRun":
                    if (stagesToRun.isEmpty()) {
                        log.info("Running pipeline with stages to run selected: "
                                + stagesToRun
                                + " (equals to running pipeline with all stages selected");
                    } else {
                        log.info("Running pipeline with stages to run selected: " + stagesToRun);
                    }
                    obj.put("stagesToRun", stagesToRun);
                    break;
                default:
                    throw new PluginException("Unexpected stage option: " + stageOption);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Constructed JSON is: " + obj.toString());
        }

        // end of json
        String requestEndpoint = "/pipelines";

        return runRestAPI(requestEndpoint, HttpMethod.POST, obj.toString());
    }

    public String runProcess(
            String projectName,
            String applicationName,
            String processName,
            String environmentProjectName,
            String environmentName,
            JSONArray actualParameters)
            throws IOException {
        JSONObject obj = new JSONObject();

        obj.put("projectName", projectName);
        obj.put("applicationName", applicationName);
        obj.put("processName", processName);
        if (environmentProjectName != null && !environmentProjectName.isEmpty()) {
            obj.put("environmentProjectName", environmentProjectName);
        }
        obj.put("environmentName", environmentName);
        obj.put(
                "actualParameter",
                getParameters(actualParameters, "actualParameterName", "actualParameterName", "value"));

        String requestEndpoint = "/jobs?request=runProcess";

        return runRestAPI(requestEndpoint, POST, obj.toString());
    }

    public String runProcedure(String projectName, String procedureName, JSONArray actualParameters)
            throws IOException {
        JSONObject obj = new JSONObject();

        obj.put("projectName", projectName);
        obj.put("procedureName", procedureName);
        obj.put(
                "actualParameter",
                getParameters(actualParameters, "actualParameterName", "actualParameterName", "value"));

        String requestEndpoint = "/jobs?request=runProcedure";

        return runRestAPI(requestEndpoint, POST, obj.toString());
    }

    public String runRelease(
            String projectName,
            String releaseName,
            List stagesToRun,
            String startingStage,
            JSONArray pipelineParameters)
            throws IOException {

        // generating json
        JSONObject obj = new JSONObject();

        obj.put("releaseName", releaseName);
        obj.put("projectName", projectName);

        if (!startingStage.isEmpty()) {
            obj.put("startingStage", startingStage);
        } else {
            obj.put("stagesToRun", stagesToRun);
        }

        obj.put(
                "pipelineParameter",
                getParameters(pipelineParameters, "pipelineParameterName", "parameterName", "parameterValue"));

        if (log.isDebugEnabled()) {
            log.debug("Constructed JSON is: " + obj.toString());
        }

        // end of json
        String requestEndpoint = "/releases";

        return runRestAPI(requestEndpoint, POST, obj.toString());
    }

    public String runRestAPI(String urlPath, HttpMethod httpMethod) throws IOException {
        return runRestAPI(urlPath, httpMethod, "", new ArrayList<>(0));
    }

    public String runRestAPI(String urlPath, HttpMethod httpMethod, String body) throws IOException {
        return runRestAPI(urlPath, httpMethod, body, new ArrayList<>(0));
    }

    public String runRestAPI(String urlPath, HttpMethod httpMethod, String body, List<Pair> parameters)
            throws IOException {
        StringBuilder result = new StringBuilder();
        JSONObject obj = new JSONObject();

        if (!urlPath.startsWith("/")) {
            urlPath = "/" + urlPath;
        }

        HttpURLConnection conn =
                getHttpURLConnection(apiVersion + urlPath, httpMethod, this.getIgnoreSslConnectionErrors());

        if (!GET.equals(httpMethod)) {
            byte[] outputInBytes = new byte[0];

            if (!parameters.isEmpty()) {

                for (Pair pair : parameters) {
                    obj.put(pair.getKey(), expandVariable(pair.getValue()));
                }

                outputInBytes = obj.toString().getBytes(CHARSET);
            } else if (!body.isEmpty()) {
                outputInBytes = body.getBytes(CHARSET);
            }

            if (outputInBytes.length != 0) {

                try (OutputStream outputStream = conn.getOutputStream()) {
                    outputStream.write(outputInBytes);
                    outputStream.close();
                }
            }
        }

        List<Integer> successCodes = new ArrayList<>();

        successCodes.add(200);
        successCodes.add(201);

        if (!successCodes.contains(conn.getResponseCode())) {
            try {
                InputStream stream = (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                if (stream != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(stream, CHARSET));
                    String output;
                    while ((output = br.readLine()) != null) {
                        result.append(output);
                    }
                } else {
                    log.info("Connection input or error stream is null");
                }
            } catch (IOException e) {
                log.error("Error on reading response body. Error: " + e.getMessage());
            } finally {
                conn.disconnect();
            }

            String errorMessage =
                    "Failed : HTTP error code : " + conn.getResponseCode() + ", " + conn.getResponseMessage();
            if (!result.toString().isEmpty()) {
                errorMessage += ", " + result.toString();
            }

            throw new RuntimeException(errorMessage);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), CHARSET))) {
            String output;

            while ((output = br.readLine()) != null) {
                result.append(output);
            }

            return result.toString();
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection getHttpURLConnection(
            @NotNull String urlPath, @NotNull HttpMethod httpMethod, boolean ignoreSsl) throws IOException {

        HttpURLConnection conn = this.getConnection(urlPath);

        conn.setRequestMethod(httpMethod.name());
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        if (ignoreSsl && conn instanceof HttpsURLConnection) {
            HttpsURLConnection sslConn = (HttpsURLConnection) conn;
            try {
                sslConn.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
            }
            sslConn.setHostnameVerifier(RelaxedSSLContext.allHostsValid);
        }

        return conn;
    }

    public JSONObject attachCIBuildDetails(CIBuildDetail details) throws IOException {
        String endpoint = "/ciBuildDetails?request=setCiBuildDetail";
        String result = runRestAPI(endpoint, POST, details.toJsonObject().toString());
        return JSONObject.fromObject(result);
    }

    public String uploadArtifact(
            Run<?, ?> build,
            TaskListener listener,
            String repo,
            String name,
            String version,
            String path,
            boolean uploadDirectory,
            FilePath workspace)
            throws IOException, KeyManagementException, NoSuchAlgorithmException, InterruptedException {

        PrintStream logger = listener.getLogger();
        // here we're getting files from directory using wildcard:
        List<File> fileList = FileHelper.getFilesFromDirectoryWildcard(build, listener, workspace, path, true, true);

        if (log.isDebugEnabled()) {
            log.debug("File path: " + path);
        }

        // Temporarily copying files from slave to master
        String uploadWorkspace = getPublishArtifactWorkspaceOnMaster(build).getRemote();

        logger.println("Uploading artifact to the repository");
        String result = uploadArtifact(fileList, uploadWorkspace, repo, name, version, uploadDirectory);
        logger.println("Upload result: " + result);

        // Removing temp
        try {
            FileHelper.removeTempDirectory(build);
            logger.println("Removed temporary directory " + uploadWorkspace);
        } catch (IOException ex) {
            logger.println("Failed to remove the temporary directory " + uploadWorkspace + "\n" + ex.getMessage());
        }

        return result;
    }

    public String uploadArtifact(
            List<File> fileList,
            String uploadWorkspace,
            String repo,
            String name,
            String version,
            boolean uploadDirectory)
            throws IOException, KeyManagementException, NoSuchAlgorithmException, InterruptedException {
        String sessionId = this.getSessionId();

        // to make it working, this file should be installed:
        // http://swarm/reviews/137432/
        String phpUrl = this.electricFlowUrl + "/commander/publishArtifact.php";
        String cgiUrl = this.electricFlowUrl + "/commander/cgi-bin/publishArtifactAPI.cgi";
        boolean isPhpEndpoint = checkIfEndpointReachable("/commander/publishArtifact.php");
        String requestURL = isPhpEndpoint ? phpUrl : cgiUrl;

        MultipartUtility multipart = new MultipartUtility(requestURL, CHARSET, this.getIgnoreSslConnectionErrors());

        multipart.addFormField("artifactName", name);
        multipart.addFormField("artifactVersionVersion", version);
        multipart.addFormField("repositoryName", repo);
        multipart.addFormField("compress", "1");
        multipart.addFormField("commanderSessionId", sessionId);

        for (File file : fileList) {
            multipart.addFilePart(isPhpEndpoint ? "files[]" : "files", file, uploadWorkspace);
        }

        List<String> response = multipart.finish();

        return response.stream()
                .peek(line -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Response: " + line);
                    }
                })
                .collect(Collectors.joining());
    }

    private boolean checkIfEndpointReachable(String destination) {

        try {
            HttpURLConnection httpURLConnection = getHttpURLConnection(destination, GET, true);
            int responseCode = httpURLConnection.getResponseCode();
            if (log.isDebugEnabled()) {
                log.debug("Response: " + responseCode);
            }

            // publishArtifact.php can return code 400.
            return responseCode == 400 || responseCode == 200;
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Connection error. URL: " + destination + ". " + e.getMessage());
            }

            return false;
        }
    }

    private String expandVariable(String var) {
        return envReplacer != null ? envReplacer.expandEnv(var) : var;
    }

    public List<String> getApplications(String projectName) throws IOException {
        String endpoint = "/projects/" + Utils.encodeURL(projectName) + "/applications";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("application")
                || !(jsonObject.get("application") instanceof JSONArray)) {
            return result;
        }

        JSONArray application = jsonObject.getJSONArray("application");

        for (int i = 0; i < application.size(); i++) {
            JSONObject applicationObject = application.getJSONObject(i);
            String applicationName = applicationObject.getString("applicationName");

            result.add(applicationName);
        }

        return result;
    }

    public List<String> getProcedures(String projectName) throws IOException {
        String endpoint = "/projects/" + Utils.encodeURL(projectName) + "/procedures";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("procedure")
                || !(jsonObject.get("procedure") instanceof JSONArray)) {
            return result;
        }

        JSONArray procedure = jsonObject.getJSONArray("procedure");

        for (int i = 0; i < procedure.size(); i++) {
            JSONObject procedureObject = procedure.getJSONObject(i);
            String procedureName = procedureObject.getString("procedureName");

            result.add(procedureName);
        }

        return result;
    }

    public List<String> getArtifactRepositories() throws Exception {
        String requestEndpoint = "/repositories";
        String result = runRestAPI(requestEndpoint, GET);
        JSONObject jsonObject = JSONObject.fromObject(result);
        JSONArray arr = jsonObject.getJSONArray("repository");
        List<String> repositories = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            String repositoryName = arr.getJSONObject(i).getString("repositoryName");

            repositories.add(repositoryName);

            if (log.isDebugEnabled()) {
                log.debug("Repository name: " + repositoryName);
            }
        }

        return repositories;
    }

    private HttpURLConnection getConnection(String endpoint) throws IOException {
        URL url = new URL(this.electricFlowUrl + endpoint);

        if (log.isDebugEnabled()) {
            log.debug("Endpoint: " + url.toString());
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (isUserNameAndPasswordCreds()) {
            String authString = this.userName + ":" + this.password;

            //
            byte[] encodedBytes = Base64.encodeBase64(authString.getBytes(CHARSET));
            String encoded = new String(encodedBytes, StandardCharsets.UTF_8);

            conn.setRequestProperty("Authorization", "Basic " + encoded);
            conn.setRequestProperty("Accept", "application/json");

            return conn;
        } else if (isSecretCreds()) {
            if (endpoint.contains("/loginSso?token=")) {
                return conn;
            } else {
                String jsonResult = runRestAPI("/loginSso?token=" + secret, POST, "{}");
                String sessionId = JSONObject.fromObject(jsonResult).getString("sessionId");
                conn.setRequestProperty("Cookie", "sessionId=" + sessionId);
                conn.setRequestProperty("Accept", "application/json");
                return conn;
            }
        }
        throw new RuntimeException("Credentials are not provided");
    }

    public boolean isUserNameAndPasswordCreds() {
        return userName != null && password != null && !userName.isEmpty() && !password.isEmpty();
    }

    public boolean isSecretCreds() {
        return secret != null && !secret.isEmpty();
    }

    public String getElectricFlowUrl() {
        return electricFlowUrl;
    }

    private boolean getIgnoreSslConnectionErrors() {
        return ignoreSslConnectionErrors;
    }

    public List<String> getEnvironments(String projectName) throws IOException {
        String endpoint = "/projects/" + Utils.encodeURL(projectName) + "/environments";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("environment")
                || !(jsonObject.get("environment") instanceof JSONArray)) {
            return result;
        }

        JSONArray environments = jsonObject.getJSONArray("environment");

        for (int i = 0; i < environments.size(); i++) {
            JSONObject environmentObject = environments.getJSONObject(i);
            String environment = environmentObject.getString("environmentName");

            result.add(environment);
        }

        return result;
    }

    public List<String> getFormalParameters(String projectName, String applicationName, String applicationProcessName)
            throws IOException {
        String endpoint = "/projects/"
                + Utils.encodeURL(projectName)
                + "/applications/"
                + Utils.encodeURL(applicationName)
                + "/processes/"
                + Utils.encodeURL(applicationProcessName)
                + "/formalParameters";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()) {
            return result;
        }

        JSONArray environments = jsonObject.getJSONArray("formalParameter");

        for (int i = 0; i < environments.size(); i++) {
            JSONObject environmentObject = environments.getJSONObject(i);
            String expansionDeferred = environmentObject.getString("expansionDeferred");

            if ("0".equals(expansionDeferred)) {
                String parameterName = environmentObject.getString("formalParameterName");

                result.add(parameterName);
            }
        }

        return result;
    }

    public List<String> getProcedureFormalParameters(String projectName, String procedureName) throws IOException {
        String endpoint = "/projects/"
                + Utils.encodeURL(projectName)
                + "/procedures/"
                + Utils.encodeURL(procedureName)
                + "/formalParameters";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("formalParameter")
                || !(jsonObject.get("formalParameter") instanceof JSONArray)) {
            return result;
        }

        JSONArray environments = jsonObject.getJSONArray("formalParameter");

        for (int i = 0; i < environments.size(); i++) {
            JSONObject environmentObject = environments.getJSONObject(i);
            String expansionDeferred = environmentObject.getString("expansionDeferred");

            if ("0".equals(expansionDeferred)) {
                String parameterName = environmentObject.getString("formalParameterName");

                result.add(parameterName);
            }
        }

        return result;
    }

    private JSONArray getParameters(
            JSONArray parameters, String argumentName, String parameterName, String parameterValue) {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < parameters.size(); i++) {
            JSONObject param = parameters.getJSONObject(i);
            String parName = param.getString(parameterName);
            String parValue = param.getString(parameterValue);
            JSONObject inner = new JSONObject();

            inner.put(argumentName, parName);
            inner.put("value", expandVariable(parValue));
            jsonArray.add(inner);
        }

        return jsonArray;
    }

    public List<String> getPipelineFormalParameters(String projectName, String pipelineName) throws Exception {
        List<String> formalParameters = new ArrayList<>();
        String pipelineId = this.getPipelineId(projectName, pipelineName);

        if (!pipelineId.isEmpty()) {
            return getPipelineFormalParameters(pipelineId);
        }

        return formalParameters;
    }

    public List<String> getPipelineFormalParameters(String pipelineId) throws Exception {
        List<String> formalParameters = new ArrayList<>();

        String requestEndpoint = "/objects?request=findObjects";
        JSONObject obj = new JSONObject();
        JSONObject filter = new JSONObject();
        JSONObject sort = new JSONObject();

        filter.put("operator", "equals");
        filter.put("propertyName", "container");
        filter.put("operand1", "pipeline-" + pipelineId);
        obj.put("filter", filter);
        sort.put("propertyName", "orderIndex");
        sort.put("order", "ascending");
        obj.put("sort", sort);
        obj.put("objectType", "formalParameter");

        String result = runRestAPI(requestEndpoint, PUT, obj.toString());
        JSONObject jsonObject = JSONObject.fromObject(result);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("object")
                || !(jsonObject.get("object") instanceof JSONArray)) {
            return formalParameters;
        }

        JSONArray arr = jsonObject.getJSONArray("object");
        for (int i = 0; i < arr.size(); i++) {
            String parameterName =
                    arr.getJSONObject(i).getJSONObject("formalParameter").getString("formalParameterName");

            if (parameterName.equals("ec_stagesToRun")) {
                continue;
            }

            formalParameters.add(parameterName);

            if (log.isDebugEnabled()) {
                log.debug("Formal parameter: " + parameterName);
            }
        }

        return formalParameters;
    }

    public List<String> getPipelineStagesNames(String projectName, String pipelineName) throws IOException {
        List<StageResponseData> stages = getPipelineStages(projectName, pipelineName);

        return stages.stream().map(StageResponseData::getStageName).collect(Collectors.toList());
    }

    public List<StageResponseData> getPipelineStages(String projectName, String pipelineName) throws IOException {
        String requestEndpoint =
                "/projects/" + Utils.encodeURL(projectName) + "/stages?pipelineName=" + Utils.encodeURL(pipelineName);
        String result = runRestAPI(requestEndpoint, GET);
        List<StageResponseData> stages = new ArrayList<>();
        for (Object object : JSONObject.fromObject(result).getJSONArray("stage")) {
            stages.add(new ObjectMapper().readValue(object.toString(), StageResponseData.class));
        }
        stages.sort(Comparator.comparing(StageResponseData::getIndex));
        return stages;
    }

    public String getPipelineId(String projectName, String pipelineName) throws Exception {
        String requestEndpoint = "/objects?request=findObjects";
        JSONObject obj = new JSONObject();
        JSONObject filterTop = new JSONObject();
        JSONArray filters = new JSONArray();
        JSONObject filterPerProject = new JSONObject();
        JSONObject filterPerPipeline = new JSONObject();

        filterPerProject.put("operator", "equals");
        filterPerProject.put("propertyName", "projectName");
        filterPerProject.put("operand1", projectName);

        filterPerPipeline.put("operator", "equals");
        filterPerPipeline.put("propertyName", "pipelineName");
        filterPerPipeline.put("operand1", pipelineName);

        filters.add(filterPerProject);
        filters.add(filterPerPipeline);

        filterTop.put("operator", "and");
        filterTop.put("filter", filters);

        obj.put("filter", filterTop);
        obj.put("objectType", "pipeline");

        String result = runRestAPI(requestEndpoint, PUT, obj.toString());
        JSONObject jsonObject = JSONObject.fromObject(result);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("object")
                || !(jsonObject.get("object") instanceof JSONArray)) {
            return "";
        }

        JSONArray arr = jsonObject.getJSONArray("object");

        for (int i = 0; i < arr.size(); i++) {
            String pipelineName2 =
                    arr.getJSONObject(i).getJSONObject("pipeline").getString("pipelineName");
            String pipelineId = arr.getJSONObject(i).getJSONObject("pipeline").getString("pipelineId");

            if (pipelineName.equals(pipelineName2)) {
                return pipelineId;
            }
        }

        return "";
    }

    public String getPipelines(String projectName) throws IOException {
        String requestEndpoint = "/projects/" + Utils.encodeURL(projectName) + "/pipelines";

        return runRestAPI(requestEndpoint, GET);
    }

    public JSONObject getProcess(String projectName, String applicationName, String processName) throws IOException {
        String endpoint = "/projects/"
                + Utils.encodeURL(projectName)
                + "/applications/"
                + Utils.encodeURL(applicationName)
                + "/processes/"
                + Utils.encodeURL(processName);
        String jsonResult = runRestAPI(endpoint, GET);

        return JSONObject.fromObject(jsonResult);
    }

    public List<String> getProcesses(String projectName, String applicationName) throws IOException {
        String endpoint = "/projects/"
                + Utils.encodeURL(projectName)
                + "/applications/"
                + Utils.encodeURL(applicationName)
                + "/processes";
        String jsonResult = runRestAPI(endpoint, GET);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSONObject.fromObject(jsonResult);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("process")
                || !(jsonObject.get("process") instanceof JSONArray)) {
            return result;
        }

        JSONArray processes = jsonObject.getJSONArray("process");

        for (int i = 0; i < processes.size(); i++) {
            JSONObject applicationObject = processes.getJSONObject(i);
            String process = applicationObject.getString("processName");

            result.add(process);
        }

        return result;
    }

    public String getProjects() throws IOException {
        String requestEndpoint = "/projects";

        return runRestAPI(requestEndpoint, GET);
    }

    public Release getRelease(String configuration, String projectName, String releaseName) throws Exception {

        Release cachedResult = getCachedRelease(configuration, projectName, releaseName);

        if (cachedResult != null) {
            return cachedResult;
        }

        // Renew list
        getReleases(configuration, projectName);

        return getCachedRelease(configuration, projectName, releaseName);
    }

    private Release getCachedRelease(String configuration, String projectName, String releaseName) {
        for (Release release : releasesList) {
            if (release.getConfiguration().equals(configuration)
                    && release.getProjectName().equals(projectName)
                    && release.getReleaseName().equals(releaseName)) {
                return release;
            }
        }
        return null;
    }

    public List<String> getReleaseNames(String configuration, String projectName) throws Exception {
        if (releasesList.size() == 0) {
            getReleases(configuration, projectName);
        }

        return releasesList.stream()
                .filter(release -> release.getConfiguration().equals(configuration)
                        && release.getProjectName().equals(projectName))
                .map(Release::getReleaseName)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getReleaseRuns(String conf, String projectName, String releaseName)
            throws Exception {

        Release release = getRelease(conf, projectName, releaseName);
        String pipelineName = release.getPipelineName();

        // Build the request
        String requestEndpoint =
                "/projects/" + Utils.encodeURL(projectName) + "/pipelines/" + Utils.encodeURL(pipelineName);

        requestEndpoint += "?request=getPipelineRuntimes&releaseId=" + release.getReleaseId();

        //    JSONObject obj = new JSONObject();
        //    obj.put("request", "");
        //    obj.put("releaseId", release.getReleaseId());
        //    String responseString = runRestAPI(requestEndpoint, PUT, obj.toString());
        String responseString = runRestAPI(requestEndpoint, PUT);

        try {
            JSONObject response = JSONObject.fromObject(responseString);

            if (!response.containsKey("flowRuntime") || !(response.get("flowRuntime") instanceof JSONArray)) {
                return new ArrayList<>(0);
            }

            JSONArray flowRuntimes = response.getJSONArray("flowRuntime");

            ArrayList<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < flowRuntimes.size(); i++) {
                result.add(flowRuntimes.getJSONObject(i));
            }

            return result;
        } catch (RuntimeException ex) {
            log.error("Failed to parse Flow server response:" + ex.getMessage());
            return null;
        }
    }

    public List<String> getReleases(String conf, String projectName) throws Exception {
        releasesList.clear();

        String requestEndpoint = "/projects/" + Utils.encodeURL(projectName) + "/releases";
        String result = runRestAPI(requestEndpoint, GET);
        JSONObject jsonObject = JSONObject.fromObject(result);

        if (jsonObject.isEmpty()
                || !jsonObject.containsKey("release")
                || !(jsonObject.get("release") instanceof JSONArray)) {
            return new ArrayList<>(0);
        }

        JSONArray releases = jsonObject.getJSONArray("release");

        for (int i = 0; i < releases.size(); i++) {
            JSONObject releaseObject = releases.getJSONObject(i);

            String gotReleaseId = releaseObject.getString("releaseId");
            String gotReleaseName = releaseObject.getString("releaseName");
            String gotPipelineName = releaseObject.getString("pipelineName");
            String gotPipelineId = releaseObject.getString("pipelineId");

            Release release = new Release(conf, projectName, gotReleaseName);
            release.setPipelineName(gotPipelineName);
            release.setPipelineId(gotPipelineId);
            release.setPipelineParameters(getPipelineFormalParameters(gotPipelineId));
            release.setReleaseId(gotReleaseId);

            // This can be missing if release wasn't run before
            if (releaseObject.containsKey("flowRuntimeId")) {
                release.setFlowRuntimeId(releaseObject.getString("flowRuntimeId"));
            }

            String stagesEndpoint = "/projects/" + Utils.encodeURL(projectName) + "/stages?pipelineName="
                    + Utils.encodeURL(gotPipelineName) + "&releaseName=" + Utils.encodeURL(gotReleaseName);
            String stagesResult = runRestAPI(stagesEndpoint, GET);
            JSONObject stagesObject = JSONObject.fromObject(stagesResult);
            if (!stagesObject.isEmpty()) {
                JSONArray stagesArray = stagesObject.getJSONArray("stage");

                if (stagesArray != null && !stagesArray.isEmpty()) {
                    List<String> stagesList = new ArrayList<>();

                    for (int j = 0; j < stagesArray.size(); j++) {
                        String stagePropertyName = stagesArray.getJSONObject(j).has("name") ? "name" : "stageName";
                        String stageName = stagesArray.getJSONObject(j).getString(stagePropertyName);
                        if (stagesList.contains(stageName)) {
                            continue;
                        }
                        stagesList.add(stageName);
                    }

                    release.setStartStages(stagesList);
                }
            }

            releasesList.add(release);
        }

        return getReleaseNames(conf, projectName);
    }

    public void testConnection() throws IOException {
        getSessionId();
    }

    public String getSessionId() throws IOException {
        String requestEndpoint = "/sessions";
        JSONObject requestObject = new JSONObject();

        if (this.userName != null) {
            requestObject.put("userName", this.userName);
            requestObject.put("password", this.password);
        }

        String result = runRestAPI(requestEndpoint, POST, requestObject.toString());
        JSONObject jsonObject = JSONObject.fromObject(result);

        return jsonObject.getString("sessionId");
    }

    public GetJobStatusResponseData getCdJobStatus(String cdJobId) throws IOException {
        String requestEndpoint = "/jobs/" + cdJobId + "?request=getJobStatus";
        String result = runRestAPI(requestEndpoint, GET);
        GetJobStatusResponseData getJobStatusResponseData = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue(result, GetJobStatusResponseData.class);
        getJobStatusResponseData.setContent(result);
        return getJobStatusResponseData;
    }

    public GetPipelineRuntimeDetailsResponseData getCdPipelineRuntimeDetails(String flowRuntimeId) throws IOException {
        String requestEndpoint = "/pipelineRuntimeDetails?request=getPipelineRuntimeDetails";
        String result = runRestAPI(requestEndpoint, PUT, "{\"flowRuntimeId\":[\"" + flowRuntimeId + "\"]}");
        GetPipelineRuntimeDetailsResponseData getPipelineRuntimeDetailsResponseData = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .readValue(
                        JSONObject.fromObject(result)
                                .getJSONArray("flowRuntime")
                                .getJSONObject(0)
                                .toString(),
                        GetPipelineRuntimeDetailsResponseData.class);
        getPipelineRuntimeDetailsResponseData.setContent(result);
        return getPipelineRuntimeDetailsResponseData;
    }
}
