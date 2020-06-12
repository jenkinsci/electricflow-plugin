package com.electriccloud.plugin.spec.core.pipeline

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.spec.PluginSpockTestSupport

class Pipeline implements ResponseDecorator {

    final String name
    final String projectName

    String id

    ArrayList<PipelineRun> pipelineRuns

    @Lazy
    Map dslObject = { _retrievePipelineDetails() }()

    Pipeline(String id) {
        this.id = id
        this.name = dslObject['pipelineName']
        this.projectName = dslObject['projectName']
    }

    Pipeline(String projectName, String name) {
        this.projectName = projectName
        this.name = name
    }

    String getId() {
        if (this.id == null) {
            this.id = dslObject['pipelineId']
        }
        return id
    }

    ArrayList<PipelineRun> getPipelineRuns() {
        if (this.pipelineRuns == null) {
            pipelineRuns = _retrievePipelineRuns()
        }
        return this.pipelineRuns
    }

    PipelineRun getLastRun() {
        if (this.getPipelineRuns() != null && pipelineRuns.size()) {
            return pipelineRuns.last()
        }
        return null
    }

    HashMap<String, Object> _retrievePipelineDetails() {
        // Assuming that pipeline was created with either id/name+projectName
        if (id != null) {
            return _retrievePipelineDetailsBy(id)
        } else {
            return _retrievePipelineDetailsBy(projectName, name)
        }
    }

    protected HashMap<String, Object> _retrievePipelineDetailsBy(String id) {
        def response = (new PluginSpockTestSupport()).dsl("""
            findObjects(
                objectType: 'pipeline',
                maxResults: 1,
                filters: [ 
                   [ propertyName: 'pipelineId', operator: 'equals', operand1: '${id}' ] 
                ]
            )
        """)

        // Take project and name from the first result
        def pipelineObject = response['object'].find() { it ->
            return it['pipeline']['pipelineId'] == id
        }
        assert pipelineObject != null: "Can't find pipeline with id: '$id'"

        String pipelineName = pipelineObject['pipeline']['pipelineName']
        String projectName = pipelineObject['pipeline']['projectName']

        // Pipeline can be a ReleasePipeline
        // TODO: refactor 'object' processing and have this in a subclass
        if (projectName == null && pipelineObject['pipeline']['releaseProjectName']) {
            projectName = pipelineObject['pipeline']['releaseProjectName']
        }

        def result = this._retrievePipelineDetailsBy(projectName, pipelineName)
        assert result['pipelineId'] == id

        return result
    }

    protected HashMap _retrievePipelineDetailsBy(String projectName, String name) {
        def result = (new PluginSpockTestSupport()).dsl("""
                getPipeline(
                    projectName: '$projectName', 
                    pipelineName: '${name}'
                )
            """)
        return result['pipeline'] as HashMap<String, Object>
    }

    void refresh() {
        this.pipelineRuns = _retrievePipelineRuns()
    }

    private ArrayList<PipelineRun> _retrievePipelineRuns() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getPipelineRuntimes( projectName: '$projectName', pipelineName: '$name' )
        """)

        ArrayList<Map> runtimes = (ArrayList<Map>) response['flowRuntime']

        ArrayList<PipelineRun> result = new ArrayList<>()
        for (Map runtime : runtimes) {
            result.add(new PipelineRun(runtime, this))
        }

        return result.reverse(true)
    }

    /**
     * Pipeline part in the flow runtime name
     * @return
     */
    String getPartName() {
        return name
    }
}
