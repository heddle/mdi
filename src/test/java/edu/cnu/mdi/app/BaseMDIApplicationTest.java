package edu.cnu.mdi.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link BaseMDIApplication#getFrameworkVersion()}.
 *
 * <p>Only the version-lookup fallback chain is tested here: it is a pure
 * static method with no dependency on the singleton application instance.
 * Constructing an actual {@code BaseMDIApplication} is deliberately avoided
 * in this test suite, since only one may ever exist per JVM and doing so
 * would leak global state into every other test that runs afterward in the
 * same JVM.</p>
 */
class BaseMDIApplicationTest {

	@Test
	void getFrameworkVersionNeverReturnsNullOrBlank() {
		String version = BaseMDIApplication.getFrameworkVersion();
		assertNotNull(version);
		assertFalse(version.isBlank());
	}

	@Test
	void getFrameworkVersionIsStableAcrossCalls() {
		assertNotNull(BaseMDIApplication.getFrameworkVersion());
		String first = BaseMDIApplication.getFrameworkVersion();
		String second = BaseMDIApplication.getFrameworkVersion();
		org.junit.jupiter.api.Assertions.assertEquals(first, second);
	}
}
