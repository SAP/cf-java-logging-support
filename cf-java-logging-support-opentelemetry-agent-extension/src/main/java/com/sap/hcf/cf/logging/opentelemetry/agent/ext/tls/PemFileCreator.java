package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.logging.Logger;

public class PemFileCreator {

    private static final Logger LOG = Logger.getLogger(PemFileCreator.class.getName());

    private static final Set<PosixFilePermission> OWNER_READ_WRITE =
            PosixFilePermissions.fromString("rw-------");

    public File writeFile(String prefix, String suffix, String content) throws IOException {
        Path path;
        try {
            path = Files.createTempFile(prefix, suffix,
                    PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE));
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (e.g. Windows): fall back and restrict after creation
            path = Files.createTempFile(prefix, suffix);
            File f = path.toFile();
            f.setReadable(false, false);
            f.setWritable(false, false);
            f.setReadable(true, true);
            f.setWritable(true, true);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid path for temp file: " + e.getMessage(), e);
        }
        path.toFile().deleteOnExit();
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.append(content);
            LOG.fine("Created temporary file " + path.toAbsolutePath());
        }
        return path.toFile();
    }
}
