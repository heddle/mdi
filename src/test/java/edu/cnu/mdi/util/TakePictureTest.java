package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;

class TakePictureTest {

	@Test
	void enforcesPngExtensionCaseInsensitively() {
		File png = new File("image.PNG");
		assertSame(png, TakePicture.enforcePngExtension(png));
		assertEquals("image.jpg.png", TakePicture.enforcePngExtension(new File("image.jpg")).getName());
		assertThrows(IllegalArgumentException.class, () -> TakePicture.enforcePngExtension(null));
	}
}
