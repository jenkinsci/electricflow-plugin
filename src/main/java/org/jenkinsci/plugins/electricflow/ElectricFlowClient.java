package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.FileHelper.getPublishArtifactWorkspaceOnMaster;
import static org.jenkinsci.plugins.electricflow.HttpMethod.GET;
import static org.jenkinsci.plugins.electricflow.HttpMethod.POST;
import static org.jenkinsci.plugins.electricflow.HttpMethod.PUT;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;

public class ElectricFlowClient {

  // ~ Static fields/initializers ---------------------------------------------

  private static final Log log = LogFactory.getLog(ElectricFlowClient.class);
  private static final String CHARSET = "UTF-8";
  public static final String JENKINS_BUILD_ASSOCIATION_TYPE = "triggeredByJenkins";
  public static final String BUILD_TRIGGER_SOURCE = "Jenkins";

  // ~ Instance fields --------------------------------------------------------

  private String electricFlowUrl;
  private String userName;
  private String password;
  private String apiVersion;
  private boolean ignoreSslConnectionErrors;
  private List<Release> releasesList = new ArrayList<>();
  private EnvReplacer envReplacer;

  public ElectricFlowClient(String configurationName, EnvReplacer envReplacer) {
    this(configurationName);
    this.envReplacer = envReplacer;
  }

  @Deprecated
  public ElectricFlowClient(String configurationName, String workspaceDir) {
    this(configurationName);
  }

  public ElectricFlowClient(String configurationName) {
    Configuration cred = Utils.getConfigurationByName(configurationName);

    if (cred != null) {
      electricFlowUrl = cred.getElectricFlowUrl();
      userName = cred.getElectricFlowUser();
      password = Secret.fromString(cred.getElectricFlowPassword()).getPlainText();
      ignoreSslConnectionErrors = cred.getIgnoreSslConnectionErrors();

      String electricFlowApiVersion = cred.getElectricFlowApiVersion();

      apiVersion = electricFlowApiVersion != null ? electricFlowApiVersion : "";
    }
  }

  public ElectricFlowClient(
      String url,
      String name,
      String password,
      String apiVersion,
      boolean ignoreSslConnectionErrors) {
    this.electricFlowUrl = url;
    this.userName = name;
    this.password = password;
    this.apiVersion = apiVersion;
    this.ignoreSslConnectionErrors = ignoreSslConnectionErrors;

    if (userName.isEmpty() || password.isEmpty()) {
      log.warn("User name and password should not be empty.");
    }
  }

  // ~ Methods ----------------------------------------------------------------

  public String deployApplicationPackage(String group, String key, String version, String file)
      throws IOException {
    String requestEndpoint =
        "/createApplicationFromDeploymentPackage?request=createApplicationFromDeploymentPackage";
    JSONObject obj = new JSONObject();

    obj.put("artifactFileName", file);
    obj.put("artifactVersion", version);
    obj.put("artifactKey", key);
    obj.put("artifactGroup", group);

    return runRestAPI(requestEndpoint, POST, obj.toString());
  }

  public String runPipeline(String projectName, String pipelineName) throws Exception {
    String requestEndpoint =
        "/pipelines?pipelineName="
            + Utils.encodeURL(pipelineName)
            + "&projectName="
            + Utils.encodeURL(projectName);

    return runRestAPI(requestEndpoint, POST);
  }

  public String runPipeline(String projectName, String pipelineName, JSONArray additionalOptions)
      throws IOException {
    JSONObject obj = new JSONObject();
    JSONArray parameters =
        getParameters(additionalOptions, "actualParameterName", "parameterName", "parameterValue");

    obj.put("actualParameter", parameters);
    obj.put("pipelineName", pipelineName);
    obj.put("projectName", projectName);

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
      String environmentName,
      JSONArray actualParameters)
      throws IOException {
    JSONObject obj = new JSONObject();

    obj.put("projectName", projectName);
    obj.put("applicationName", applicationName);
    obj.put("processName", processName);
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
        getParameters(
            pipelineParameters, "pipelineParameterName", "parameterName", "parameterValue"));

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

