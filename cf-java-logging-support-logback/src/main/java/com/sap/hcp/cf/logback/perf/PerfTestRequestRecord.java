package com.sap.hcp.cf.logback.perf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

public class PerfTestRequestRecord {

	private static final int DEF_ITERATIONS = 1000000;
	private final int iterations;
	private static final Logger LOGGER = LoggerFactory.getLogger(PerfTestRequestRecord.class);

	public PerfTestRequestRecord(int iterations) {
		if (iterations > 0) {
			this.iterations = iterations;
		} else {
			this.iterations = DEF_ITERATIONS;
		}
	}

	public static void main(String[] args) {
		new PerfTestRequestRecord(DEF_ITERATIONS).run(args);
	}

	private void run(String[] args) {
		long start = System.nanoTime();
		PrintStream defOut = System.out;
		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
			}
		}));
		for (int i = 0; i < iterations; i++) {
			RequestRecord rrec = new RequestRecord(PerfTestRequestRecord.class.getName());
			LOGGER.info(Markers.REQUEST_MARKER, rrec.toString());
		}
		double delta = (System.nanoTime() - start) / 1000000.0;
		System.setOut(defOut);
		System.out.println("Writing " + iterations + " records took " + delta + " msecs, " + delta / iterations
				+ " msecs per record");
	}

}
