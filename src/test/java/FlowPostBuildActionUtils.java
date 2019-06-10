import java.util.Map;

public class FlowPostBuildActionUtils {

    public static String getPipelineStrCallRestApi(
            String configName,
            String urlPath,
            String httpMethod,
            String body,
            Map<String, String> parameters
    ) {
        StringBuilder parametersStr = new StringBuilder();

        parameters.forEach((key, value) -> parametersStr.append(
                "                [$class: 'Pair',\n" +
                        "                    key: '" + key + "',\n" +
                        "                    value: '" + value + "'\n" +
                        "                ],\n"));
        return "node{\n" +
                "        step([$class: 'ElectricFlowGenericRestApi',\n" +
                "            configuration: '" + configName + "',\n" +
                "            urlPath : '" + urlPath + "',\n" +
                "            httpMethod : '" + httpMethod + "',\n" +
                "            body : '" + body + "',\n" +
                "            parameters : [\n" +
                parametersStr +
                "            ]\n" +
                "    ])\n" +
                "}";
    }
}
