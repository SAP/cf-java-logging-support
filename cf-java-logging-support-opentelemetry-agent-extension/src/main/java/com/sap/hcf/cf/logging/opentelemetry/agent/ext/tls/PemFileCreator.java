package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class PemFileCreator {

    private static final Logger LOG = Logger.getLogger(PemFileCreator.class.getName());

    public File writeFile(String prefix, String suffix, String content) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        try (FileWriter writer = new FileWriter(file)) {
            writer.append(content);
            LOG.fine("Created temporary file " + file.getAbsolutePath());
        }
        return file;
    }
}
