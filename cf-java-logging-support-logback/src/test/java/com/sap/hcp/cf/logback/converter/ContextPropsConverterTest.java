package com.sap.hcp.cf.logback.converter;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.converter.DefaultPropertiesConverter;
import com.sap.hcp.cf.logging.common.customfields.CustomField;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;

@RunWith(MockitoJUnitRunner.class)
public class ContextPropsConverterTest {

	@Mock
	private ILoggingEvent event;

	@Mock
	private Context context;

	@Mock
	private DefaultPropertiesConverter defaultConverter;

	@Mock
	private CustomFieldsAdapter customFieldsAdapter;

	@Captor
	private ArgumentCaptor<List<String>> exclusions;

	@Captor
	private ArgumentCaptor<Map<String, String>> eventProperties;

	@InjectMocks
	private ContextPropsConverter converter;

	@Before
	public void initConverter() {
		converter.setContext(context);
		converter.setConverter(defaultConverter);
		converter.setCustomFieldsAdapter(customFieldsAdapter);
	}

	@Test
	public void forwardsMdcFieldsFromEvent() throws Exception {
		HashMap<String, String> mdcMap = new HashMap<>();
		when(event.getMDCPropertyMap()).thenReturn(mdcMap);
		converter.start();

		converter.convert(event);

		verify(defaultConverter).convert(any(), eq(mdcMap));
	}

	@Test
	public void loadsContextFields() throws Exception {
		// needs to be set since value default is null
		// which removes the request id from the MDC
		MDC.put(Fields.REQUEST_ID, "test_request_id");
		converter.start();

		converter.convert(event);

		String[] keys = LogContext.getContextFieldsKeys().toArray(new String[0]);
		assertThat(MDC.getCopyOfContextMap().keySet(), hasItems(keys));
	}

	@Test
	public void initalizesAdapter() throws Exception {
		converter.start();

		verify(customFieldsAdapter).initialize(context);
	}

	@Test
	public void setsExclusionsFromOptionsList() throws Exception {
		converter.setOptionList(asList("this key", "that key"));

		converter.start();

		verify(defaultConverter).setExclusions(exclusions.capture());
		assertThat(exclusions.getValue(), hasItems("this key", "that key"));
	}

	@Test
	public void setsExclusionsFromCustomFieldsAdapter() throws Exception {
		when(customFieldsAdapter.getCustomFieldExclusions()).thenReturn(asList("this key", "that key"));

		converter.start();

		verify(defaultConverter).setExclusions(exclusions.capture());
		assertThat(exclusions.getValue(), hasItems("this key", "that key"));
	}

	@Test
	public void testAddsCustomField() throws Exception {
		converter.start();
		when(event.getArgumentArray()).thenReturn(new Object[] {CustomField.customField("this key", "this value")});
		
		converter.convert(event);
		
		verify(defaultConverter).convert(any(StringBuilder.class), eventProperties.capture());
		assertThat(eventProperties.getValue(), hasEntry("this key", "this value"));
	}

    @Test
    public void setsSendDefaultValuesFromCustomFieldsAdapter() throws Exception {
        when(customFieldsAdapter.isSendDefaultValues()).thenReturn(true);

        converter.start();

        verify(defaultConverter).setSendDefaultValues(true);
    }
}
