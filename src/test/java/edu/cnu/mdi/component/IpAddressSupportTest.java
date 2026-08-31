package edu.cnu.mdi.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpAddressSupportTest {

	@Test
	void validatesAddressesAndWildcardPatterns() {
		assertTrue(IpAddressSupport.validate("129.57.167.227"));
		assertFalse(IpAddressSupport.validate("256.57.167.227"));
		assertTrue(IpAddressSupport.validateSimpleWildcard("129.57.*.*"));
		assertTrue(IpAddressSupport.validateSimpleWildcard(IpAddressSupport.ANY_ADDRESS));
		assertFalse(IpAddressSupport.validateSimpleWildcard("129.57.*"));
	}

	@Test
	void createsMatchingPatternForWildcardAddress() {
		var pattern = IpAddressSupport.createPattern("129.57.*.*");
		assertTrue(pattern.matcher("129.57.167.227").matches());
		assertFalse(pattern.matcher("130.57.167.227").matches());
		assertNull(IpAddressSupport.createPattern(IpAddressSupport.ANY_ADDRESS));
		assertNull(IpAddressSupport.createPattern("not-an-address"));
	}
}
