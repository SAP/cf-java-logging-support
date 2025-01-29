package com.sap.hcp.cf.logging.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

public class TestDoubleValue {

    @Test
    public void test() {
        double value = 123.456789;

        assertThat(new DoubleValue(value).toString(), is("123.457"));
    }

}
