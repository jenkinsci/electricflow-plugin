import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;
import static org.jvnet.hudson.test.JenkinsRule.getLog;

public class TestCallRestApiWithMockFlow {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().httpsPort(8443));

    @Before
    public void createFlowConfigurations() {
        MockFlowUtils.createDefaultMockFlowConfiguration(jenkinsRule);
    }

    @Test
    public void testCallRestApiWithParamsCreateProjectSucceed() throws Exception {
        String testName = "testCallRestApiWithParamsCreateProjectSucceed";

        String testUrlPath = "/projects";
        String testHttpMethod = "POST";

        String testParamKeyProjectName = "projectName";
        String testParamValueProjectName = "EC-TEST-Jenkins-1.00.00.01-" + testName;
        String testParamKeyDescription = "description";
        String testParamValueDescription = "Native Jenkins Test Project " + testName;

        Map<String, String> testParameters = new HashMap<>();
        testParameters.put(testParamKeyProjectName, testParamValueProjectName);
        testParameters.put(testParamKeyDescription, testParamValueDescription);

        String testBody = "";

        stubFor(
                post(urlPathMatching(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath))
                        .withBasicAuth(MockFlowUtils.MOCK_FLOW_USER, MockFlowUtils.MOCK_FLOW_PASSWORD)
                        .withHeader("Accept", equalTo("application/json"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withRequestBody(matching("\\{.*,.*\\}"))
                        .withRequestBody(containing("\"" + testParamKeyProjectName + "\":\"" + testParamValueProjectName + "\""))
                        .withRequestBody(containing("\"" + testParamKeyDescription + "\":\"" + testParamValueDescription + "\""))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"someJsonParamKey1\":\"SomeJsonParamValue1\"," +
                                        "\"someJsonParamKey2\":\"SomeJsonParamValue2\"}")));

        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(
                FlowPostBuildActionUtils.getPipelineStrCallRestApi(
                        MockFlowUtils.MOCK_FLOW_CONFIG_NAME,
                        testUrlPath,
                        testHttpMethod,
                        testBody,
                        testParameters
                ), true));

        WorkflowRun build = ExtraTestJenkinsUtils.buildAndAssertSuccess(project, jenkinsRule);

        verify(exactly(1), postRequestedFor(urlEqualTo(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath)));

        String log = getLog(build);
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey1\".*:.*\"SomeJsonParamValue1\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey2\".*:.*\"SomeJsonParamValue2\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
    }

    @Test
    public void testCallRestApiWithBodyCreateProjectSucceed() throws Exception {
        String testName = "testCallRestApiWithBodyCreateProjectSucceed";

        String testUrlPath = "/projects";
        String testHttpMethod = "POST";

        String testParamKeyProjectName = "projectName";
        String testParamValueProjectName = "EC-TEST-Jenkins-1.00.00.01-" + testName;
        String testParamKeyDescription = "description";
        String testParamValueDescription = "Native Jenkins Test Project " + testName;

        Map<String, String> testParameters = new HashMap<>();

        String testBody = "{\"" + testParamKeyProjectName + "\":\"" + testParamValueProjectName + "\"," +
                "\"" + testParamKeyDescription + "\":\"" + testParamValueDescription + "\"}";

        stubFor(
                post(urlPathMatching(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath))
                        .withBasicAuth(MockFlowUtils.MOCK_FLOW_USER, MockFlowUtils.MOCK_FLOW_PASSWORD)
                        .withHeader("Accept", equalTo("application/json"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withRequestBody(equalTo(testBody))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"someJsonParamKey1\":\"SomeJsonParamValue1\"," +
                                        "\"someJsonParamKey2\":\"SomeJsonParamValue2\"}")));

        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(
                FlowPostBuildActionUtils.getPipelineStrCallRestApi(
                        MockFlowUtils.MOCK_FLOW_CONFIG_NAME,
                        testUrlPath,
                        testHttpMethod,
                        testBody,
                        testParameters
                ), true));

        WorkflowRun build = ExtraTestJenkinsUtils.buildAndAssertSuccess(project, jenkinsRule);

        verify(exactly(1), postRequestedFor(urlEqualTo(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath)));

        String log = getLog(build);
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey1\".*:.*\"SomeJsonParamValue1\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey2\".*:.*\"SomeJsonParamValue2\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
    }

    @Test
    public void testCallRestApiParamsOverrideBody() throws Exception {
        String testName = "testCallRestApiParamsOverrideBody";

        String testUrlPath = "/projects";
        String testHttpMethod = "POST";

        String testParamKeyProjectName = "projectName";
        String testParamValueProjectName = "EC-TEST-Jenkins-1.00.00.01-" + testName;
        String testParamKeyDescription = "description";
        String testParamValueDescription = "Native Jenkins Test Project " + testName;

        Map<String, String> testParameters = new HashMap<>();
        testParameters.put(testParamKeyProjectName, testParamValueProjectName);
        testParameters.put(testParamKeyDescription, testParamValueDescription);

        String testBody = "{\"NO_MATCH\":\"NO_MATCH\"," +
                "\"NO_MATCH\":\"NO_MATCH\"}";

        stubFor(
                post(urlPathMatching(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath))
                        .withBasicAuth(MockFlowUtils.MOCK_FLOW_USER, MockFlowUtils.MOCK_FLOW_PASSWORD)
                        .withHeader("Accept", equalTo("application/json"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withRequestBody(matching("\\{.*,.*\\}"))
                        .withRequestBody(containing("\"" + testParamKeyProjectName + "\":\"" + testParamValueProjectName + "\""))
                        .withRequestBody(containing("\"" + testParamKeyDescription + "\":\"" + testParamValueDescription + "\""))
                        .withRequestBody(notMatching(".*NO_MATCH.*"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"someJsonParamKey1\":\"SomeJsonParamValue1\"," +
                                        "\"someJsonParamKey2\":\"SomeJsonParamValue2\"}")));

        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(
                FlowPostBuildActionUtils.getPipelineStrCallRestApi(
                        MockFlowUtils.MOCK_FLOW_CONFIG_NAME,
                        testUrlPath,
                        testHttpMethod,
                        testBody,
                        testParameters
                ), true));

        WorkflowRun build = ExtraTestJenkinsUtils.buildAndAssertSuccess(project, jenkinsRule);

        verify(exactly(1), postRequestedFor(urlEqualTo(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath)));

        String log = getLog(build);
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey1\".*:.*\"SomeJsonParamValue1\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
        assertTrue(Pattern
                .compile(".*\"someJsonParamKey2\".*:.*\"SomeJsonParamValue2\".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
    }

    @Test
    public void testCallRestApiWithParamsCreateProjectFailed() throws Exception {
        String testName = "testCallRestApiWithParamsCreateProjectFailed";

        String testUrlPath = "/projects";
        String testHttpMethod = "POST";

        String testParamKeyProjectName = "projectName";
        String testParamValueProjectName = "EC-TEST-Jenkins-1.00.00.01-" + testName;
        String testParamKeyDescription = "description";
        String testParamValueDescription = "Native Jenkins Test Project " + testName;

        Map<String, String> testParameters = new HashMap<>();
        testParameters.put(testParamKeyProjectName, testParamValueProjectName);
        testParameters.put(testParamKeyDescription, testParamValueDescription);

        String testBody = "";

        String errorResponseBody = "{\"errorKey1\":\"errorValue1\",\"errorKey2\":\"errorValue2\"}";

        stubFor(
                post(urlPathMatching(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath))
                        .withBasicAuth(MockFlowUtils.MOCK_FLOW_USER, MockFlowUtils.MOCK_FLOW_PASSWORD)
                        .withHeader("Accept", equalTo("application/json"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withRequestBody(matching("\\{.*,.*\\}"))
                        .withRequestBody(containing("\"" + testParamKeyProjectName + "\":\"" + testParamValueProjectName + "\""))
                        .withRequestBody(containing("\"" + testParamKeyDescription + "\":\"" + testParamValueDescription + "\""))
                        .willReturn(aResponse()
                                .withStatus(409)
                                .withStatusMessage("Conflict")
                                .withHeader("Content-Type", "application/json")
                                .withBody(errorResponseBody)));

        WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(
                FlowPostBuildActionUtils.getPipelineStrCallRestApi(
                        MockFlowUtils.MOCK_FLOW_CONFIG_NAME,
                        testUrlPath,
                        testHttpMethod,
                        testBody,
                        testParameters
                ), true));

        WorkflowRun build = ExtraTestJenkinsUtils.buildAndAssertFailure(project, jenkinsRule);

        verify(exactly(1), postRequestedFor(urlEqualTo(MockFlowUtils.MOCK_FLOW_REST_API_URI_PATH + testUrlPath)));

        String log = getLog(build);
        assertTrue(Pattern
                .compile(".*409.*Conflict.*" + Pattern.quote(errorResponseBody) + ".*", Pattern.DOTALL)
                .matcher(log)
                .matches()
        );
    }
}
