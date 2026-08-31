package edu.cnu.mdi.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Regression coverage for {@link IpField#setText(String)}.
 *
 * <p>{@code setText} must set the text verbatim, like the standard
 * {@code JTextField} contract, rather than silently substituting a fixed
 * sentinel address when the given text fails validation. Validity is
 * tracked separately via {@link IpField#validText()}.</p>
 */
class IpFieldTest {

	@BeforeAll
	static void initFonts() {
		// IpField's constructor reads Fonts.mono, which is populated lazily.
		Fonts.refresh();
	}

	@Test
	void setTextStoresTheGivenTextVerbatimEvenWhenInvalid() {
		IpField field = new IpField();

		field.setText("not-an-address");

		assertEquals("not-an-address", field.getText(),
				"setText must not silently substitute a different address");
		assertFalse(field.validText(),
				"the invalid text should be reflected by validText(), not masked");
	}

	@Test
	void setTextStoresAValidAddressAndMarksItValid() {
		IpField field = new IpField();

		field.setText("129.57.167.227");

		assertEquals("129.57.167.227", field.getText());
		assertTrue(field.validText());
	}

	@Test
	void resetRestoresTheWildcardEveryAddress() {
		IpField field = new IpField("129.57.167.227");
		assertFalse(field.inResetState());

		field.reset();

		assertEquals(IpField.EVERYADDRESS, field.getText());
		assertTrue(field.inResetState());
	}
}
