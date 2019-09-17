package org.jenkinsci.plugins.electricflow.models;

import org.jenkinsci.plugins.electricflow.Pair;

import java.util.List;
import java.util.stream.Collectors;

public interface CallRestApiModel {

    public String getConfiguration();

    public String getUrlPath();

    public String getHttpMethod();

    public List<Pair> getParameters();

    public String getBody();

    public default String getEnvVarNameForResult() {
        return "";
    }

    public default boolean isEnvVarNameForResultSet() {
        return getEnvVarNameForResult() != null && !getEnvVarNameForResult().isEmpty();
    }

    public default String getSummary() {
        return "Configuration: " + getConfiguration() + "; " +
                "URL Path: " + getUrlPath() + "; " +
                "HTTP Method: " + getHttpMethod() + "; " +
                "Parameters: [" + getParameters().stream().map(it-> it.getKey() + ": " + it.getValue()).collect(Collectors.joining(";")) + "]; " +
                "Body: " + getBody() + "; " +
                (isEnvVarNameForResultSet() ? "" : "Environment variable name for storing result: " + getEnvVarNameForResult());
    }
}
