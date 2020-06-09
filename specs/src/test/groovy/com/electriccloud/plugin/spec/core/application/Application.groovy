package com.electriccloud.plugin.spec.core.application

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.spec.PluginSpockTestSupport

class Application implements ResponseDecorator {

    @Lazy(soft = true)
    Map dslObject = { _retrieveApplicationDetails() }()

    @Lazy
    ArrayList<Process> processes = { _retrieveProcesses() }()

    final String projectName
    final String name

    Application(String projectName, String name) {
        this.projectName = projectName
        this.name = name
    }

    int getProcessRunsCount(String processName) {
        Process pr = findProcess(processName)
        if (pr == null)
            throw new RuntimeException("There is no process '${processName}' in application '${name}'")

        ArrayList<ProcessRun> processRuns = pr.getRuns()
        return processRuns.size()
    }

    Process findProcess(String processName) {
        return processes.find({ it['processName'].equals(processName) })
    }

    private Map _retrieveApplicationDetails() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getApplication(
                projectName: '${projectName}',
                 applicationName: '${name}'
            )
        """)

        return response['application'] as Map
    }

    private ArrayList<Process> _retrieveProcesses() {
        Map response = (new PluginSpockTestSupport()).dsl("""
            getProcesses(
                projectName: '${projectName}',
                applicationName: '${name}'
            )
        """)

        ArrayList<Map> responseObjects = response['process']

        ArrayList<Process> result = new ArrayList<>()
        for (Map processMap : responseObjects) {
            Process pr = new Process(this, processMap['processName'])
            result.add(pr)
        }

        return result
    }

}
