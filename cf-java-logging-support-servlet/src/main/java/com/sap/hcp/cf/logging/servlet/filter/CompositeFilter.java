package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * The {@link CompositeFilter} allows to group several servlet {@link Filter}
 * into one. This is used to allow customizable filters and provide a backwards
 * compatible {@link RequestLoggingFilter}. The {@link FilterConfig} is
 * forwarded to all filters, that are grouped. This may lead to
 * incompatibilities between those shared configurations.
 * </p>
 * 
 * <p>
 * You can easily create a subclass of {@link CompositeFilter} and add all the
 * filters from com.sap.hcp.cf.logging.servlet.filter you wish. You can even
 * bring your own filters. Make sure, there are no incompatible
 * {@link FilterConfig} initializations.
 * </p>
 */
public class CompositeFilter implements Filter {

    private final List<Filter> filters;

    public CompositeFilter(Filter... filters) {
        this.filters = new ArrayList<>(filters.length);
        Collections.addAll(this.filters, filters);
    }

    public List<Filter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    /**
     * Forwards the {@link FilterConfig} to all grouped filters. Be care to use
     * only filters, that are compatible to each other.
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        for (Filter filter: this.filters) {
            filter.init(config);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        new InternalFilterChain(chain, this.filters).doFilter(request, response);
    }

    @Override
    public void destroy() {
        for (int i = this.filters.size() - 1; i >= 0; i--) {
            Filter filter = this.filters.get(i);
            filter.destroy();
        }
    }

    private static class InternalFilterChain implements FilterChain {

        private final FilterChain originalChain;
        private final Iterator<? extends Filter> current;

        public InternalFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
            this.originalChain = chain;
            this.current = additionalFilters.iterator();
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException,
                                                                                           ServletException {
            if (current.hasNext()) {
                current.next().doFilter(request, response, this);
            } else {
                originalChain.doFilter(request, response);
            }
        }
    }

}
