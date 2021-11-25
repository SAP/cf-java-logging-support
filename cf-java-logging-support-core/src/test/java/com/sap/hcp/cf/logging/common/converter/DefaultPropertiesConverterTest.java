package com.sap.hcp.cf.logging.common.converter;

import static com.sap.hcp.cf.logging.common.converter.UnmarshallUtilities.unmarshal;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;

public class DefaultPropertiesConverterTest {

    private static final String HACK_ATTEMPT = "}{:\",\"";

    private DefaultPropertiesConverter converter;

    @Before
    public void initConverter() {
        this.converter = new DefaultPropertiesConverter();
    }

    @Before
    public void cleadMdc() {
        MDC.clear();
    }

    @Test
    public void emptyProperties() throws Exception {
        StringBuilder sb = new StringBuilder();

        converter.convert(sb, Collections.emptyMap());

        assertTrue("Should have empty properties by default", unmarshal(sb).isEmpty());
    }

    @Test
    public void singleMdcEntry() throws Exception {
        StringBuilder sb = new StringBuilder();
        MDC.put("some key", "some value");

        converter.convert(sb, Collections.emptyMap());

        assertThat(unmarshal(sb), hasEntry("some key", "some value"));
    }

    @Test
    public void twoMdcEntries() throws Exception {
        StringBuilder sb = new StringBuilder();
        MDC.put("some key", "some value");
        MDC.put("other key", "other value");

        converter.convert(sb, Collections.emptyMap());

        assertThat(unmarshal(sb), allOf(hasEntry("some key", "some value"), hasEntry("other key", "other value")));
    }

    @Test
    public void singleExplicitEntry() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("explicit key", "explicit value");
            }
        };

        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), hasEntry("explicit key", "explicit value"));
    }

    @Test
    public void mergesDifferentMdcAndExplicitEntries() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("explicit key", "explicit value");
            }
        };
        MDC.put("some key", "some value");

        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), allOf(hasEntry("some key", "some value"), hasEntry("explicit key",
                                                                                     "explicit value")));
    }

    @Test
    public void explicitValuesOverwritesMdc() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("some key", "explicit value");
            }
        };
        MDC.put("some key", "some value");

        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), hasEntry("some key", "explicit value"));
    }

    @Test
    public void dropsExclusions() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("excluded explicit key", "excluded explicit value");
                put("retained explicit key", "retained explicit value");
            }
        };
        MDC.put("retained mdc key", "retained mdc value");
        MDC.put("excluded mdc key", "excluded mdc value");

        converter.setExclusions(Arrays.asList("excluded explicit key", "excluded mdc key"));
        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), allOf(hasEntry("retained mdc key", "retained mdc value"), not(hasEntry(
                                                                                                         "excluded mdc key",
                                                                                                         "excluded mdc value")),
                                        hasEntry("retained explicit key", "retained explicit value"), not(hasEntry(
                                                                                                                   "excluded explicit key",
                                                                                                                   "excluded explicit value"))));

    }

    @Test
    public void properlyEscapesKeys() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("explicit" + HACK_ATTEMPT, "explicit value");
            }
        };
        MDC.put("mdc" + HACK_ATTEMPT, "mdc value");

        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), allOf(hasEntry("mdc" + HACK_ATTEMPT, "mdc value"), hasEntry("explicit" + HACK_ATTEMPT,
                                                                                              "explicit value")));
    }

    @Test
    public void properlyEscapesValues() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("explicit key", "explicit" + HACK_ATTEMPT);
            }
        };
        MDC.put("mdc key", "mdc" + HACK_ATTEMPT);

        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), allOf(hasEntry("mdc key", "mdc" + HACK_ATTEMPT), hasEntry("explicit key", "explicit" +
                                                                                                            HACK_ATTEMPT)));
    }

    @Test
    public void properlyEscapesExclusions() throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("serial")
        Map<String, String> explicitFields = new HashMap<String, String>() {
            {
                put("explicit" + HACK_ATTEMPT, "explicit value");
            }
        };
        MDC.put("mdc" + HACK_ATTEMPT, "mdc value");

        converter.setExclusions(Arrays.asList("explicit" + HACK_ATTEMPT, "mdc" + HACK_ATTEMPT));
        converter.convert(sb, explicitFields);

        assertThat(unmarshal(sb), allOf(not(hasEntry("mdc" + HACK_ATTEMPT, "mdc value")), not(hasEntry("explicit" +
                                                                                                       HACK_ATTEMPT,
                                                                                                       "explicit value"))));
    }

    @Test
    public void fullDefaultPropertiesIfConfigured() throws Exception {
        StringBuilder sb = new StringBuilder();

        converter.setSendDefaultValues(true);
        converter.convert(sb, Collections.emptyMap());

        assertThat(unmarshal(sb), hasDefaultProperties());

    }

    @SuppressWarnings("unchecked")
    private static Matcher<Map<String, Object>> hasDefaultProperties() {
        return allOf(hasEntry(Fields.CORRELATION_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.TENANT_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.COMPONENT_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.COMPONENT_NAME, Defaults.UNKNOWN), //
                     hasEntry(Fields.COMPONENT_TYPE, Defaults.COMPONENT_TYPE), //
                     hasEntry(Fields.COMPONENT_INSTANCE, Defaults.COMPONENT_INDEX), //
                     hasEntry(Fields.CONTAINER_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.ORGANIZATION_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.ORGANIZATION_NAME, Defaults.UNKNOWN), //
                     hasEntry(Fields.SPACE_ID, Defaults.UNKNOWN), //
                     hasEntry(Fields.SPACE_NAME, Defaults.UNKNOWN), //
                     not(hasKey(Fields.REQUEST_ID)));
    }
}
