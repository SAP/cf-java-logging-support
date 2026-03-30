package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class CloudFoundryServicesAdapterTest {
    private static final String DEFAULT_VCAP_SERVICES = "{\n" + //
            "  \"managed-find-me-service\": [\n" + //
            "    {\n" + //
            "      \"label\": \"managed-find-me-service\",\n" + //
            "      \"tags\": [\"Find Me!\"],\n" + //
            "      \"name\": \"managed-find-me1\"\n" + //
            "    },\n" + //
            "    {\n" + //
            "      \"label\": \"managed-find-me-service\",\n" + //
            "      \"tags\": [\"Find Me!\"],\n" + //
            "      \"name\": \"managed-find-me2\"\n" + //
            "    },\n" + //
            "    {\n" + //
            "      \"label\": \"managed-find-me-service\",\n" + //
            "      \"tags\": [\"You can't see me!\"],\n" + //
            "      \"name\": \"managed-other\"\n" + //
            "    }\n" + //
            "  ],\n" + //
            "  \"managed-notice-me-not-service\": [\n" + //
            "    {\n" + //
            "      \"label\": \"managed-notice-me-not-service\",\n" + //
            "      \"tags\": [\"Find Me!\"],\n" + //
            "      \"name\": \"managed-other1\"\n" + //
            "    },\n" + //
            "    {\n" + //
            "      \"label\": \"managed-notice-me-not-service\",\n" + //
            "      \"tags\": [\"You can't see me!\"],\n" + //
            "      \"name\": \"managed-other2\"\n" + //
            "    }\n" + //
            "  ],\n" + //
            "  \"user-provided\": [\n" + //
            "    {\n" + //
            "      \"label\": \"user-provided\",\n" + //
            "      \"tags\": [\"Find Me!\"],\n" + //
            "      \"name\": \"ups-find-me1\"\n" + //
            "    },\n" + //
            "    {\n" + //
            "      \"label\": \"user-provided\",\n" + //
            "      \"tags\": [\"Find Me!\"],\n" + //
            "      \"name\": \"ups-find-me2\"\n" + //
            "    },\n" + //
            "    {\n" + //
            "      \"label\": \"user-provided\",\n" + //
            "      \"tags\": [\"You can't see me!\"],\n" + //
            "      \"name\": \"ups-other\"\n" + //
            "    }\n" + //
            "  ]\n" + //
            "}";

    static Stream<CloudFoundryServicesAdapter> adapters() {
        return Stream.of(new CloudFoundryServicesAdapter(DEFAULT_VCAP_SERVICES), createFileAdapter());
    }

    private static CloudFoundryServicesAdapter createFileAdapter() {
        try {
            String filePath = CloudFoundryServicesAdapterTest.class.getResource("vcap_services.json").toURI().getPath();
            return CloudFoundryServicesAdapter.builder(key -> filePath).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load vcap_services.json", e);
        }
    }

    @ParameterizedTest
    @MethodSource("adapters")
    void getsAllServicesWithNullParameters(CloudFoundryServicesAdapter adapter) {
        List<CloudFoundryServiceInstance> services = adapter.stream(null, null).collect(toList());
        assertServiceNames(services).containsExactly("managed-find-me1", "managed-find-me2", "managed-other",
                                                     "managed-other1", "managed-other2", "ups-find-me1", "ups-find-me2",
                                                     "ups-other");
    }

    @ParameterizedTest
    @MethodSource("adapters")
    void filtersBySingleLabel(CloudFoundryServicesAdapter adapter) {
        List<CloudFoundryServiceInstance> services =
                adapter.stream(List.of("managed-find-me-service"), emptyList()).collect(toList());
        assertServiceNames(services).containsExactlyInAnyOrder("managed-find-me1", "managed-find-me2", "managed-other");
    }

    @ParameterizedTest
    @MethodSource("adapters")
    void priotizesByServiceLabel(CloudFoundryServicesAdapter adapter) {
        List<CloudFoundryServiceInstance> services =
                adapter.stream(List.of("user-provided", "managed-find-me-service"), emptyList()).collect(toList());
        assertServiceNames(services).containsExactly("ups-find-me1", "ups-find-me2", "ups-other", "managed-find-me1",
                                                     "managed-find-me2", "managed-other");
    }

    @ParameterizedTest
    @MethodSource("adapters")
    void filtersBySingleTag(CloudFoundryServicesAdapter adapter) {
        List<CloudFoundryServiceInstance> services = adapter.stream(emptyList(), List.of("Find Me!")).collect(toList());
        assertServiceNames(services).containsExactlyInAnyOrder("managed-find-me1", "managed-find-me2", "managed-other1",
                                                               "ups-find-me1", "ups-find-me2");
    }

    @ParameterizedTest
    @MethodSource("adapters")
    void standardUseCase(CloudFoundryServicesAdapter adapter) {
        List<CloudFoundryServiceInstance> services =
                adapter.stream(List.of("user-provided", "managed-find-me-service"), List.of("Find Me!"))
                       .collect(toList());
        assertServiceNames(services).containsExactly("ups-find-me1", "ups-find-me2", "managed-find-me1",
                                                     "managed-find-me2");
    }

    private static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertServiceNames(
            List<CloudFoundryServiceInstance> services) {
        return assertThat(services).extracting(CloudFoundryServiceInstance::getName);
    }
}
