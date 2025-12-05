package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanitizeSpanExporterCustomizerTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private SpanData spanData;

    @Mock
    private SpanExporter delegateExporter;

    @Captor
    private ArgumentCaptor<List<SpanData>> spanDataCaptor;

    private SpanExporter sanitizeExporter;

    @BeforeEach
    void setUp() {
        when(spanData.getName()).thenReturn("test-span");
        this.sanitizeExporter = new SanitizeSpanExporterCustomizer().apply(delegateExporter, null);
    }

    @Test
    void forwardsSpanWithoutAttributes() {
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spans);
    }

    @Test
    void forwardsSpanWithEmptyAttributes() {
        List<SpanData> spans = List.of(spanData);
        when(spanData.getAttributes()).thenReturn(Attributes.empty());
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spans);
    }

    @Test
    void forwardsSpanWithoutSensitiveAttributeKey() {
        Attributes attributes = Attributes.builder().put("some.key", "some value").build();
        when(spanData.getAttributes()).thenReturn(attributes);
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spans);
    }

    @Test
    void forwardsSpanWithSensitiveAttributeKeyButWithoutSensitiveValue() {
        Attributes attributes = Attributes.builder().put("db.query.text", "some safe value").build();
        when(spanData.getAttributes()).thenReturn(attributes);
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spans);
    }

    @Test
    void redactsSensitiveDbQueryTextValue() {
        Attributes attributes = Attributes.builder().put("db.query.text", "Connect somewhere").build();
        when(spanData.getAttributes()).thenReturn(attributes);
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spanDataCaptor.capture());
        SpanData sanitizedSpan = spanDataCaptor.getValue().get(0);
        assertThat(sanitizedSpan).extracting(SpanData::getName).isEqualTo("test-span");
        assertThat(sanitizedSpan).extracting(SpanData::getAttributes)
                                 .extracting(attrs -> attrs.get(AttributeKey.stringKey("db.query.text")))
                                 .isEqualTo("Connect [REDACTED]");
    }

    @Test
    void redactsSensitiveDbStatementValue() {
        Attributes attributes = Attributes.builder().put("db.statement", "CONNECT somewhere").build();
        when(spanData.getAttributes()).thenReturn(attributes);
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spanDataCaptor.capture());
        SpanData sanitizedSpan = spanDataCaptor.getValue().get(0);
        assertThat(sanitizedSpan).extracting(SpanData::getName).isEqualTo("test-span");
        assertThat(sanitizedSpan).extracting(SpanData::getAttributes)
                                 .extracting(attrs -> attrs.get(AttributeKey.stringKey("db.statement")))
                                 .isEqualTo("CONNECT [REDACTED]");
    }

    @Test
    void keepsOtherAttributesOnRedaction() {
        Attributes attributes =
                Attributes.builder().put("db.query.text", "connect somewhere").put("some.key", "some.value").build();
        when(spanData.getAttributes()).thenReturn(attributes);
        List<SpanData> spans = List.of(spanData);
        sanitizeExporter.export(spans);

        verify(delegateExporter).export(spanDataCaptor.capture());
        SpanData sanitizedSpan = spanDataCaptor.getValue().get(0);
        assertThat(sanitizedSpan).extracting(SpanData::getName).isEqualTo("test-span");
        assertThat(sanitizedSpan).extracting(SpanData::getAttributes)
                                 .extracting(attrs -> attrs.get(AttributeKey.stringKey("db.query.text")))
                                 .isEqualTo("connect [REDACTED]");
        assertThat(sanitizedSpan).extracting(SpanData::getAttributes)
                                 .extracting(attrs -> attrs.get(AttributeKey.stringKey("some.key")))
                                 .isEqualTo("some.value");
    }

    @Test
    void canBeDisabledViaConfig() {
        Map<String, String> configEntries = new HashMap<>();
        configEntries.put("sap.cf.integration.otel.extension.sanitizer.enabled", "false");
        DefaultConfigProperties configProperties = DefaultConfigProperties.createFromMap(configEntries);
        SpanExporter spanExporter = new SanitizeSpanExporterCustomizer().apply(delegateExporter, configProperties);
        assertThat(spanExporter).isSameAs(delegateExporter);
    }

    @Test
    void delegatesFlush() {
        sanitizeExporter.flush();
        verify(delegateExporter).flush();
    }

    @Test
    void delegatesShutdown() {
        sanitizeExporter.shutdown();
        verify(delegateExporter).shutdown();
    }
}
