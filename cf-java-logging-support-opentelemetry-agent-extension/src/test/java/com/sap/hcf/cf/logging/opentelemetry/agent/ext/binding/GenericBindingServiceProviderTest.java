package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericBindingServiceProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CloudFoundryServiceInstance instance;

    @Test
    void returnsEmptyWhenNoInstances() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.empty());
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-binding");

        assertThat(provider.get()).isEmpty();
    }

    @Test
    void findsInstanceByName() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(instance.getName()).thenReturn("my-binding");
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-binding");

        assertThat(provider.get()).contains(instance);
    }

    @Test
    void doesNotMatchWhenNameDiffers() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(instance.getName()).thenReturn("other-binding");
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-binding");

        assertThat(provider.get()).isEmpty();
    }

    @Test
    void findsInstanceByLabel() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(instance.getLabel()).thenReturn("my-label");
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-label");

        assertThat(provider.get()).contains(instance);
    }

    @Test
    void findsInstanceByTag() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(instance.getTags()).thenReturn(List.of("my-tag"));
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-tag");

        assertThat(provider.get()).contains(instance);
    }

    @Test
    void returnsFirstInstanceWhenMultipleMatch() {
        CloudFoundryServiceInstance first = mock(CloudFoundryServiceInstance.class);
        CloudFoundryServiceInstance second = mock(CloudFoundryServiceInstance.class);
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(first, second));
        when(first.getName()).thenReturn("my-binding");
        lenient().when(second.getName()).thenReturn("my-binding");
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-binding");

        assertThat(provider.get()).contains(first);
    }

    @Test
    void doesNotMatchWhenTagsAreNull() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(instance.getName()).thenReturn("other");
        when(instance.getTags()).thenReturn(null);
        GenericBindingServiceProvider provider = new GenericBindingServiceProvider(adapter, "my-tag");

        assertThat(provider.get()).isEmpty();
    }
}
