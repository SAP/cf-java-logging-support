package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

class PEMUtil {
    @NotNull
    static byte[] read(String resourceName) throws IOException {
        try (InputStream is = PEMUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new FileNotFoundException("Resource " + resourceName + " not found.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
        }
    }
}
