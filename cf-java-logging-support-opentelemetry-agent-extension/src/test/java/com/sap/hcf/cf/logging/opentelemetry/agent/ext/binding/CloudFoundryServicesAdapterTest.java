package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class CloudFoundryServicesAdapterTest {
    private static final String DEFAULT_VCAP_SERVICES = """
            {
              "managed-find-me-service": [
                {
                  "label": "managed-find-me-service",
                  "tags": ["Find Me!"],
                  "name": "managed-find-me1"
                },
                {
                  "label": "managed-find-me-service",
                  "tags": ["Find Me!"],
                  "name": "managed-find-me2"
                },
                {
                  "label": "managed-find-me-service",
                  "tags": ["You can't see me!"],
                  "name": "managed-other"
                }
              ],
              "managed-notice-me-not-service": [
                {
                  "label": "managed-notice-me-not-service",
                  "tags": ["Find Me!"],
                  "name": "managed-other1"
                },
                {
                  "label": "managed-notice-me-not-service",
                  "tags": ["You can't see me!"],
                  "name": "managed-other2"
                }
              ],
              "user-provided": [
                {
                  "label": "user-provided",
                  "tags": ["Find Me!"],
                  "name": "ups-find-me1"
                },
                {
                  "label": "user-provided",
                  "tags": ["Find Me!"],
                  "name": "ups-find-me2"
                },
                {
                  "label": "user-provided",
                  "tags": ["You can't see me!"],
                  "name": "ups-other"
                }
              ]
            }""";

    private static final CloudFoundryServicesAdapter DEFAULT_ADAPTER =
            new CloudFoundryServicesAdapter(DEFAULT_VCAP_SERVICES);

    @Test
    void getsAllServicesWithNullParameters() {
        List<CloudFoundryServiceInstance> services = DEFAULT_ADAPTER.stream(null, null).toList();
        assertServiceNames(services).containsExactly("managed-find-me1", "managed-find-me2", "managed-other",
                                                     "managed-other1", "managed-other2", "ups-find-me1", "ups-find-me2",
                                                     "ups-other");

    }

    private static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertServiceNames(
            List<CloudFoundryServiceInstance> services) {
        return assertThat(services).extracting(CloudFoundryServiceInstance::getName);
    }

    @Test
    void filtersBySingleLabel() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(Collections.singletonList("managed-find-me-service"), emptyList()).toList();
        assertServiceNames(services).containsExactlyInAnyOrder("managed-find-me1", "managed-find-me2", "managed-other");
    }

    @Test
    void priotizesByServiceLabel() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(asList("user-provided", "managed-find-me-service"), emptyList()).toList();
        assertServiceNames(services).containsExactly("ups-find-me1", "ups-find-me2", "ups-other", "managed-find-me1",
                                                     "managed-find-me2", "managed-other");
    }

    @Test
    void filtersBySingleTag() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(emptyList(), Collections.singletonList("Find Me!")).toList();
        assertServiceNames(services).containsExactlyInAnyOrder("managed-find-me1", "managed-find-me2", "managed-other1",
                                                               "ups-find-me1", "ups-find-me2");
    }

    @Test
    void standardUseCase() {
        List<CloudFoundryServiceInstance> services =
                DEFAULT_ADAPTER.stream(asList("user-provided", "managed-find-me-service"),
                                       Collections.singletonList("Find Me!")).collect(toList());
        assertServiceNames(services).containsExactly("ups-find-me1", "ups-find-me2", "managed-find-me1",
                                                     "managed-find-me2");
    }

}
