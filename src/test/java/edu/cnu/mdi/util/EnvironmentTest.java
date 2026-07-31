package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Dimension;

import org.junit.jupiter.api.Test;

class EnvironmentTest {

	@Test
	void frameSizeIsDefensivelyCopied() {
		Environment environment = Environment.getInstance();
		Dimension supplied = new Dimension(800, 600);
		environment.setFrameSize(supplied);
		supplied.width = 1;
		Dimension returned = environment.getFrameSize();
		assertEquals(new Dimension(800, 600), returned);
		assertNotSame(returned, environment.getFrameSize());
	}

	@Test
	void parsesLegacyModernAndEarlyAccessJavaVersions() {
		assertEquals(8, Environment.parseJavaMajorVersion("1.8.0_402"));
		assertEquals(17, Environment.parseJavaMajorVersion("17.0.10"));
		assertEquals(22, Environment.parseJavaMajorVersion("22-ea"));
		assertEquals(-1, Environment.parseJavaMajorVersion("unknown"));
		assertEquals(-1, Environment.parseJavaMajorVersion(null));
	}

	@Test
	void screenHelpersAreSafeInHeadlessTests() {
		assertEquals(1.0, Environment.getDisplayScaleFactor());
		assertEquals(0, Environment.getGraphicsDevices().length);
	}
}
