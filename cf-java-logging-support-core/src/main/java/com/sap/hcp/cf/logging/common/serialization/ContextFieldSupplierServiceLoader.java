package com.sap.hcp.cf.logging.common.serialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextFieldSupplierServiceLoader<T> {

    private static Logger logger() {
        return LoggerFactory.getLogger(ContextFieldSupplierServiceLoader.class);
    }

    private ContextFieldSupplierServiceLoader() {
    }

    public static <T extends ContextFieldSupplier> ArrayList<T> addFieldSuppliers(Stream<T> original, Class<T> clazz) {
        Stream<T> spiSuppliers = loadSafely(clazz);
        return Stream.concat(original, spiSuppliers).sorted().collect(Collectors.toCollection(ArrayList::new));
    }

    private static <T extends ContextFieldSupplier> Stream<T> loadSafely(Class<T> clazz) {
        ArrayList<T> result = new ArrayList<>();
        var iterator = ServiceLoader.load(clazz).iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) break;
                result.add(iterator.next());
            } catch (ServiceConfigurationError e) {
                logger().warn("Skipping invalid SPI provider for {}: {}", clazz.getName(), e.getMessage());
            }
        }
        return result.stream();
    }

}
