package com.electriccloud.plugin.spec.core

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.spec.PluginSpockTestSupport

/**
 * Wrapper for the different job properties calls
 */
class Job implements ResponseDecorator {

    static JenkinsHelper jh = new JenkinsHelper()

    String jobId

    // This allows casting dsl runProcedure() result to Job
    ArrayList<String> advisoryMessages

    @Lazy
    Map<String, Object> properties = { _retrieveJobProperties() }()

    Map<String, Object> dslObject = null

    @Lazy
    Map<String, Object> details = { _retrieveJobDetails() }()

    @Lazy
    Map<Integer, Map<String, String>> outputParameters = { _retrieveOutputParameters() }()

    @Lazy
    String outcome = { dslObject['outcome'] }()

    @Lazy
    ArrayList jobSteps = { (ArrayList) getDetails()['jobStep'] }()

    @Lazy
    String logs = {
        try {
            _retrieveJobLogs()
        } catch (RuntimeException ex) {
            "Failed to get job logs"
        }
    }()

    @Lazy
    String status = { dslObject['status'] }()

    Job(String jobId) {
        this.jobId = jobId
    }

    Job(Map<String, Object> dslObject) {
        assert dslObject['jobId'] != null
        assert dslObject.size() > 1
        this.jobId = dslObject['jobId']
        this.dslObject = dslObject
    }

    def getJobProperty(String propertyPath) {

        // Checking job "Properties"
        if (properties[propertyPath] != null) {
            return properties[propertyPath]
        }

        // Checking job built-in properties
        if (getDslObject().get(propertyPath) != null) {
            return getDslObject().get(propertyPath)
        }

        // Requesting raw property
        def result = jh.getJobProperty(propertyPath, jobId)
        if (result != null) {
            // Saving to cache
            properties[propertyPath] = result
            return result
        }

        System.err.println("Failed to retrieve '$propertyPath'")

        return null
    }

    def getOutputParameter(String name, int stepNumber = 0) {
        if (outputParameters[stepNumber] == null)
            return null

        return outputParameters[stepNumber][name]
    }

    boolean isSuccess() {
        if (outcome != 'success') {
            if (logs != null) {
                println(logs)
            } else {
                println("Failed to retrieve job logs")
            }
        }

        return outcome == 'success'
    }

    Map<String, Object> getDslObject() {
        if (this.dslObject == null) {
            dslObject = _retrieveJobDetails()
        }
        return this.dslObject
    }

    Map<String, Object> _retrieveJobDetails() {
        def details = jh.dsl("getJobDetails(jobId: '$jobId')")
        return details['job'] as Map<String, Object>
    }

    void refresh() {
        properties = _retrieveJobProperties()
        outputParameters = _retrieveOutputParameters()
        dslObject = _retrieveJobDetails()

        outcome = dslObject['outcome']
        logs = dslObject['ec_debug_logs']
        status = dslObject['status']
    }

    protected Map<String, Object> _retrieveJobProperties() {
        def result = jh.getJobProperties(jobId)
        return result as Map<String, Object>
    }

    protected def _retrieveJobBuiltinProperty(String name) {
        return jh.getJobProperty('/myJob/' + name, jobId)
    }

    protected Map<Integer, Map<String, String>> _retrieveOutputParameters() {
        // TODO: the whole thing can be refactored to retrieve
        //  only the parameters for the requested steps
        Map<Integer, Map<String, String>> result = new HashMap<>()

        getJobSteps().eachWithIndex { it, i ->
            ArrayList opResponse = jh.dsl("""
                getOutputParameters(
                  jobStepId: '${it['jobStepId']}'
                )"""
            )['outputParameter'] as ArrayList

            if (opResponse != null) {
                Map<String, String> params = new HashMap<>()
                for (op in opResponse) {
                    params[(String) op['outputParameterName']] = (String) op['value']
                }
                result[i] = params
            }
        }

        return result
    }

