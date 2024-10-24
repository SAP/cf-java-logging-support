package com.sap.cloud.cf.monitoring.spring.configuration;

import com.sap.cloud.cf.monitoring.client.MonitoringClient;
import com.sap.cloud.cf.monitoring.client.configuration.SystemGetEnvWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CustomMetricsTestBase {

    protected MockedStatic<SystemGetEnvWrapper> systemGetEnvWrapper;

    @BeforeEach
    public void gearUpSystemGetEnvWrapper() {
        systemGetEnvWrapper = mockStatic(SystemGetEnvWrapper.class);
    }

    @AfterEach
    public void tearDownSystemGetEnvWrapper() {
        systemGetEnvWrapper.close();
    }

    public static Map<String, String> convertEnvArrayIntoEnvMap(String[][] envArray) {
        return  Stream.of(envArray)
                .collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

}
