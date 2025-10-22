package com.sap.hcf.cf.logging.opentelemetry.agent.ext.testing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

public final class ResourceAssertions {

    private ResourceAssertions() {
    }

    public static AbstractObjectAssert<?, String> assertStringAttribute(Resource resource, String key) {
        return Assertions.assertThat(resource).extracting(r -> r.getAttribute(AttributeKey.stringKey(key)));
    }
}
