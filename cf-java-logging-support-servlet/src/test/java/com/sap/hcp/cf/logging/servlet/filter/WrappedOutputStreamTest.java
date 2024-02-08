package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ServletOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WrappedOutputStreamTest {

    @Mock
    private ServletOutputStream out;

    @InjectMocks
    private WrappedOutputStream wrapper;

    @Test
    public void delegatesClose() throws Exception {
        wrapper.close();
        verify(out).close();
    }

    @Test
    public void delegatesFlush() throws Exception {
        wrapper.flush();
        verify(out).flush();
    }

    @Test
    public void emptyStreamReportsMinusOneBytes() throws Exception {
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(-1L);
    }

    @Test
    public void writeOneByteIncreasesByteCountByOne() throws Exception {
        wrapper.write(0);
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(1L);
        verify(out).write(0);
    }

    @Test
    public void repeatedlyWritingSingleBytesIncreasesByteCountCorrectly() throws Exception {
        for (int i = 0; i < 10; i++) {
            wrapper.write(0);
        }
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(10L);
        verify(out, times(10)).write(0);
    }

    @Test
    public void writingBufferAddsLengthToByteCount() throws Exception {
        byte[] b = new byte[13];
        wrapper.write(b);
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(13L);
        verify(out).write(b);
    }

    @Test
    public void writingBufferWithOffsetAddsLengthToByteCount() throws Exception {
        byte[] b = new byte[13];
        wrapper.write(b, 3, 10);
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(10L);
        verify(out).write(b, 3, 10);
    }

    @Test
    public void printingAsciiStringAddsLengthToByteCount() throws Exception {
        wrapper.print("Testing");
        Assertions.assertThat(wrapper.getContentLength()).isEqualTo(7L);
    }
}
