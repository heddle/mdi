package edu.cnu.mdi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StartupInfoTest {
	@Test void builderCreatesOptionalMetadata() {
		StartupInfo info = StartupInfo.builder("Example").version("1.2").organization("CNU").copyright("2026").build();
		assertEquals("Example", info.applicationName()); assertEquals("1.2", info.version());
		assertEquals("CNU", info.organization()); assertEquals("2026", info.copyright()); assertNull(info.logo());
	}
}
