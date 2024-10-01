package org.jenkinsci.plugins.electricflow.data;

import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;

public class CloudBeesFlowSCMData {

    private List<CloudBeesFlowSCM> scmData;

    public CloudBeesFlowSCMData(List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets) {
        Iterator<ChangeLogSet<? extends ChangeLogSet.Entry>> itrChangeSet = changeSets.iterator();
        this.scmData = new ArrayList<>();
        while (itrChangeSet.hasNext()) {
            ChangeLogSet<? extends ChangeLogSet.Entry> str = itrChangeSet.next();
            List<Object> items = Arrays.asList(str.getItems());
            for (int i = 0; i < items.size(); i++) {
                Object cs = items.get(i);
                CloudBeesFlowSCM changeSet = CloudBeesFlowSCM.build(cs);
                this.scmData.add(changeSet);
            }
        }
    }

    public List<CloudBeesFlowSCM> getScmData() {
        return scmData;
    }

    public void setScmData(List<CloudBeesFlowSCM> scmData) {
        this.scmData = scmData;
    }
}
