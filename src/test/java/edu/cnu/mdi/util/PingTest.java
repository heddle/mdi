package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class PingTest {

	@Test
	void validatesDelayAndControlsTimerLifecycle() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new Ping(0));
		SwingUtilities.invokeAndWait(() -> {
			Ping ping = new Ping(1000, false);
			assertFalse(ping.isRunning());
			ping.setDelay(25);
			assertEquals(25, ping.getDelay());
			ping.start();
			assertTrue(ping.isRunning());
			ping.close();
			assertFalse(ping.isRunning());
		});
	}
}
