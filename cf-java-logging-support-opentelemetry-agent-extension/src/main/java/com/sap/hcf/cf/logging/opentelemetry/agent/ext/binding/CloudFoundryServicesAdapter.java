package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

class CloudFoundryServicesAdapter {

    private static final Logger LOG = Logger.getLogger(CloudFoundryServicesAdapter.class.getName());

    private static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static final String SERVICE_NAME = "name";
    private static final String SERVICE_TAGS = "tags";
    private static final String SERVICE_CREDENTIALS = "credentials";
    private static final String SERVICE_CREDENTIALS_ENDPOINT = "ingest-otlp-endpoint";
    private static final String SERVICE_CREDENTIALS_CLIENT_KEY = "ingest-otlp-key";

    private final String vcapServicesJson;

    public CloudFoundryServicesAdapter() {
        this(System.getenv(VCAP_SERVICES));
    }

    CloudFoundryServicesAdapter(String vcapServicesJson) {
        this.vcapServicesJson = vcapServicesJson;
    }

    /**
     * Stream CfServices, that match the provided properties. Empty or null values are interpreted as not applicable. No
     * check will be performed during search. User-provided service instances will be preferred unless the
     * {@code userProvidedLabel is null or empty. Provided only null values will return all service instances.
     *
     * @param serviceLabels
     *         the labels of services
     * @param serviceTags
     *         the tags of services
     * @return a stream of service instances present in the CloudFoundry environment variable VCAP_SERVICES
     */
    Stream<CloudFoundryServiceInstance> stream(List<String> serviceLabels, List<String> serviceTags) {
        if (vcapServicesJson == null) {
            LOG.info("No environment variable " + VCAP_SERVICES + "found. Skipping service binding detection.");
            return Stream.empty();
        }
        try (JsonParser parser = new JsonFactory().createParser(vcapServicesJson)) {
            parser.nextToken();
            List<CloudFoundryServiceInstance> services = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String label = parser.currentName();
                if (isNullOrEmpty(serviceLabels) || serviceLabels.contains(label)) {
                    parseServiceInstances(parser, label, serviceInstance -> {
                        if (serviceInstance.getName() != null) {
                            if (hasServiceTag(serviceTags, serviceInstance.getTags())) {
                                services.add(serviceInstance);
                            }
                        }
                    });
                } else {
                    parser.skipChildren();
                }
            }
            if (isNullOrEmpty(serviceLabels)) {
                return services.stream();
            } else {
                return services.stream().sorted(byLabels(serviceLabels));
            }
        } catch (JsonParseException cause) {
            LOG.warning("Invalid JSON content in environment variable " + VCAP_SERVICES);
        } catch (IOException cause) {
            LOG.warning("Cannot parse content of environment variable " + VCAP_SERVICES);
        }
        return Stream.empty();
    }

    private static void parseServiceInstances(JsonParser parser, String label,
                                              Consumer<CloudFoundryServiceInstance> consumer) throws IOException {
        if (parser.nextToken() == JsonToken.START_ARRAY) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    CloudFoundryServiceInstance serviceInstance = parseServiceInstance(label, parser);
                    consumer.accept(serviceInstance);
                }
            }
        }
    }

    private static boolean isNullOrEmpty(List<String> items) {
        return items == null || items.isEmpty();
    }

    private static CloudFoundryServiceInstance parseServiceInstance(String label, JsonParser parser)
            throws IOException {
        CloudFoundryServiceInstance.Builder builder = CloudFoundryServiceInstance.builder().label(label);
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.currentName()) {
            case SERVICE_NAME:
                parseServiceName(builder, parser);
                break;
            case SERVICE_TAGS:
                parserServiceTags(parser, builder);
                break;
            case SERVICE_CREDENTIALS:
                parseServiceCredentials(parser, builder);
                break;
            default:
                parser.skipChildren();
            }
        }
        return builder.build();
    }

    private static void parseServiceName(CloudFoundryServiceInstance.Builder builder, JsonParser parser)
            throws IOException {
        builder.name(parser.getValueAsString());
    }

    private static void parserServiceTags(JsonParser parser, CloudFoundryServiceInstance.Builder builder)
            throws IOException {
        if (parser.nextToken() == JsonToken.START_ARRAY) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                builder.tag(parser.getValueAsString());
            }
        }
    }

    private static void parseServiceCredentials(JsonParser parser, CloudFoundryServiceInstance.Builder builder)
            throws IOException {
        if (parser.nextToken() == JsonToken.START_OBJECT) {
            CloudFoundryCredentials.Builder credentials = CloudFoundryCredentials.builder();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken().isScalarValue()) {
                    credentials.add(parser.currentName(), parser.getValueAsString());
                } else {
                    parser.skipChildren();
                }
            }
            builder.credentials(credentials.build());
        }
    }

    private int getIndex(List<String> serviceLabels, String label) {
        if (label == null) {
            return Integer.MAX_VALUE;
        }
        int index = serviceLabels.indexOf(label);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private boolean hasServiceTag(List<String> requiredTags, List<String> instanceTags) {
        if (isNullOrEmpty(requiredTags)) {
            return true;
        }
        if (isNullOrEmpty(instanceTags)) {
            return false;
        }
        return instanceTags.containsAll(requiredTags);
    }

    private Comparator<CloudFoundryServiceInstance> byLabels(List<String> serviceLabels) {
        return (l, r) -> getIndex(serviceLabels, l.getLabel()) - getIndex(serviceLabels, r.getLabel());
    }
}
