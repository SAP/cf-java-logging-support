package com.sap.hcp.cf.logging.common.serialization;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.sap.hcp.cf.logging.common.serialization.SapApplicationLoggingTestBindings.JUST_ONE_SAP_APPLICATION_LOGGING_BINDING;
import static com.sap.hcp.cf.logging.common.serialization.SapApplicationLoggingTestBindings.NO_SAP_APPLICATION_LOGGING_BINDING;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ContextFieldConverterTest {

    @Test
    void addsRegisteredCustomFields() throws IOException {
        ContextFieldConverter converter = new ContextFieldConverter(false, List.of("customFieldName"), emptyList(),
                                                                    JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue", "otherFieldName", "otherFieldValue");

        converter.addCustomFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace(
                "{\"#cf\":{\"string\":[{\"k\":\"customFieldName\",\"v\":\"customFieldValue\",\"i\":0}]}}");
    }

    private static ObjectComposer<JSONComposer<String>> createObjectComposer() throws IOException {
        return new JSON().composeString().startObject();
    }

    private static AbstractStringAssert<?> assertJson(ObjectComposer<JSONComposer<String>> objectComposer)
            throws IOException {
        return assertThat(objectComposer.end().finish());
    }

    @Test
    void respectsOrderingOfRegisteredCustomFields() throws IOException {
        ContextFieldConverter converter =
                new ContextFieldConverter(false, List.of("firstFieldName", "secondFieldName"), emptyList(),
                                          JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = new TreeMap<>(); // for fixed iteration order
        fields.put("secondFieldName", "secondFieldValue"); // place second field deliberately before first
        fields.put("firstFieldName", "firstFieldValue");

        converter.addCustomFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace(
                "{\"#cf\":{\"string\":[{\"k\":\"firstFieldName\",\"v\":\"firstFieldValue\",\"i\":0}," + "{\"k\":\"secondFieldName\",\"v\":\"secondFieldValue\",\"i\":1}]}}");
    }

    @Test
    void doesNotCreateCustomFieldsWithoutSapApplicationLoggingBinding() throws IOException {
        ContextFieldConverter converter = new ContextFieldConverter(false, List.of("customFieldName"), emptyList(),
                                                                    NO_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue", "otherFieldName", "otherFieldValue");

        converter.addCustomFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace("{}");
    }

    @Test
    void addsUnregisteredFieldsAsContextFields() throws IOException {
        ContextFieldConverter converter = new ContextFieldConverter(false, emptyList(), emptyList(),
                                                                    JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue");

        converter.addContextFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace("{\"customFieldName\":\"customFieldValue\"}");
    }

    @Test
    void doesNotAddRegisteredFieldsAsContextFields() throws IOException {
        ContextFieldConverter converter = new ContextFieldConverter(false, List.of("customFieldName"), emptyList(),
                                                                    JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue");

        converter.addContextFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace("{}");
    }

    @Test
    void addsRetainedFieldsAsContextFields() throws IOException {
        ContextFieldConverter converter =
                new ContextFieldConverter(false, List.of("customFieldName"), List.of("customFieldName"),
                                          JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue");

        converter.addContextFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace("{\"customFieldName\":\"customFieldValue\"}");
    }

    @Test
    void addsRegisteredFieldsWithoutSapApplicationLoggingBindingAsContextFields() throws IOException {
        ContextFieldConverter converter = new ContextFieldConverter(false, List.of("customFieldName"), emptyList(),
                                                                    NO_SAP_APPLICATION_LOGGING_BINDING.getDetector());
        ObjectComposer<JSONComposer<String>> objectComposer = createObjectComposer();
        Map<String, Object> fields = Map.of("customFieldName", "customFieldValue");

        converter.addContextFields(objectComposer, fields);

        assertJson(objectComposer).isEqualToIgnoringWhitespace("{\"customFieldName\":\"customFieldValue\"}");
    }

}
