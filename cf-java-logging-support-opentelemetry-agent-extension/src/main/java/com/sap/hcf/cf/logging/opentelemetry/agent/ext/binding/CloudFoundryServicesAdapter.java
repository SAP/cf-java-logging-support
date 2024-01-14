package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.util.List;
import java.util.stream.Stream;

class CloudFoundryServicesAdapter {

    private final CfEnv cfEnv;

    CloudFoundryServicesAdapter(CfEnv cfEnv) {
        this.cfEnv = cfEnv;
    }

    /**
     * Stream CfServices, that match the provided properties. Empty or null values are interpreted as not applicable. No
     * check will be performed during search. User-provided service instances will be preferred unless the
     * {@code userProvidedLabel is null or empty. Provided only null values will return all service instances.
     *
     * @param serviceLabels the labels of services
     * @param serviceTags   the tags of services
     * @return a stream of service instances present in the CloudFoundry environment variable VCAP_SERVICES
     */
    Stream<CfService> stream(List<String> serviceLabels, List<String> serviceTags) {
        Stream<CfService> services;
        if (serviceLabels == null || serviceLabels.isEmpty())
            services = cfEnv.findAllServices().stream();
        else {
            services = serviceLabels.stream().flatMap(l -> cfEnv.findServicesByLabel(l).stream());
        }
        if (serviceTags == null || serviceTags.isEmpty()) {
            return services;
        }
        return services.filter(svc -> svc.existsByTagIgnoreCase(serviceTags.toArray(new String[0])));
    }

}
