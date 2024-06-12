package org.jenkinsci.plugins.electricflow.triggerRelease;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class TriggerReleaseIsStagesChecked {
    private static final Log log = LogFactory.getLog(TriggerReleaseIsStagesChecked.class);

    public static String getCheckedValue(String parameters, String startingStage, Object stageOptions) {
        return isStagesChecked(parameters, startingStage, stageOptions);
    }
    public static String getCheckedValue(String parameters, String startingStage, String stageOptions) {
        return isStagesChecked(parameters, startingStage, stageOptions);
    }
    public static String isStagesChecked(String parameters, String startingStage, Object stageOptions) {
        String checkedVal = (String) stageOptions;
        if (StringUtils.isEmpty((String) stageOptions)) {
            if (!startingStage.isEmpty()){
                checkedVal = "startingStage";
            }else {
                try {
                    JSONObject jsonObject = new JSONObject(parameters);
                    JSONArray stages = jsonObject.getJSONObject("release").getJSONArray("stages");
                    for (int i = 0; i < stages.length(); i++) {
                        String stageValue = stages.getJSONObject(i).get("stageValue").toString();
                        if (stageValue.equals("true")) {
                            checkedVal = "stagesToRun";
                        }
                    }
                    checkedVal = (checkedVal == null || !checkedVal.equals("stagesToRun")) ? "runAllStages" : checkedVal;
                } catch (Exception e) {
                    log.info(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return checkedVal;
    }
}
