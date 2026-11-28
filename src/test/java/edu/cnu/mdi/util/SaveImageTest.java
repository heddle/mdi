package edu.cnu.mdi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The PNG file and the dialog choices behind "Save Image". */
class SaveImageTest {

	@TempDir
	Path folder;

	/** The recorded pixels per meter, or null if the file records none. */
	private static String pixelsPerMeter(File file) throws IOException {
		try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
			ImageReader reader = ImageIO.getImageReaders(in).next();
			reader.setInput(in);
			IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(0)
					.getAsTree("javax_imageio_png_1.0");
			reader.dispose();
			var nodes = root.getElementsByTagName("pHYs");
			return nodes.getLength() == 0 ? null
					: ((IIOMetadataNode) nodes.item(0)).getAttribute("pixelsPerUnitXAxis");
		}
	}

	private static BufferedImage image(int size) {
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		java.util.Random random = new java.util.Random(1);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				image.setRGB(x, y, random.nextInt());
			}
		}
		return image;
	}

	@Test
	void recordsTheResolutionSoALargeImagePrintsAtItsOriginalSize() throws IOException {
		File file = folder.resolve("a.png").toFile();

		TakePicture.writePng(image(20), file, 288);

		assertEquals(Long.toString(Math.round(288 / 0.0254)), pixelsPerMeter(file));
		assertEquals(20, ImageIO.read(file).getWidth());
	}

	@Test
	void recordsNoResolutionAtScreenSize() throws IOException {
		File file = folder.resolve("a.png").toFile();

		TakePicture.writePng(image(20), file, 0);

		assertNull(pixelsPerMeter(file));
	}

	@Test
	void replacingALargerFileLeavesNothingOfIt() throws IOException {
		File file = folder.resolve("a.png").toFile();
		File fresh = folder.resolve("fresh.png").toFile();
		TakePicture.writePng(image(200), file, 0);

		TakePicture.writePng(image(10), file, 0);
		TakePicture.writePng(image(10), fresh, 0);

		assertEquals(Files.size(fresh.toPath()), Files.size(file.toPath()));
	}

	private static JPanel sized(int width, int height) {
		JPanel panel = new JPanel();
		panel.setSize(width, height);
		return panel;
	}

	private static List<JRadioButton> buttons(SaveImagePanel panel) {
		List<JRadioButton> found = new ArrayList<>();
		for (Component child : panel.getComponents()) {
			if (child instanceof JRadioButton button) {
				found.add(button);
			}
		}
		return found;
	}

	private static JRadioButton button(SaveImagePanel panel, String start) {
		return buttons(panel).stream().filter(b -> b.getText().startsWith(start)).findFirst().orElseThrow();
	}

	@Test
	void offersContentOrTheEntireViewAndShowsTheSizeOfEach() {
		JPanel content = sized(100, 50);
		JPanel view = sized(300, 200);
		SaveImagePanel panel = new SaveImagePanel(content, view, false, 2);

		assertTrue(panel.hasAreaChoice());
		assertSame(content, panel.area());
		assertEquals(2, panel.scale());
		assertEquals("4×  (400 × 200 pixels)", button(panel, "4").getText());

		button(panel, "Entire view").doClick();
		button(panel, "4").doClick();

		assertSame(view, panel.area());
		assertEquals(4, panel.scale());
		assertEquals("4×  (1200 × 800 pixels)", button(panel, "4").getText());
	}

	@Test
	void startsOnThePreferredArea() {
		JPanel view = sized(300, 200);

		assertSame(view, new SaveImagePanel(sized(100, 50), view, true, 1).area());
	}

	@Test
	void offersNoAreaChoiceWhenThereIsOnlyOneArea() {
		JPanel only = sized(100, 50);
		for (SaveImagePanel panel : new SaveImagePanel[] {
				new SaveImagePanel(only, null, true, 2),
				new SaveImagePanel(null, only, false, 2),
				new SaveImagePanel(sized(0, 0), only, false, 2) }) {
			assertFalse(panel.hasAreaChoice());
			assertSame(only, panel.area());
			assertEquals(3, buttons(panel).size(), "just the sizes");
		}
		assertNull(new SaveImagePanel(sized(0, 0), null, false, 2).area());
	}
}
