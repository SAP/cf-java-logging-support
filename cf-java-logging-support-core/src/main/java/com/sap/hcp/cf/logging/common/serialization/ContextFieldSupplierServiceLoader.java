package com.sap.hcp.cf.logging.common.serialization;

import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextFieldSupplierServiceLoader<T> {

    private ContextFieldSupplierServiceLoader() {
    }

    public static <T extends ContextFieldSupplier> ArrayList<T> addFieldSuppliers(Stream<T> original, Class<T> clazz) {
        Stream<T> spiSuppliers = ServiceLoader.load(clazz).stream().map(ServiceLoader.Provider::get).sorted();
        return Stream.concat(original, spiSuppliers).sorted().collect(Collectors.toCollection(ArrayList::new));
    }

}
