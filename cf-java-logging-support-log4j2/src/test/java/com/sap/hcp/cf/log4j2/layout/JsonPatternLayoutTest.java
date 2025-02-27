package com.sap.hcp.cf.log4j2.layout;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.BaseFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.EventContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.RequestRecordFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPatternLayoutTest {

    private final JsonPatternLayout LAYOUT =
            JsonPatternLayout.createLayout(StandardCharsets.UTF_8, false, 10, null, new CustomFieldElement[0],
                                           new Log4jContextFieldSupplierElement[] {
                                                   Log4jContextFieldSupplierElement.newBuilder().setClazz(
                                                           SampleLog4jContextFieldSupplier.class.getName()).build() },
                                           new ContextFieldSupplierElement[] { ContextFieldSupplierElement.newBuilder()
                                                                                                          .setClazz(
                                                                                                                  SampleContextFieldSupplier.class.getName()).build() },
                                           null);

    @Test
    void loadsContextFieldSuppliers() {
        assertThat(LAYOUT.getContextFieldSuppliers()).map(Object::getClass).map(Class::getName)
                                                     .containsExactly(SampleContextFieldSupplier.class.getName(),
                                                                      SpiContextFieldSupplier.class.getName());
    }

    @Test
    void getLog4jContextFieldSuppliers() {
        assertThat(LAYOUT.getLog4jContextFieldSuppliers()).map(Object::getClass).map(Class::getName)
                                                          .containsExactly(BaseFieldSupplier.class.getName(),
                                                                           EventContextFieldSupplier.class.getName(),
                                                                           RequestRecordFieldSupplier.class.getName(),
                                                                           SampleLog4jContextFieldSupplier.class.getName(),
                                                                           SpiLog4jContextFieldSupplier.class.getName());
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

    static class SampleLog4jContextFieldSupplier implements Log4jContextFieldSupplier {

        @Override
        public Map<String, Object> map(LogEvent event) {
            return Collections.emptyMap();
        }
    }

    public static class SpiLog4jContextFieldSupplier implements Log4jContextFieldSupplier {

        @Override
        public Map<String, Object> map(LogEvent event) {
            return Collections.emptyMap();
        }
    }
}
