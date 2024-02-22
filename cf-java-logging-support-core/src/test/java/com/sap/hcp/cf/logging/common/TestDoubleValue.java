package com.sap.hcp.cf.logging.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDoubleValue {

    @Test
    public void test() {
        double value = 123.456789;

        assertThat(new DoubleValue(value)).asString().isEqualTo("123.457");
    }

}
