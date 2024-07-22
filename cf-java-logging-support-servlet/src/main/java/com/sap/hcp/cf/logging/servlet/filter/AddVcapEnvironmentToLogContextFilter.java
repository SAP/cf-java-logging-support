package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.LogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * The {@link AddVcapEnvironmentToLogContextFilter} extracts CF specific metadata about the running application and adds
 * it to the log context. The metadata includes the app, org and space names and ids. This allows to attribute a
 * generated log message to the correct source from the message alone.
 * </p>
 *
 * <p>
 * <b>Note:</b> This metadata can usually also be obtained from the CF channel
 * shipping the logs, e.g. syslog structured data. Hence, depending on the use-case, it may not be required to add the
 * metadata with this filter.
 * </p>
 */
public class AddVcapEnvironmentToLogContextFilter extends AbstractLoggingFilter {

    @Override
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
        LogContext.loadContextFields();
    }

    @Override
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
        LogContext.resetContextFields();
    }
}
