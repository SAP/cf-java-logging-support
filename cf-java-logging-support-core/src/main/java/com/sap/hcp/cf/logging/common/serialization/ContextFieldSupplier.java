package com.sap.hcp.cf.logging.common.serialization;

import java.util.Map;
import java.util.function.Supplier;

@FunctionalInterface
public interface ContextFieldSupplier extends Supplier<Map<String, Object>>, Comparable<ContextFieldSupplier> {

    default int order() {
        return 0;
    }

    @Override
    default int compareTo(ContextFieldSupplier other) {
        return order() - other.order();
    }
}
