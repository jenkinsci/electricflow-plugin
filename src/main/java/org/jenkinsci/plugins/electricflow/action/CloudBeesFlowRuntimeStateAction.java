package org.jenkinsci.plugins.electricflow.action;

import hudson.Extension;
import hudson.model.InvisibleAction;

import java.util.ArrayList;
import java.util.List;

@Extension
public class CloudBeesFlowRuntimeStateAction extends InvisibleAction {
    protected List<String> notWaitingForBuildDataConfigs;
    public CloudBeesFlowRuntimeStateAction() {
        this.notWaitingForBuildDataConfigs = new ArrayList<>();
    }

    public void setNotWaitingForBuildData(String configName) {
        List<String> configs = this.getNotWaitingForBuildDataConfigs();
        if (!configs.contains(configName)) {
            configs.add(configName);
        }
    }
    public boolean isWaitingForBuildData(String configName) {
        List<String> configs = this.getNotWaitingForBuildDataConfigs();
        if (configs.contains(configName)) {
            return false;
        }
        return true;
    }
    public List<String> getNotWaitingForBuildDataConfigs() {
        return notWaitingForBuildDataConfigs;
    }
}
