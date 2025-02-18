package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sap.hcp.cf.logback.encoder.ContextFieldSuppliersServiceLoader.addSpiContextFieldSuppliers;
import static com.sap.hcp.cf.logback.encoder.ContextFieldSuppliersServiceLoader.addSpiLogbackContextFieldSuppliers;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ContextFieldSuppliersServiceLoaderTest {

    @Test
    void loadsSpiContextFieldSuppliers() {
        List<ContextFieldSupplier> fieldSuppliers = addSpiContextFieldSuppliers(emptyList());
        assertThat(fieldSuppliers).map(s -> s.getClass().getName())
                                  .containsExactly(FirstContextFieldSupplier.class.getName(),
                                                   SecondContextFieldSupplier.class.getName());
    }

    @Test
    void stableSortsContextFieldSuppliers() {
        List<ContextFieldSupplier> fieldSuppliers =
                addSpiContextFieldSuppliers(List.of(new NoopContextFieldSupplier(2)));
        assertThat(fieldSuppliers).map(s -> s.getClass().getName())
                                  .containsExactly(FirstContextFieldSupplier.class.getName(),
                                                   NoopContextFieldSupplier.class.getName(),
                                                   SecondContextFieldSupplier.class.getName());
    }

    @Test
    void loadsSpiLogbackContextFieldSuppliers() {
        List<LogbackContextFieldSupplier> fieldSuppliers = addSpiLogbackContextFieldSuppliers(emptyList());
        assertThat(fieldSuppliers).map(s -> s.getClass().getName())
                                  .containsExactly(FirstLogbackContextFieldSupplier.class.getName(),
                                                   SecondLogbackContextFieldSupplier.class.getName());
    }

    @Test
    void stableSortsLogbackContextFieldSuppliers() {
        List<LogbackContextFieldSupplier> fieldSuppliers =
                addSpiLogbackContextFieldSuppliers(List.of(new NoopLogbackContextFieldSupplier(2)));
        assertThat(fieldSuppliers).map(s -> s.getClass().getName())
                                  .containsExactly(FirstLogbackContextFieldSupplier.class.getName(),
                                                   NoopLogbackContextFieldSupplier.class.getName(),
                                                   SecondLogbackContextFieldSupplier.class.getName());
    }

    @Test
    void retainsLegacyOrdering() {
        List<LogbackContextFieldSupplier> fieldSuppliers = addSpiLogbackContextFieldSuppliers(
                List.of(new RequestRecordFieldSupplier(), new BaseFieldSupplier(),
                        new NoopLogbackContextFieldSupplier(0), new EventContextFieldSupplier()));
        assertThat(fieldSuppliers).map(s -> s.getClass().getName()).containsExactly(BaseFieldSupplier.class.getName(),
                                                                                    EventContextFieldSupplier.class.getName(),
                                                                                    RequestRecordFieldSupplier.class.getName(),
                                                                                    NoopLogbackContextFieldSupplier.class.getName(),
                                                                                    FirstLogbackContextFieldSupplier.class.getName(),
                                                                                    SecondLogbackContextFieldSupplier.class.getName());

    }

    private static class NoopContextFieldSupplier implements ContextFieldSupplier {

        private final int order;

        private NoopContextFieldSupplier(int order) {
            this.order = order;
        }

        @Override
        public Map<String, Object> get() {
            return Collections.emptyMap();
        }

        @Override
        public int order() {
            return order;
        }
    }

    public static class FirstContextFieldSupplier extends NoopContextFieldSupplier {
        public FirstContextFieldSupplier() {
            super(1);
        }
    }

    public static class SecondContextFieldSupplier extends NoopContextFieldSupplier {
        public SecondContextFieldSupplier() {
            super(2);
        }
    }

    private static class NoopLogbackContextFieldSupplier implements LogbackContextFieldSupplier {

        private final int order;

        private NoopLogbackContextFieldSupplier(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public Map<String, Object> map(ILoggingEvent event) {
            return Collections.emptyMap();
        }
    }

    public static class FirstLogbackContextFieldSupplier extends NoopLogbackContextFieldSupplier {
        public FirstLogbackContextFieldSupplier() {
            super(1);
        }
    }

    public static class SecondLogbackContextFieldSupplier extends NoopLogbackContextFieldSupplier {
        public SecondLogbackContextFieldSupplier() {
            super(2);
        }
    }
}
