import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.DefaultOtelBackendPropertiesSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultOtelBackendPropertiesSupplierTest {

    @Test
    void shouldReturnEmptyMapWhenNoSuppliersConfigured() {
        DefaultOtelBackendPropertiesSupplier supplier = DefaultOtelBackendPropertiesSupplier.builder().build();

        assertThat(supplier.get()).isEmpty();
    }

    @Test
    void shouldReturnFirstNonEmptyMap() {
        DefaultOtelBackendPropertiesSupplier supplier =
                DefaultOtelBackendPropertiesSupplier.builder().add(() -> emptyMap()).add(() -> Map.of("key1", "value1"))
                                                    .add(() -> Map.of("key2", "value2")).build();

        assertThat(supplier.get()).containsExactlyEntriesOf(Map.of("key1", "value1"));
    }

    @Test
    void shouldReturnEmptyMapWhenAllSuppliersReturnEmpty() {
        DefaultOtelBackendPropertiesSupplier supplier =
                DefaultOtelBackendPropertiesSupplier.builder().add(() -> emptyMap()).add(() -> emptyMap()).build();

        assertThat(supplier.get()).isEmpty();
    }
}
