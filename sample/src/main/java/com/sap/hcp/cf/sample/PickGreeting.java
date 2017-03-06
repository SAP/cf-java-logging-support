 package com.sap.hcp.cf.sample;

  import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickGreeting {
	private static Random random = new Random();
	
	private static final String[] GREETINGS = new String[] {
		"Hello Stranger",
		"Bonjour!",
		"Nice to see you...",
		"At your service",
		"What a beautiful day!",
		"\u0c90 \u0ca0"
	};
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PickGreeting.class);
	
	public static String pick() {
		String greeting = GREETINGS[random.nextInt(GREETINGS.length)];
		LOGGER.info("Picked greeting : {}", greeting);
		return greeting;
	}
}
