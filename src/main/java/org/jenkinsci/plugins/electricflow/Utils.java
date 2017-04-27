
// Utils.java --
//
// Utils.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.GlobalConfiguration;

public class Utils
{

    //~ Methods ----------------------------------------------------------------

    public static String encodeURL(String url)
        throws UnsupportedEncodingException
    {
        return URLEncoder.encode(url, "UTF-8")
                         .replaceAll("\\+", "%20");
    }

    public static ListBoxModel fillConfigurationItems()
    {
        ListBoxModel m = new ListBoxModel();

        m.add("Select configuration", "");

        for (Configuration cred : getConfigurations()) {
            m.add(cred.getConfigurationName(), cred.getConfigurationName());
        }

        return m;
    }

    public static FormValidation validateValueOnEmpty(
            String value,
            String fieldName)
    {

        if (!value.isEmpty()) {
            return FormValidation.ok();
        }
        else {
            return FormValidation.warning(fieldName
                    + " field should not be empty.");
        }
    }

    public static Configuration getConfigurationByName(String name)
    {

        for (Configuration cred : getConfigurations()) {

            if (cred.getConfigurationName()
                    .equals(name)) {
                return cred;
            }
        }

        return null;
    }

    public static List<Configuration> getConfigurations()
    {
        ElectricFlowGlobalConfiguration cred = GlobalConfiguration.all()
                                                                  .get(
                                                                      ElectricFlowGlobalConfiguration.class);

        if (cred != null && cred.efConfigurations != null) {
            return cred.efConfigurations;
        }

        return new ArrayList<>();
    }
}
