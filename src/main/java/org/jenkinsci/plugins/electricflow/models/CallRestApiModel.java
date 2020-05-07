package org.jenkinsci.plugins.electricflow.models;

import java.util.List;
import java.util.stream.Collectors;
import org.jenkinsci.plugins.electricflow.Credential;
import org.jenkinsci.plugins.electricflow.EnvReplacer;
import org.jenkinsci.plugins.electricflow.Pair;

public interface CallRestApiModel {

  String getConfiguration();

  Credential getOverrideCredential();

  String getUrlPath();

  String getHttpMethod();

  List<Pair> getParameters();

  default List<Pair> getParameters(EnvReplacer envReplacer) {
    return getParameters().stream()
        .map(it -> new Pair(it.getKey(), envReplacer.expandEnv(it.getValue())))
        .collect(Collectors.toList());
  }

  String getBody();

  default String getEnvVarNameForResult() {
    return "";
  }

  default boolean isEnvVarNameForResultSet() {
    return getEnvVarNameForResult() != null && !getEnvVarNameForResult().isEmpty();
  }

  default String getSummary() {
    return "Configuration: "
        + getConfiguration()
        + "; "
        + "URL Path: "
        + getUrlPath()
        + "; "
        + "HTTP Method: "
        + getHttpMethod()
        + "; "
        + "Parameters: ["
        + getParameters().stream()
            .map(it -> it.getKey() + ": " + it.getValue())
            .collect(Collectors.joining(";"))
        + "]; "
        + "Body: "
        + getBody()
        + "; "
        + (isEnvVarNameForResultSet()
            ? ""
            : "Environment variable name for storing result: " + getEnvVarNameForResult());
  }
}
