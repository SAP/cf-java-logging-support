
package com.sap.hcp.cf.logback.converter;

import static com.sap.hcp.cf.logback.converter.CustomFieldsAdapter.OPTION_MDC_CUSTOM_FIELDS;
import static com.sap.hcp.cf.logback.converter.CustomFieldsAdapter.OPTION_MDC_RETAINED_FIELDS;
import static com.sap.hcp.cf.logback.converter.CustomFieldsAdapter.OPTION_SEND_DEFAULT_VALUES;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sap.hcp.cf.logging.common.LogContext;

import ch.qos.logback.core.Context;

@RunWith(MockitoJUnitRunner.class)
public class CustomFieldsAdapterTest {

	@Mock
	private Context context;

	@InjectMocks
	private CustomFieldsAdapter adapter;

	@Test
	public void emptyExclusionsWithoutContext() throws Exception {
		adapter.initialize(null);

		List<String> exclusions = adapter.getCustomFieldExclusions();

        assertThat(exclusions, is(empty()));
	}

	@Test
	public void emptyExclusionsWithoutConfigObject() throws Exception {
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, is(empty()));
	}

	@Test
	public void emptyExclusionsWithImproperCustomFields() throws Exception {
		when(context.getObject(OPTION_MDC_CUSTOM_FIELDS)).thenReturn(new Object());
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, is(empty()));
	}

	@Test
	public void emptyExclusionsWithImproperRetains() throws Exception {
		when(context.getObject(OPTION_MDC_RETAINED_FIELDS)).thenReturn(new Object());
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, is(empty()));
	}

	@Test
	public void emptyExclusionsWithoutRetains() throws Exception {
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, is(empty()));
	}

	@Test
	public void excludesCustomFields() throws Exception {
		when(context.getObject(OPTION_MDC_CUSTOM_FIELDS)).thenReturn(asList("this key", "that key"));
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, containsInAnyOrder("this key", "that key"));

	}

	@Test
	public void removesCustomFieldsFromExclusionsWhenRetained() throws Exception {
		when(context.getObject(OPTION_MDC_CUSTOM_FIELDS)).thenReturn(asList("this key", "that key"));
		when(context.getObject(OPTION_MDC_RETAINED_FIELDS)).thenReturn(asList("that key"));
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, containsInAnyOrder("this key"));
	}

	@Test
	public void neverExcludesLogContextFieldsEvenWhenConfigured() throws Exception {
		when(context.getObject(OPTION_MDC_RETAINED_FIELDS)).thenReturn(LogContext.getContextFieldsKeys());
		adapter.initialize(context);

		List<String> exclusions = adapter.getCustomFieldExclusions();

		assertThat(exclusions, is(empty()));
	}

    @Test
    public void missingSendDefaultValueOption() throws Exception {
        assertFalse("Should not send default values without config.", adapter.isSendDefaultValues());
    }

    @Test
    public void falseSendDefaultValueOption() throws Exception {
        when(context.getObject(OPTION_SEND_DEFAULT_VALUES)).thenReturn(Boolean.FALSE);
        adapter.initialize(context);
        assertFalse("Should not send default values when configured false.", adapter.isSendDefaultValues());
    }

    @Test
    public void trueSendDefaultValueOption() throws Exception {
        when(context.getObject(OPTION_SEND_DEFAULT_VALUES)).thenReturn(true);
        adapter.initialize(context);
        assertTrue("Should send default values when configured true.", adapter.isSendDefaultValues());
    }

    @Test
    public void faultySendDefaultValueOption() throws Exception {
        when(context.getObject(OPTION_SEND_DEFAULT_VALUES)).thenReturn(new Object());
        adapter.initialize(context);
        assertFalse("Should not send default values when configured with generic object.", adapter
                                                                                                  .isSendDefaultValues());

    }

}
