package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Value;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.assertLastEventFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, ConsoleExtension.class })
public class RequestLoggerTest {

    @Mock
    private ContentLengthTrackingResponseWrapper responseWrapper;

    @Mock
    private RequestRecord requestRecord;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Captor
    private ArgumentCaptor<Value> valueCaptor;

    private RequestLogger createLoggerWithoutResponse(HttpServletResponse response) {
        return new RequestLogger(requestRecord, httpRequest, response);
    }

    @Test
    public void stopsRequestRecord() throws Exception {
        createLoggerWithoutResponse(httpResponse).logRequest();
        verify(requestRecord).stop();
    }

    @Test
    public void addsHttpStatusAsValue() throws Exception {
        when(httpResponse.getStatus()).thenReturn(123);
        createLoggerWithoutResponse(httpResponse).logRequest();
        verify(requestRecord).addValue(matches(Fields.RESPONSE_STATUS), valueCaptor.capture());
        assertThat(valueCaptor.getValue().asLong()).isEqualTo(123L);
    }

    @Test
    public void addsResponseContentTypeAsTag() throws Exception {
        mockGetHeader(HttpHeaders.CONTENT_LENGTH, "13");
        mockGetHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.test");
        createLoggerWithoutResponse(httpResponse).logRequest();
        verify(requestRecord).addTag(Fields.RESPONSE_CONTENT_TYPE, "application/vnd.test");
    }

    private void mockGetHeader(HttpHeaders header, String value) {
        when(httpResponse.getHeader(header.getName())).thenReturn(value);
    }

    @Test
    public void addsRequestContentLengthAsValue() throws Exception {
        when(httpRequest.getContentLength()).thenReturn(12345);
        createLoggerWithoutResponse(httpResponse).logRequest();
        verify(requestRecord).addValue(matches(Fields.REQUEST_SIZE_B), valueCaptor.capture());
        assertThat(valueCaptor.getValue().asLong()).isEqualTo(12345L);
    }

    @Test
    public void addsResponseContentLengthAsValueFromHeaderIfAvailable() throws Exception {
        mockGetHeader(HttpHeaders.CONTENT_LENGTH, "1234");
        createLoggerWithoutResponse(httpResponse).logRequest();
        verify(requestRecord).addValue(matches(Fields.RESPONSE_SIZE_B), valueCaptor.capture());
        verifyNoInteractions(responseWrapper);
        assertThat(valueCaptor.getValue().asLong()).isEqualTo(1234L);
    }

    @Test
    public void addsResponseContentLengthAsValueFromWrapperAsFAllback() throws Exception {
        when(responseWrapper.getContentLength()).thenReturn(1234L);
        createLoggerWithoutResponse(responseWrapper).logRequest();
        verify(requestRecord).addValue(matches(Fields.RESPONSE_SIZE_B), valueCaptor.capture());
        assertThat(valueCaptor.getValue().asLong()).isEqualTo(1234L);
    }

    @Test
    public void writesRequestLogWithMDCEntries(ConsoleOutput console) throws Exception {
        Map<String, String> mdcAttributes = new HashMap<>();
        mdcAttributes.put("this-key", "this-value");
        mdcAttributes.put("that-key", "that-value");
        when(httpRequest.getAttribute(MDC.class.getName())).thenReturn(mdcAttributes);
        createLoggerWithoutResponse(httpResponse).logRequest();

        assertLastEventFields(console).containsEntry("this-key", "this-value").containsEntry("that-key", "that-value");

    }

}
