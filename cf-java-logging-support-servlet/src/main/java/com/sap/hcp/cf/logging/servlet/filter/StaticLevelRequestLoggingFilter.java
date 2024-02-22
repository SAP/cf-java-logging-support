package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.Filter;

/**
 * The {@link StaticLevelRequestLoggingFilter} is an example of a customized {@link Filter}. It uses all filters from
 * {@link RequestLoggingFilter}, but leaves out the dynamic log level feature. This allows to exclude the java-jwt Maven
 * dependency, thus shrinking the dependency tree.
 */
public class StaticLevelRequestLoggingFilter extends CompositeFilter {

    public StaticLevelRequestLoggingFilter() {
        super(new AddVcapEnvironmentToLogContextFilter(), new AddHttpHeadersToLogContextFilter(),
              new CorrelationIdFilter(), new GenerateRequestLogFilter());
    }

}