  public String runRestAPI(
      String urlPath, HttpMethod httpMethod, String body, List<Pair> parameters)
      throws IOException {
    StringBuilder result = new StringBuilder();
    JSONObject obj = new JSONObject();

    if (!urlPath.startsWith("/")) {
      urlPath = "/" + urlPath;
    }

    HttpsURLConnection conn = this.getConnection(apiVersion + urlPath);

    conn.setRequestMethod(httpMethod.name());
    conn.setUseCaches(false);
    conn.setDoInput(true);
    conn.setDoOutput(true);

    if (this.getIgnoreSslConnectionErrors()) {
      try {
        conn.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
      } catch (KeyManagementException | NoSuchAlgorithmException e) {
        if (log.isDebugEnabled()) {
          log.debug(e.getMessage(), e);
        }
      }
      conn.setHostnameVerifier(RelaxedSSLContext.allHostsValid);
    }

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
        InputStream stream =
            (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299)
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

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader((conn.getInputStream()), CHARSET))) {
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

  public JSONObject setCIBuildDetails(CIBuildDetail details) throws IOException {
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

    // here we're getting files from directory using wildcard:
    List<File> fileList =
        FileHelper.getFilesFromDirectoryWildcard(build, listener, workspace, path, true, true);

    if (log.isDebugEnabled()) {
      log.debug("File path: " + path);
    }

    String uploadWorkspace = getPublishArtifactWorkspaceOnMaster(build).getRemote();

    return uploadArtifact(fileList, uploadWorkspace, repo, name, version, uploadDirectory);
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
    String requestURL = this.electricFlowUrl + "/commander/cgi-bin/publishArtifactAPI.cgi";

    MultipartUtility multipart =
        new MultipartUtility(requestURL, CHARSET, this.getIgnoreSslConnectionErrors());

    multipart.addFormField("artifactName", name);
    multipart.addFormField("artifactVersionVersion", version);
    multipart.addFormField("repositoryName", repo);
    multipart.addFormField("compress", "1");
    multipart.addFormField("commanderSessionId", sessionId);

    for (File file : fileList) {
      if (file.isDirectory()) {

        if (!uploadDirectory) {
          continue;
        }

        List<File> dirFiles = FileHelper.getFilesFromDirectory(file);

        for (File f : dirFiles) {
          multipart.addFilePart("files", f, uploadWorkspace);
        }
      } else {
        multipart.addFilePart("files", file, uploadWorkspace);
      }
    }

    List<String> response = multipart.finish();

    String resultLine = "";

    for (String line : response) {

      resultLine = resultLine + line;

      if (log.isDebugEnabled()) {
        log.debug("Response: " + line);
      }
    }

    return resultLine;
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

  private HttpsURLConnection getConnection(String endpoint) throws IOException {
    URL url = new URL(this.electricFlowUrl + endpoint);

    if (log.isDebugEnabled()) {
      log.debug("Endpoint: " + url.toString());
    }

    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
    String authString = this.userName + ":" + this.password;

    //
    byte[] encodedBytes = Base64.encodeBase64(authString.getBytes(CHARSET));
    String encoded = new String(encodedBytes, StandardCharsets.UTF_8);

    conn.setRequestProperty("Authorization", "Basic " + encoded);
    conn.setRequestProperty("Accept", "application/json");

    return conn;
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

  public List<String> getFormalParameters(
      String projectName, String applicationName, String applicationProcessName)
      throws IOException {
    String endpoint =
        "/projects/"
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

  public List<String> getProcedureFormalParameters(String projectName, String procedureName)
      throws IOException {
    String endpoint =
        "/projects/"
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

  public List<String> getPipelineFormalParameters(String projectName, String pipelineName)
      throws Exception {
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

    filter.put("operator", "equals");
    filter.put("propertyName", "container");
    filter.put("operand1", "pipeline-" + pipelineId);
    obj.put("filter", filter);
    obj.put("objectType", "formalParameter");

    String result = runRestAPI(requestEndpoint, PUT, obj.toString());
    JSONObject jsonObject = JSONObject.fromObject(result);
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

  private String getPipelineId(String projectName, String pipelineName) throws Exception {
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

    if (!jsonObject.isEmpty()) {
      JSONArray arr = jsonObject.getJSONArray("object");

      for (int i = 0; i < arr.size(); i++) {
        String pipelineName2 =
            arr.getJSONObject(i).getJSONObject("pipeline").getString("pipelineName");
        String pipelineId = arr.getJSONObject(i).getJSONObject("pipeline").getString("pipelineId");

        if (pipelineName.equals(pipelineName2)) {
          return pipelineId;
        }
      }
    }

    return "";
  }

  public String getPipelines(String projectName) throws IOException {
    String requestEndpoint = "/projects/" + Utils.encodeURL(projectName) + "/pipelines";

    return runRestAPI(requestEndpoint, GET);
  }

  public JSONObject getProcess(String projectName, String applicationName, String processName)
      throws IOException {
    String endpoint =
        "/projects/"
            + Utils.encodeURL(projectName)
            + "/applications/"
            + Utils.encodeURL(applicationName)
            + "/processes/"
            + Utils.encodeURL(processName);
    String jsonResult = runRestAPI(endpoint, GET);

    return JSONObject.fromObject(jsonResult);
  }

  public List<String> getProcesses(String projectName, String applicationName) throws IOException {
    String endpoint =
        "/projects/"
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

  public Release getRelease(String configuration, String projectName, String releaseName)
      throws Exception {

    for (Release release : releasesList) {

      if (release.getConfiguration().equals(configuration)
          && release.getProjectName().equals(projectName)
          && release.getReleaseName().equals(releaseName)) {
        return release;
      }
    }

    getReleases(configuration, projectName);

    if (!releaseName.isEmpty()) {
      return getRelease(configuration, projectName, releaseName);
    }

    return null;
  }

  public List<String> getReleaseNames(String configuration, String projectName) {
    return releasesList.stream()
        .filter(
            release ->
                release.getConfiguration().equals(configuration)
                    && release.getProjectName().equals(projectName))
        .map(Release::getReleaseName)
        .collect(Collectors.toList());
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

      JSONObject stages = releaseObject.getJSONObject("stages");
      if (!stages.isEmpty()) {
        JSONArray stagesArray = stages.getJSONArray("stage");

        if (stagesArray != null && !stagesArray.isEmpty()) {
          List<String> stagesList = new ArrayList<>();

          for (int j = 0; j < stagesArray.size(); j++) {
            String stageName = stagesArray.getJSONObject(j).getString("name");
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

    requestObject.put("userName", this.userName);
    requestObject.put("password", this.password);

    String result = runRestAPI(requestEndpoint, POST, requestObject.toString());
    JSONObject jsonObject = JSONObject.fromObject(result);

    return jsonObject.getString("sessionId");
  }
}
