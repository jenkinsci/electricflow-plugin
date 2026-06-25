package org.jenkinsci.plugins.electricflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.FormValidation;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.junit.jupiter.api.Test;

class BasicUnitTests {

    @Test
    void testSelectItemValidationWrapper() {
        SelectItemValidationWrapper selectItemValidationWrapper =
                new SelectItemValidationWrapper(FieldValidationStatus.OK, "validation message", "value");
        System.out.println(selectItemValidationWrapper.getJsonStr());
        String selectItemValidationWrapperJsonStr = selectItemValidationWrapper.getJsonStr();

        assertTrue(Pattern.compile(".*\"validationMessage\":\"validation message\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern.compile(".*\"validationStatus\":\"OK\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern.compile(".*\"value\":\"value\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
    }

    @Test
    void checkValidateValueOnEmpty() {
        FormValidation formValidation;

        formValidation = Utils.validateValueOnEmpty("", "Field name");
        System.out.println(formValidation.getMessage());
        assertEquals("Field name field should not be empty.", formValidation.getMessage());
        assertSame(FormValidation.Kind.WARNING, formValidation.kind);

        formValidation = Utils.validateValueOnEmpty("test", "Field name");
        assertSame(FormValidation.Kind.OK, formValidation.kind);
    }
}
