// ElectricFlowConfigurationManager.java --
//
// ElectricFlowConfigurationManager.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.util.List;

public class ElectricFlowConfigurationManager {

    // ~ Instance fields --------------------------------------------------------

    public List<Configuration> efConfigurations;

    // ~ Constructors -----------------------------------------------------------

    public ElectricFlowConfigurationManager() {
        efConfigurations = Utils.getConfigurations();
    }

    // ~ Methods ----------------------------------------------------------------

    public Configuration getConfigurationByName(String name) {

        for (Configuration cred : this.efConfigurations) {

            if (cred.getConfigurationName().equals(name)) {
                return cred;
            }
        }

        return null;
    }
}
