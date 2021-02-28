package com.sap.hcp.cf.logging.jersey.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LongValue;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class ResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHandler.class);

	public void handle(ResponseContextAdapter responseContext, RequestRecord rr) {
		if (rr != null) {
			rr.addValue(Fields.RESPONSE_SIZE_B, new LongValue(responseContext.getLength()));
			rr.addTag(Fields.RESPONSE_CONTENT_TYPE, responseContext.getHeader(HttpHeaders.CONTENT_TYPE));
			rr.addValue(Fields.RESPONSE_STATUS, new LongValue(responseContext.getStatus()));
			rr.stop();
			LOGGER.info(Markers.REQUEST_MARKER, "{}", rr);
		} else {
			LOGGER.error("No record found to handle response {}", responseContext);
		}
	}
}
