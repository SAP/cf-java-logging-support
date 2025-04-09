package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DynamicLogLevelFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @BeforeEach
    void resetMockLogLevelProvider() {
        MDC.clear();
        streamMockLogLevelProvider().forEach(MockLogLevelProvider::reset);
    }

    @AfterEach
    void removesDynamicLogLevelFromMDC() {
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void loadsMockProvider() {
        assertThat(streamMockLogLevelProvider().count()).isEqualTo(1);
    }

    @Test
    void forwardsRequestToProvider() throws Exception {
        new DynamicLogLevelFilter().doFilter(request, response, chain);

        List<HttpServletRequest> requests =
                streamMockLogLevelProvider().flatMap(p -> p.getRequests().stream()).toList();

        assertThat(requests).containsExactly(request);
    }

    @Test
    void setsValidConfiguration() throws ServletException, IOException {
        streamMockLogLevelProvider().forEach(p -> p.setConfig(new DynamicLogLevelConfiguration("INFO", "my.package")));
        doAnswer(i -> {
            assertThat(MDC.getCopyOfContextMap()).containsEntry(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "INFO")
                                                 .containsEntry(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES,
                                                                "my.package");
            return null;
        }).when(chain).doFilter(request, response);

        new DynamicLogLevelFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void setsConfigurationWithoutPackage() throws ServletException, IOException {
        streamMockLogLevelProvider().forEach(p -> p.setConfig(new DynamicLogLevelConfiguration("INFO", null)));
        doAnswer(i -> {
            assertThat(MDC.getCopyOfContextMap()).containsEntry(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "INFO")
                                                 .doesNotContainKey(
                                                         DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
            return null;
        }).when(chain).doFilter(request, response);

        new DynamicLogLevelFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsConfigurationWithInvalidLevel() throws ServletException, IOException {
        streamMockLogLevelProvider().forEach(p -> p.setConfig(new DynamicLogLevelConfiguration("INVALID", null)));
        doAnswer(i -> {
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            return null;
        }).when(chain).doFilter(request, response);

        new DynamicLogLevelFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsEmptyConfiguration() throws ServletException, IOException {
        streamMockLogLevelProvider().forEach(p -> p.setConfig(DynamicLogLevelConfiguration.EMPTY));
        doAnswer(i -> {
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            return null;
        }).when(chain).doFilter(request, response);

        new DynamicLogLevelFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsNullConfiguration() throws ServletException, IOException {
        streamMockLogLevelProvider().forEach(p -> p.setConfig(null));
        doAnswer(i -> {
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            return null;
        }).when(chain).doFilter(request, response);

        new DynamicLogLevelFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private static Stream<MockLogLevelProvider> streamMockLogLevelProvider() {
        return DynamicLogLevelFilter.getDynamicLogLevelProviders().stream()
                                    .filter(p -> p instanceof MockLogLevelProvider).map(p -> (MockLogLevelProvider) p);
    }

    public static class MockLogLevelProvider implements DynamicLogLevelProvider {

        private DynamicLogLevelConfiguration config = DynamicLogLevelConfiguration.EMPTY;

        private final List<HttpServletRequest> requests = new ArrayList<>();

        public MockLogLevelProvider() {
        }

        private void setConfig(DynamicLogLevelConfiguration config) {
            this.config = config;
        }

        private void reset() {
            requests.clear();
        }

        @Override
        public DynamicLogLevelConfiguration apply(HttpServletRequest request) {
            requests.add(request);
            return config;
        }

        private List<HttpServletRequest> getRequests() {
            return requests;
        }
    }
}
