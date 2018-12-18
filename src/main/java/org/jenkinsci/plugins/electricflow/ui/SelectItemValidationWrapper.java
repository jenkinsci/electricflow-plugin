package org.jenkinsci.plugins.electricflow.ui;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.util.HashMap;
import java.util.Map;

public class SelectItemValidationWrapper {

    private FieldValidationStatus validationStatus;
    private String validationMessage;
    private String value;

    SelectItemValidationWrapper() {
    }

    public SelectItemValidationWrapper(
            FieldValidationStatus validationStatus,
            String validationMessage,
            String value) {
        this.validationStatus = validationStatus;
        this.validationMessage = validationMessage;
        this.value = value;
    }

    public SelectItemValidationWrapper(String jsonStr) {
        JSONObject jsonObject = JSONObject.fromObject(jsonStr);
        this.validationStatus = FieldValidationStatus.valueOf(jsonObject.getString("validationStatus"));
        this.validationMessage = jsonObject.getString("validationMessage");
        this.value = jsonObject.getString("value");
    }

    public FieldValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(FieldValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getJsonStr() {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("validationStatus", getValidationStatus().name());
        jsonMap.put("validationMessage", getValidationMessage());
        jsonMap.put("value", getValue());
        return JSONSerializer.toJSON(jsonMap).toString();
    }
}
