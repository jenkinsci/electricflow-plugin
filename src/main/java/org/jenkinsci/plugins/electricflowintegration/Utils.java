
// Utils.java --
//
// Utils.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.util.ArrayList;
import java.util.List;

import hudson.util.ListBoxModel;

import jenkins.model.GlobalConfiguration;

public class Utils
{

    //~ Methods ----------------------------------------------------------------

    public static ListBoxModel fillCredentialItems()
    {
        ListBoxModel m = new ListBoxModel();

        m.add("Select configuration", "");

        for (Configuration cred : getCredentials()) {
            m.add(cred.getCredentialName(), cred.getCredentialName());
        }

        return m;
    }

    public static Configuration getCredentialByName(String name)
    {

        for (Configuration cred : getCredentials()) {

            if (cred.getCredentialName()
                    .equals(name)) {
                return cred;
            }
        }

        return null;
    }

    public static List<Configuration> getCredentials()
    {
        ElectricFlowGlobalConfiguration cred = GlobalConfiguration.all()
                                                                  .get(
                                                                      ElectricFlowGlobalConfiguration.class);

        if (cred != null) {
            return cred.efConfigurations;
        }

        return new ArrayList<>();
    }
}
