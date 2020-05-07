package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;

public class CloudBeesFlowPipeline implements ExtensionPoint {

  protected String stageName;
  protected String result;
  protected String reason;
  protected long duration;
  protected long timestamp;
  protected String logs;

  public CloudBeesFlowPipeline() {}

  public static List<CloudBeesFlowPipeline> build(Run<?, ?> run) {
    List<CloudBeesFlowPipeline> result = new ArrayList<>();
    ExtensionList.lookup(CloudBeesFlowPipeline.class);
    ExtensionList<CloudBeesFlowPipeline> makers = ExtensionList.lookup(CloudBeesFlowPipeline.class);
    for (CloudBeesFlowPipeline m : makers) {
      result = m.generate(run);
      if (result != null) {
        return result;
      }
    }
    return result;
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();

    if (this.getStageName() != null) {
      json.put("stageName", this.getStageName());
    }
    if (this.getResult() != null) {
      json.put("result", this.getResult());
    }
    if (this.getReason() != null) {
      json.put("reason", this.getReason());
    }
    json.put("duration", this.getDuration());
    json.put("timestamp", this.getTimestamp());
    json.put("logs", this.getLogs());

    return json;
  }

  public List<CloudBeesFlowPipeline> generate(Run<?, ?> run) {
    return new ArrayList<>();
  }

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getLogs() {
    return logs;
  }

  public void setLogs(String logs) {
    this.logs = logs;
  }
}
