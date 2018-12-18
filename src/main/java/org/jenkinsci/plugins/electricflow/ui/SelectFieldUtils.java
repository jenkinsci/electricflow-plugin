package org.jenkinsci.plugins.electricflow.ui;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.Arrays;

public class SelectFieldUtils {

    public static ListBoxModel getListBoxModelOnException(String displayValue) {
        SelectItemValidationWrapper selectItemValidationWrapper = new SelectItemValidationWrapper(
                FieldValidationStatus.ERROR,
                "Error when fetching values for this parameter. Check the Jenkins logs for more details.",
                ""
        );

        ListBoxModel m = new ListBoxModel();
        m.add(new ListBoxModel.Option(displayValue, selectItemValidationWrapper.getJsonStr(), true));
        return m;
    }

    public static ListBoxModel getListBoxModelOnWrongConf(String displayValue) {
        SelectItemValidationWrapper selectItemValidationWrapper = new SelectItemValidationWrapper(
                FieldValidationStatus.ERROR,
                "Connection to Electric Flow Server Failed. Please fix connection information and reload this page.",
                ""
        );

        ListBoxModel m = new ListBoxModel();
        m.add(new ListBoxModel.Option(displayValue, selectItemValidationWrapper.getJsonStr(), true));
        return m;
    }

    public static FormValidation getFormValidationBasedOnSelectItemValidationWrapper(String item) {
        if (isSelectItemValidationWrapper(item)) {
            SelectItemValidationWrapper selectItemValidationWrapper = new SelectItemValidationWrapper(item);
            switch (selectItemValidationWrapper.getValidationStatus()) {
                case ERROR: return FormValidation.error(selectItemValidationWrapper.getValidationMessage());
                case WARN: return FormValidation.warning(selectItemValidationWrapper.getValidationMessage());
                case OK: return FormValidation.ok(selectItemValidationWrapper.getValidationMessage());
            }
        }
        return null;
    }

    public static boolean isSelectItemValidationWrapper(String item) {
        return item != null
                && !item.isEmpty()
                && item.startsWith("{")
                && item.contains("validationStatus")
                && item.contains("validationMessage")
                && item.contains("value")
                && item.endsWith("}");
    }

    public static String getSelectItemValue(String item) {
        return isSelectItemValidationWrapper(item)
                ? new SelectItemValidationWrapper(item).getValue()
                : item;
    }

    public static boolean checkAllSelectItemsAreNotValidationWrappers(String... items) {
        for (String item: items) {
            if (isSelectItemValidationWrapper(item)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAnySelectItemsIsValidationWrappers(String... items) {
        return Arrays
                .stream(items)
                .anyMatch(SelectFieldUtils::isSelectItemValidationWrapper);
    }

}
