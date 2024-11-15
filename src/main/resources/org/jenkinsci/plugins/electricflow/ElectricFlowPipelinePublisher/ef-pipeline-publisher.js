function deleteParameterRows() {
    var paramRows = document.querySelectorAll("._ef_row");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}

function deleteStagesToRun() {
    var paramRows = document.querySelectorAll("._ef_row_stages");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}

function fillParameters() {
    deleteParameterRows();

    if (!document.getElementById('addP')) {
        return;
    }

    var option = document.getElementById('addP').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);

    var table = document.querySelector("[descriptorid='electricFlowSettings'] table:not([name='runPipelineCompare'])");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }

    if (json) {
        if (json.pipeline) {
            var parameters;
            try {
                parameters = JSON.parse(json.pipeline.parameters);
            }
            catch (error) {
                parameters = json.pipeline.parameters;
            }

            if (table == null) {
                if (parameters.length) {
                    var el = document.createElement("div");
                    el.innerHTML = "&lt;br&gt;&lt;b&gt;Pipeline Parameters&lt;/b&gt;";
                    el.className = "_ef_row";
                    var div = document.getElementById("addP");
                    div = cbcd_insertAfter(div, el);
                    parameters.forEach(function (elem) {
                        var el_param = document.createElement("div");
                        el_param.className = "_ef_parameters _ef_row";
                        div = cbcd_insertAfter(div, el_param);
                        addParameterRowForRunPipeline(el_param, elem.parameterName, elem.parameterValue, false, false, updateJsonParameters);
                    });
                }
            } else {
                var row1 = table.insertRow(table.rows.length - 2);
                row1.className = "_ef_row";
                if (parameters.length) {
                    row1.appendChild(createTitleForRunPipeline());
                    parameters.forEach(function (elem) {
                        var row = table.insertRow(table.rows.length - 2);
                        row.className = "_ef_parameters _ef_row";
                        addParameterRowForRunPipeline(row, elem.parameterName, elem.parameterValue, false, true, updateJsonParameters);
                    });
                }
            }
        }
    }
}

function fillStagesToRun() {
    deleteStagesToRun();

    if (!document.getElementById('stagesToRunEntry')) {
        return;
    }

    var option = document.getElementById('stagesToRunEntry').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);

    var table = document.querySelector("[descriptorid='electricFlowSettings'] table:not([name='runPipelineCompare'])");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }

    if (json) {
        if (json.pipeline) {
            var stages;
            try {
                stages = JSON.parse(json.pipeline.stages);
            }
            catch (error) {
                stages = json.pipeline.stages;
            }

            if (table == null) {
                if (stages.length) {
                    var el = document.createElement("div");
                    el.innerHTML = "&lt;br&gt;&lt;b&gt;Stages To Run&lt;/b&gt;";
                    el.className = "_ef_row_stages";
                    var div = document.getElementById("stagesToRunEntry");
                    div = cbcd_insertAfter(div, el);
                    stages.forEach(function (elem) {
                        var el_param = document.createElement("div");
                        el_param.className = "_ef_rp_stages _ef_row_stages";
                        div = cbcd_insertAfter(div, el_param);
                        addParameterRowForRunPipeline(el_param, elem.stageName, elem.stageValue, true, false, updateJsonStagesToRun);
                    });
                }
            } else {
                var row1 = table.insertRow(table.rows.length - 2);
                row1.className = "_ef_row_stages";
                if (stages.length) {
                    row1.appendChild(createTitleForTriggerRelease("Stages to run"));
                    stages.forEach(function (elem) {
                        var row = table.insertRow(table.rows.length - 2);
                        row.className = "_ef_rp_stages _ef_row_stages";
                        addParameterRowForRunPipeline(row, elem.stageName, elem.stageValue, true, true, updateJsonStagesToRun);
                    });
                }
            }
        }
    }
}

function cbcd_insertAfter(referenceNode, newNode) {
    return referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
}

