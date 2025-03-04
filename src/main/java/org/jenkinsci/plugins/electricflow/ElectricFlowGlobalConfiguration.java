// ElectricFlowGlobalConfiguration.java --
//
// ElectricFlowGlobalConfiguration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

@Extension
@Symbol("electricflow")
public class ElectricFlowGlobalConfiguration extends GlobalConfiguration {

    // ~ Instance fields --------------------------------------------------------

    public List<Configuration> configurations;

    @Deprecated
    private transient List<Configuration> efConfigurations;

    // ~ Constructors -----------------------------------------------------------

    public ElectricFlowGlobalConfiguration() {
        load();
    }

    // ~ Methods ----------------------------------------------------------------

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException {
        this.configurations = null;
        req.bindJSON(this, formData);
        save();

        return true;
    }

    public List<Configuration> getConfigurations() {
        return this.configurations;
    }

    @DataBoundSetter
    public void setConfigurations(List<Configuration> configurations) {
        this.configurations = configurations;
    }

    /*
     * This is required to transform the old efConfigurations to the new configurations
     */
    private Object readResolve() {
        if (efConfigurations != null) {
            this.configurations = efConfigurations;
        }
        return this;
    }
}