    protected _retrieveJobLogs() {
        String stepLogs = ''

        if (this.jobSteps.size()) {
            def first = this.jobSteps.first()
            ArrayList<Map> steps = first['calledProcedure']['jobStep']
            for (Map step : steps) {
                stepLogs += "\n" + collectLogsFromJobStep(step)
            }
        }

        return stepLogs
    }

    protected static String collectLogsFromJobStep(Map<String, Object> jobStep) {
        String result = ''
        String jobStepId = jobStep['jobStepId']

        result += "\nStep: " + jobStep['stepName']

        if (jobStep['errorMessage'])
            result += "\n - Step error message: \n - - " + jobStep['errorMessage']

        def logResponse = jh.dsl("""
           getProperty(propertyName: '/myJobStep/ec_debug_log', jobStepId: '$jobStepId')
        """)
        if (logResponse['property']) {
            result += "\n - Step logs: \n - - " + logResponse['property']['value']
        }

        return result
    }

    static Job findJob(List<Map<String, String>> filters) {

        ArrayList<Job> jobs = findJobs(filters)

        if (jobs.size() > 1) {
            throw new RuntimeException("More than one job was found for given filter")
        }

        return jobs[0]
    }

    static ArrayList<Job> findJobs(
            List<Map<String, Object>> filters,
            List<Map<String, String>> sort = null
    ) {
        String dslFilters = stringifyListOfMaps(filters)
        String dslSorts = ""
        if (sort != null) {
            String sorts = stringifyListOfMaps(sort)
            dslSorts = ",sort: constructSorts($sorts)"
        }

        def response = (new PluginSpockTestSupport()).dsl(getFindObjectsPreamble() + """
            findObjects(
                objectType: 'job',
                filter: constructFilters($dslFilters)
                $dslSorts
            )
        """)

        ArrayList<Job> result = response['object'].collect({ new Job(it['job']) })

        return result
    }

    static ArrayList<Job> findJobsOfProcedure(String procedureName) {
        return findJobs(
                // Filter
                [[propertyName: 'procedureName', operator: 'equals', operand1: procedureName]],
                // Sort
                [[propertyName: "createTime", order: "ascending"]]
        )
    }

    static String stringifyListOfMaps(List<Map<String, String>> maps) {
        ArrayList resultList = new ArrayList()
        for (Map map : maps) {
            List<String> pairs = new ArrayList<>()
            map.each { k, v -> pairs.add("$k: \"\"\"$v\"\"\"") }
            resultList += "[" + pairs.join(", ") + "]"
        }
        return "[" + resultList.join(",") + "]"
    }

    static String getFindObjectsPreamble() {
        return """
import com.electriccloud.query.Filter
import com.electriccloud.query.CompositeFilter
import com.electriccloud.query.PropertyFilter
import com.electriccloud.query.Operator
import com.electriccloud.query.SelectSpec
import com.electriccloud.util.SortOrder
import com.electriccloud.util.SortSpec

def findObjectsSimplified(Map args) {
    findObjectsSimplified(args.objectType, args.filters, args.selects, args.sorts)
}

def findObjectsSimplified(String objectType, def filters = null, def selects = null, def sorts = null) {
    def result = findObjects(objectType: objectType, filter: constructFilters(filters), select: constructSelects(selects), sort: constructSorts(sorts))
}

def constructFilters(def filters) {
    filters?.collect { f ->
        def op = Operator.valueOf(f['operator'])
        if (op.isBoolean()) {
            new CompositeFilter(op, constructFilters(f['filters']) as Filter[])
        } else {
            new PropertyFilter(f['propertyName'], op, f['operand1'], f['operand2'])
        }
    }
}

def constructSelects(def selects) {
    selects?.collect { s ->
        s instanceof String ? new SelectSpec(s, false) : new SelectSpec(s['propertyName'], s['recurse'])
    }
}

def constructSorts(def sorts) {
    sorts?.collect { s ->
        new SortSpec(s['propertyName'], SortOrder.valueOf(s['order'] ?: "ascending"))
    }
}
"""
    }
}
