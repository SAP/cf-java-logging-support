package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PemFileCreatorTest {

    @Test
    void shouldCreateFileWithValidContent() throws IOException {
        PemFileCreator creator = new PemFileCreator();
        String content = "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----";

        File file = creator.writeFile("test-cert", ".pem", content);

        assertThat(file).isNotNull().exists().isFile().hasName(file.getName()).content().isEqualTo(content);
        assertThat(file.getName()).startsWith("test-cert").endsWith(".pem");
    }

    @Test
    void shouldPreserveContentExactly() throws IOException {
        PemFileCreator creator = new PemFileCreator();
        String content = "Line1\nLine2\nLine3\n";

        File file = creator.writeFile("test", ".txt", content);

        assertThat(file).content().isEqualTo(content);
    }

    @Test
    void shouldCreateEmptyFile() throws IOException {
        PemFileCreator creator = new PemFileCreator();

        File file = creator.writeFile("empty", ".pem", "");

        assertThat(file).exists().isEmpty().canRead();
    }

    @Test
    void shouldCreateMultipleFiles() throws IOException {
        PemFileCreator creator = new PemFileCreator();

        File file1 = creator.writeFile("test1", ".pem", "content1");
        File file2 = creator.writeFile("test2", ".pem", "content2");

        assertThat(file1.getAbsolutePath()).isNotEqualTo(file2.getAbsolutePath());
        assertThat(file1).content().isEqualTo("content1");
        assertThat(file2).content().isEqualTo("content2");
    }

    @Test
    void shouldPropagateIOException() {
        PemFileCreator creator = new PemFileCreator();

        assertThatThrownBy(() -> creator.writeFile("\0invalid", ".pem", "content")).isInstanceOf(IOException.class);
    }
    
}
