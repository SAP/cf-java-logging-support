package com.sap.hcp.cf.logging.common.helper;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import org.assertj.core.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public final class ConsoleAssertions {

    private ConsoleAssertions() {
    }

    public static AbstractStringAssert<?> assertLastEventMessage(ConsoleOutput console) {
        return assertThat(console.getLastMessage());
    }

    public static MapAssert<String, Object> assertLastEventFields(ConsoleOutput console) {
        return assertThat(console.getLastEventMap());
    }

    public static ListAssert<String> assertLastEventCategories(ConsoleOutput console) {
        return assertThat(console.getLastCategories());
    }

    public static AbstractLongAssert<?> assertLastEventTimestamp(ConsoleOutput console) {
        Object rawTimestamp = console.getLastEventMap().get(Fields.WRITTEN_TS);
        return assertThat(Long.valueOf(rawTimestamp.toString()));
    }

    @SuppressWarnings("unchecked")
    public static ClassBasedNavigableListAssert<?, List<? extends String>, String, StringAssert> assertLastEventStacktrace(
            ConsoleOutput console) {
        Object rawStracktrace = console.getLastEventMap().get(Fields.STACKTRACE);
        return Assertions.assertThat((List<String>) rawStracktrace, StringAssert.class);
    }

    @SuppressWarnings("unchecked")
    public static ListAssert<Map<String, Object>> assertLastEventCustomFields(ConsoleOutput console) {
        Object rawWrappedCustomFields = console.getLastEventMap().get(Fields.CUSTOM_FIELDS);
        if (rawWrappedCustomFields == null) {
            return assertThat(Collections.emptyList());
        }
        Object rawCustomFields = ((Map<String, Object>) rawWrappedCustomFields).get("string");
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) rawCustomFields;
        return assertThat(customFields);
    }

    public static CustomFieldAssertion assertCustomField(Map<String, Object> entries) {
        return new CustomFieldAssertion(Assertions.assertThat(entries));
    }

    public static class CustomFieldAssertion {

        private MapAssert<String, Object> assertion;

        private CustomFieldAssertion(MapAssert<String, Object> assertion) {
            this.assertion = assertion;
        }

        public CustomFieldAssertion hasKey(String expectedKey) {
            assertion = assertion.containsEntry("k", expectedKey);
            return this;
        }

        public CustomFieldAssertion hasValue(String expectedValue) {
            assertion = assertion.containsEntry("v", expectedValue);
            return this;
        }

        public CustomFieldAssertion hasIndex(Integer expectedIndex) {
            assertion = assertion.containsEntry("i", expectedIndex);
            return this;
        }
    }
}