function updateJsonParameters() {
    var pipelineName = document.getElementById("pipelineN").value;
    var json = JSON.parse('{"pipeline":{"pipelineName":"' + pipelineName + '","parameters":[]}}');
    var parameters = document.querySelectorAll("._ef_parameters");
    [].forEach.call(parameters, function (elem) {
        json.pipeline.parameters.push({
            'parameterName': elem.querySelector(".setting-name").textContent,
            'parameterValue': elem.querySelector(".setting-input").value
        })
    });
    document.getElementById('addP').options[0].value = JSON.stringify(json);
    document.getElementById('addP').options[0].textContent = JSON.stringify(json);
}

function updateJsonStagesToRun() {
    var pipelineName = document.getElementById("pipelineN").value;
    var json = JSON.parse('{"pipeline":{"pipelineName":"' + pipelineName + '","stages":[]}}');
    var stages = document.querySelectorAll("._ef_rp_stages");
    [].forEach.call(stages, function (elem) {
        json.pipeline.stages.push({
            'stageName': elem.querySelector(".setting-name").textContent,
            'stageValue': elem.querySelector(".setting-input").checked
        })
    });

    document.getElementById('stagesToRunEntry').options[0].value = JSON.stringify(json);
    document.getElementById('stagesToRunEntry').options[0].textContent = JSON.stringify(json);
}

function createTitleForRunPipeline() {
    var td = document.createElement('td');
    td.colSpan = 4;
    var div = document.createElement('div');
    div.className = "section-header";
    div.textContent = "Pipeline Parameters";
    td.appendChild(div);
    return td;
}

function addParameterRowForRunPipeline(row, label, value, isCheckbox, isTable, onChangeFunction) {
    var td1 = document.createElement(isTable ? 'td' : 'div');
    td1.className = 'setting-leftspace';
    td1.textContent = '';
    row.appendChild(td1);

    var td2 = document.createElement(isTable ? 'td' : 'div');
    td2.className = 'setting-name';
    td2.textContent = label;
    row.appendChild(td2);

    var td3 = document.createElement(isTable ? 'td' : 'div');
    td3.className = 'setting-main';
    var input1 = document.createElement('input');
    input1.name = 'parameterName';
    if (isCheckbox) {
        input1.type = "checkbox";
        if (value === "true") {
            input1.checked = value;
        }
        input1.style="width: auto;";
    }
    input1.value = value;
    input1.onchange = onChangeFunction;
    input1.className = 'setting-input';
    td3.appendChild(input1);
    row.appendChild(td3);

    var td4 = document.createElement(isTable ? 'td' : 'div');
    td4.className = 'setting-no-help';
    row.appendChild(td4);
}

function triggerExtraValidationForRunPipeline() {
    document.getElementById("ef_runPipeline_validationTrigger").click();
}

function getElementByXpath(path) {
    return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

const behaviourConfig = {
    '.ef-pipeline-publisher-config': {
        name: 'config',
        event: 'change',
        handler: deleteParameterRows
    },
    '#projectN': {
        name: 'projectName',
        event: 'change',
        handler: deleteParameterRows
    },
    '#pipelineN': {
        name: 'pipelineName',
        event: 'change',
        handler: () => {
            fillParameters();
            fillStagesToRun();
        }
    },
    '#stagesToRunEntry': {
        name: 'stages',
        event: 'change',
        handler: fillStagesToRun
    },
    '#addP': {
        name: 'params',
        event: 'change',
        handler: fillParameters
    },
    '.validate-before-apply-btn': {
        name: 'validate',
        event: 'click',
        handler: triggerExtraValidationForRunPipeline
    }
};

Object.entries(behaviourConfig).forEach(([selector, config]) => {
    Behaviour.specify(selector, config.name, 0, function(element) {
        element.addEventListener(config.event, config.handler);
    });
});

fillParameters();
fillStagesToRun();
