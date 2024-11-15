function deleteReleaseParameterRows() {
    var paramRows = document.querySelectorAll("._ef_st_row");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}
function deleteStagesToRun() {
    var paramRows = document.querySelectorAll("._ef_str_row");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}

function fillReleasesParameters() {
    deleteReleaseParameterRows();

    if (!document.getElementById('ef_sr_parameters')) {
        return;
    }

    var option = document.getElementById('ef_sr_parameters').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);

    var table = document.querySelector("[descriptorid='electricFlowTriggerRelease'] table");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }

    if (json) {

        if (json.release) {
            var parameters;
            try {
                parameters = JSON.parse(json.release.parameters);
            }
            catch (error) {
                parameters = json.release.parameters;
            }

            if (table == null) {
                if (parameters.length) {
                    var el = document.createElement("div");
                    el.innerHTML = "&lt;br&gt;&lt;b&gt;Release pipeline parameters&lt;/b&gt;";
                    el.className = "_ef_st_row";
                    var div = document.getElementById("ef_sr_parameters");
                    div = cbcd_insertAfter(div, el);
                    parameters.forEach(function (elem) {
                        var el_param = document.createElement("div");
                        el_param.className = "_ef_sr_parameters _ef_st_row";
                        div = cbcd_insertAfter(div, el_param);
                        addParameterRowForTriggerRelease(el_param, elem.parameterName, elem.parameterValue, false, false);
                    });
                }
            } else {
                var row1 = table.insertRow(table.rows.length - 2);
                row1.className = "_ef_st_row";
                if (parameters.length) {
                    row1.appendChild(createTitleForTriggerRelease("Release pipeline parameters"));
                    parameters.forEach(function (elem) {
                        var row = table.insertRow(table.rows.length - 2);
                        row.className = "_ef_sr_parameters _ef_st_row";
                        addParameterRowForTriggerRelease(row, elem.parameterName, elem.parameterValue, false, true);
                    });
                }
            }


        }
    }
}
function fillStagesToRun() {
    deleteStagesToRun();
    if (!document.getElementById('ef_sr_parameters')) {
        return;
    }
    var option = document.getElementById('ef_sr_parameters').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);
    var table = document.querySelector("[descriptorid='electricFlowTriggerRelease'] table");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }
    var stages;
    if (typeof json.release !== "undefined") {
        try {
            stages = JSON.parse(json.release.stages);
        } catch (error) {
            stages = json.release.stages;
        }

        if (table == null) {
            if (stages.length) {
                var el = document.createElement("div");
                el.innerHTML = "&lt;br&gt;&lt;b&gt;Stages to run&lt;/b&gt;";
                el.className = "_ef_str_row";
                var div = document.getElementById("ef_rt_stagesToRunEntry");
                div = cbcd_insertAfter(div, el);
                stages.forEach(function (elem) {
                    var el_param = document.createElement("div");
                    el_param.className = "_ef_sr_stages _ef_str_row";
                    div = cbcd_insertAfter(div, el_param);
                    addParameterRowForTriggerRelease(el_param, elem.stageName, elem.stageValue, true, false);
                });
            }
        } else {
            var row1 = table.insertRow(table.rows.length - 2);
            row1.className = "_ef_str_row";
            if (stages.length) {
                row1.appendChild(createTitleForTriggerRelease("Stages to run"));
                stages.forEach(function (elem) {
                    var row = table.insertRow(table.rows.length - 2);
                    row.className = "_ef_sr_stages _ef_str_row";
                    addParameterRowForTriggerRelease(row, elem.stageName, elem.stageValue, true, true);
                });
            }
        }
    }
}
function cbcd_insertAfter(referenceNode, newNode) {
    return referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
}

function updateReleaseJson() {
    var releaseName = document.getElementById("ef_sr_releaseName").value;
    var json = JSON.parse('{"release":{"releaseName":"' + releaseName + '","stages":[], "parameters":[]}}');
    var stages = document.querySelectorAll("._ef_sr_stages");

    [].forEach.call(stages, function (elem) {
        json.release.stages.push({
            'stageName': elem.querySelector(".setting-name").textContent,
            'stageValue': elem.querySelector(".setting-input").checked
        })
    });

    var stages = document.querySelectorAll("._ef_sr_parameters");

    [].forEach.call(stages, function (elem) {
        json.release.parameters.push({
            'parameterName': elem.querySelector(".setting-name").textContent,
            'parameterValue': elem.querySelector(".setting-input").value
        })
    });
    document.getElementById('ef_sr_parameters').options[0].value = JSON.stringify(json);
    document.getElementById('ef_sr_parameters').options[0].textContent = JSON.stringify(json);
}

function createTitleForTriggerRelease(title) {
    var td = document.createElement('td');
    td.colSpan = 4;
    var div = document.createElement('div');
    div.className = "section-header";
    div.textContent = title;
    td.appendChild(div);
    return td;
}

function addParameterRowForTriggerRelease(row, label, value, isCheckbox, isTable) {
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
    input1.onchange = updateReleaseJson;
    input1.className = 'setting-input';
    td3.appendChild(input1);
    row.appendChild(td3);

    var td4 = document.createElement(isTable ? 'td' : 'div');
    td4.className = 'setting-no-help';
    row.appendChild(td4);
}

function triggerExtraValidationForTriggerRelease() {
    document.getElementById("ef_sr_validationTrigger").click();
}

function getElementByXpath(path) {
    return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

const behaviourConfig = {
    '.ef-release-config': {
        name: 'config',
        event: 'change',
        handler: () => {
            deleteReleaseParameterRows();
            deleteStagesToRun();
        }
    },
    '.ef-release-project': {
        name: 'project',
        event: 'change',
        handler: () => {
            deleteReleaseParameterRows();
            deleteStagesToRun();
        }
    },
    '#ef_sr_releaseName': {
        name: 'release',
        event: 'change',
        handler: () => {
            fillReleasesParameters();
            fillStagesToRun();
        }
    },
    '#ef_sr_parameters': {
        name: 'params',
        event: 'change',
        handler: () => {
            fillReleasesParameters();
            fillStagesToRun();
        }
    },
    '#ef_rt_stagesToRunEntry': {
        name: 'stages',
        event: 'change',
        handler: fillStagesToRun
    },
    '.validate-before-apply-btn': {
        name: 'validate',
        event: 'click',
        handler: triggerExtraValidationForTriggerRelease
    }
};

Object.entries(behaviourConfig).forEach(([selector, config]) => {
    Behaviour.specify(selector, config.name, 0, function(element) {
        element.addEventListener(config.event, config.handler);
    });
});

fillReleasesParameters();
fillStagesToRun();