import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.LinkedList;

public class MockFlowUtils {

    public static final String MOCK_FLOW_CONFIG_NAME = "mockFlowConfig";
    public static final String MOCK_FLOW_PROTOCOL = "https";
    public static final String MOCK_FLOW_HOST = "localhost";
    public static final String MOCK_FLOW_PORT = "8443";
    public static final String MOCK_FLOW_ENDPOINT = MOCK_FLOW_PROTOCOL + "://" + MOCK_FLOW_HOST + ":" + MOCK_FLOW_PORT;
    public static final String MOCK_FLOW_USER = "user";
    public static final String MOCK_FLOW_PASSWORD = "password";
    public static final String MOCK_FLOW_REST_API_URI_PATH = "/rest/v1.0";

    public static void createDefaultMockFlowConfiguration(JenkinsRule jenkinsRule) {
        ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration = (ElectricFlowGlobalConfiguration) jenkinsRule.getInstance().getDescriptorByName("org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

        electricFlowGlobalConfiguration.efConfigurations = new LinkedList<>();

        electricFlowGlobalConfiguration
                .getConfigurations()
                .forEach(item ->
                        System.out.println("before: " + item.getConfigurationName()));

        Configuration configuration = new Configuration(MOCK_FLOW_CONFIG_NAME,
                MOCK_FLOW_ENDPOINT,
                MOCK_FLOW_USER,
                MOCK_FLOW_PASSWORD,
                MOCK_FLOW_REST_API_URI_PATH,
                true);

        electricFlowGlobalConfiguration.efConfigurations.add(configuration);
        electricFlowGlobalConfiguration.save();

        electricFlowGlobalConfiguration
                .getConfigurations()
                .forEach(item ->
                        System.out.println("after: " + item.getConfigurationName()));
    }
}
