package com.sap.hcp.cf.logging.servlet.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.junit.rules.ExternalResource;

import com.fasterxml.jackson.jr.ob.JSON;

public class SystemOutRule extends ExternalResource {

	private PrintStream originalOut;
	private OutputStream output = new ByteArrayOutputStream();

	@Override
	protected void before() throws Throwable {
		this.originalOut = System.out;
		System.setOut(new PrintStream(output));
	}

	@Override
	protected void after() {
		System.setOut(originalOut);
		System.out.append(output.toString());
	};

	@Override
	public String toString() {
		return output.toString();
	}

	public Map<String, Object> findLineAsMapWith(String key, String expected) throws IOException {
		for (String line : output.toString().split("\n")) {
			Map<String, Object> map = JSON.std.mapFrom(line);
			if (expected.equals(getAsString(map, key))) {
				return map;
			}
		}
		return Collections.emptyMap();
	}

	private String getAsString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

}
