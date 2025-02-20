package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTest {

    private final static JsonEncoder ENCODER = new JsonEncoder();

    @BeforeAll
    static void addContextFieldSuppliers() {
        ENCODER.addContextFieldSupplier(SampleContextFieldSupplier.class.getName());
        ENCODER.addLogbackContextFieldSupplier(SampleLogbackContextFieldSupplier.class.getName());
        ENCODER.start();
    }

    @Test
    void loadsContextFieldSuppliers() {
        assertThat(ENCODER.getContextFieldSuppliers()).map(Object::getClass).map(Class::getName)
                                                      .containsExactly(SampleContextFieldSupplier.class.getName(),
                                                                       SpiContextFieldSupplier.class.getName());
    }

    @Test
    void loadsLogbackContextFieldSuppliers() {
        assertThat(ENCODER.getLogbackContextFieldSuppliers()).map(Object::getClass).map(Class::getName)
                                                             .containsExactly(BaseFieldSupplier.class.getName(),
                                                                              EventContextFieldSupplier.class.getName(),
                                                                              RequestRecordFieldSupplier.class.getName(),
                                                                              SampleLogbackContextFieldSupplier.class.getName(),
                                                                              SpiLogbackContextFieldSupplier.class.getName());
    }

    static class SampleContextFieldSupplier implements ContextFieldSupplier {
        @Override
        public Map<String, Object> get() {
            return Collections.emptyMap();
        }
    }

    public static class SpiContextFieldSupplier implements ContextFieldSupplier {

        public SpiContextFieldSupplier() {
        }

        @Override
        public Map<String, Object> get() {
            return Collections.emptyMap();
        }

    }

    static class SampleLogbackContextFieldSupplier implements LogbackContextFieldSupplier {

        @Override
        public Map<String, Object> map(ILoggingEvent event) {
            return Collections.emptyMap();
        }
    }

    public static class SpiLogbackContextFieldSupplier implements LogbackContextFieldSupplier {

        @Override
        public Map<String, Object> map(ILoggingEvent event) {
            return Collections.emptyMap();
        }
    }

}
