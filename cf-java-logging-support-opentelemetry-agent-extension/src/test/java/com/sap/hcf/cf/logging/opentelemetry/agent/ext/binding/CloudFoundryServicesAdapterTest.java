package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.hamcrest.FeatureMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CloudFoundryServicesAdapterTest {
    private static final String DEFAULT_VCAP_SERVICES = "{" + "\"managed-find-me-service\":[" //
            + "{\"label\":\"managed-find-me-service\", \"tags\":[\"Find Me!\"],\"name\":\"managed-find-me1\"}," //
            + "{\"label\":\"managed-find-me-service\", \"tags\":[\"Find Me!\"],\"name\":\"managed-find-me2\"}," //
            + "{\"label\":\"managed-find-me-service\", \"tags\":[\"You can't see me!\"],\"name\":\"managed-other\"}" //
            + "]," //
            + "\"managed-notice-me-not-service\":[" //
            + "{\"label\":\"managed-notice-me-not-service\", \"tags\":[\"Find Me!\"],\"name\":\"managed-other1\"}," //
            + "{\"label\":\"managed-notice-me-not-service\", \"tags\":[\"You can't see me!\"],\"name\":\"managed-other2\"}" //
            + "]," //
            + "\"user-provided\":[" //
            + "{\"label\":\"user-provided\", \"tags\":[\"Find Me!\"],\"name\":\"ups-find-me1\"}," //
            + "{\"label\":\"user-provided\", \"tags\":[\"Find Me!\"],\"name\":\"ups-find-me2\"}," //
            + "{\"label\":\"user-provided\", \"tags\":[\"You can't see me!\"],\"name\":\"ups-other\"}" //
            + "]}";

    private static final CloudFoundryServicesAdapter DEFAULT_ADAPTER =
            new CloudFoundryServicesAdapter(DEFAULT_VCAP_SERVICES);

    @NotNull
    private static FeatureMatcher<CloudFoundryServiceInstance, String> withName(String expected) {
        return new FeatureMatcher<CloudFoundryServiceInstance, String>(equalTo(expected), "name", "name") {
            @Override
            protected String featureValueOf(CloudFoundryServiceInstance cfService) {
                return cfService.getName();
            }
        };
    }

    @Test
    public void getsAllServicesWithNullParameters() {
        List<CloudFoundryServiceInstance> services = DEFAULT_ADAPTER.stream(null, null).collect(toList());
        assertThat(services, allOf(hasItem(withName("managed-find-me1")), hasItem(withName("managed-find-me2")),
                                   hasItem(withName("managed-other")), hasItem(withName("managed-other1")),
                                   hasItem(withName("managed-other2")), hasItem(withName("ups-find-me1")),
                                   hasItem(withName("ups-find-me2")), hasItem(withName("ups-other"))));
    }

    @Test
    public void filtersBySingleLabel() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(Collections.singletonList("managed-find-me-service"), emptyList())
                               .collect(toList());
        assertThat(services, allOf(hasItem(withName("managed-find-me1")), hasItem(withName("managed-find-me2")),
                                   hasItem(withName("managed-other"))));
        assertThat(services, hasSize(3));
    }

    @Test
    public void priotizesByServiceLabel() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(asList("user-provided", "managed-find-me-service"), emptyList())
                               .collect(toList());
        assertThat(services, contains(withName("ups-find-me1"), withName("ups-find-me2"), withName("ups-other"),
                                      withName("managed-find-me1"), withName("managed-find-me2"),
                                      withName("managed-other")));
    }

    @Test
    public void filtersBySingleTag() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(emptyList(), Collections.singletonList("Find Me!")).collect(toList());
        assertThat(services, allOf(hasItem(withName("managed-find-me1")), hasItem(withName("managed-find-me2")),
                                   hasItem(withName("managed-other1")), hasItem(withName("ups-find-me1")),
                                   hasItem(withName("ups-find-me2"))));
        assertThat(services, hasSize(5));
    }

    @Test
    public void standardUseCase() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(asList("user-provided", "managed-find-me-service"),
                                       Collections.singletonList("Find Me!")).collect(toList());
        assertThat(services, contains(withName("ups-find-me1"), withName("ups-find-me2"), withName("managed-find-me1"),
                                      withName("managed-find-me2")));
    }

}
