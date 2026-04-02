package com.sap.hcp.cf.logging.common.serialization;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ContextFieldSupplierServiceLoaderTest {

    public static class ValidSupplier implements ContextFieldSupplier {
        @Override
        public Map<String, Object> get() {
            return Collections.emptyMap();
        }
    }

    @Test
    void doesNotThrowWhenSpiProviderIsInvalid() {
        assertThatCode(() -> ContextFieldSupplierServiceLoader.addFieldSuppliers(Stream.empty(),
                                                                                 ContextFieldSupplier.class))
                .doesNotThrowAnyException();
    }

    @Test
    void skipsInvalidSpiProviderAndKeepsValidOnes() {
        ValidSupplier valid = new ValidSupplier();

        ArrayList<ContextFieldSupplier> result = ContextFieldSupplierServiceLoader.addFieldSuppliers(
                Stream.of(valid), ContextFieldSupplier.class);

        assertThat(result).contains(valid);
    }

}
