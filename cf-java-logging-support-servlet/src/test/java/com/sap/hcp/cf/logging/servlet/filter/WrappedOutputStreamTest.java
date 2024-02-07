package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.ServletOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
		assertThat(wrapper.getContentLength(), is(-1L));
	}

	@Test
	public void writeOneByteIncreasesByteCountByOne() throws Exception {
		wrapper.write(0);
		assertThat(wrapper.getContentLength(), is(1L));
		verify(out).write(0);
	}

	@Test
	public void repeatedlyWritingSingleBytesIncreasesByteCountCorrectly() throws Exception {
		for (int i = 0; i < 10; i++) {
			wrapper.write(0);
		}
		assertThat(wrapper.getContentLength(), is(10L));
		verify(out, times(10)).write(0);
	}

	@Test
	public void writingBufferAddsLengthToByteCount() throws Exception {
		byte[] b = new byte[13];
		wrapper.write(b);
		assertThat(wrapper.getContentLength(), is(13L));
		verify(out).write(b);
	}

	@Test
	public void writingBufferWithOffsetAddsLengthToByteCount() throws Exception {
		byte[] b = new byte[13];
		wrapper.write(b, 3, 10);
		assertThat(wrapper.getContentLength(), is(10L));
		verify(out).write(b, 3, 10);
	}

	@Test
	public void printingAsciiStringAddsLengthToByteCount() throws Exception {
		wrapper.print("Testing");
		assertThat(wrapper.getContentLength(), is(7L));
	}
}
