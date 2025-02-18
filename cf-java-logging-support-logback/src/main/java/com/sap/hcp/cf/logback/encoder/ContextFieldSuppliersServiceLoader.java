package com.sap.hcp.cf.logback.encoder;

import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextFieldSuppliersServiceLoader {

    private ContextFieldSuppliersServiceLoader() {
    }

    public static ArrayList<ContextFieldSupplier> addSpiContextFieldSuppliers(
            List<ContextFieldSupplier> fieldSuppliers) {
        return addFieldSupplier(fieldSuppliers, ContextFieldSupplier.class);
    }

    private static <T extends ContextFieldSupplier> ArrayList<T> addFieldSupplier(List<T> original, Class<T> clazz) {
        Stream<T> spiSuppliers = ServiceLoader.load(clazz).stream().map(ServiceLoader.Provider::get).sorted();
        return Stream.concat(original.stream(), spiSuppliers).sorted().collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<LogbackContextFieldSupplier> addSpiLogbackContextFieldSuppliers(
            List<LogbackContextFieldSupplier> original) {
        return addFieldSupplier(original, LogbackContextFieldSupplier.class);
    }
}
