package org.jenkinsci.plugins.electricflow.models;

import static org.jenkinsci.plugins.electricflow.Utils.expandParameters;

import java.util.ArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.EnvReplacer;

public class ReleaseRunParameters {

  final JSONObject release;
  final JSONArray stages;
  final JSONArray pipelineParameters;
  final ArrayList<String> stagesToRun;
  final String startingStage;

  public ReleaseRunParameters(EnvReplacer env, String parameters, String startStage) {
    release = JSONObject.fromObject(parameters).getJSONObject("release");
    pipelineParameters = JSONArray.fromObject(release.getString("parameters"));

    stages = JSONArray.fromObject(release.getString("stages"));
    stagesToRun = new ArrayList<>();
    if (startStage.isEmpty()) {
      for (int i = 0; i < stages.size(); i++) {
        JSONObject stage = stages.getJSONObject(i);
        if (stage.getString("stageName").length() > 0) {
          stagesToRun.add(stage.getString("stageName"));
        }
      }
    }

    this.startingStage = startStage;

    expandParameters(pipelineParameters, env);
  }

  public JSONObject getRelease() {
    return release;
  }

  public JSONArray getStages() {
    return stages;
  }

  public JSONArray getPipelineParameters() {
    return pipelineParameters;
  }

  public ArrayList<String> getStagesToRun() {
    return stagesToRun;
  }
}
