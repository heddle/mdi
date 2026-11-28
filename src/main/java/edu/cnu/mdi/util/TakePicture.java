package edu.cnu.mdi.util;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * Saves a Swing {@link Component} as an image, to a PNG file or the system
 * clipboard.
 *
 * <p>{@link #takePicture(Component)} is the single entry point: the toolbar
 * camera button, plot and histogram panels, {@link BaseView#saveImage()} and
 * MDI-3D's Image menu all use it. It asks what to capture and at what size:</p>
 * <ul>
 *   <li><b>Area:</b> the component itself ("Content only") or, when it sits
 *       inside a {@link BaseView}, the whole internal frame with its title bar,
 *       menus, toolbar and side panels ("Entire view").</li>
 *   <li><b>Size:</b> 1, 2 or 4 times the size on screen. The component is
 *       painted again at that size (see
 *       {@link GraphicsUtils#getComponentImage(Component, int)}).</li>
 *   <li><b>Destination:</b> a PNG file or the clipboard.</li>
 * </ul>
 *
 * <p>A PNG saved at 2 or 4 times records 2 or 4 times the resolution programs
 * otherwise assume (72 dots per inch on macOS, 96 elsewhere), so that those
 * which honor it show the picture at the same size as one saved at 1 time. The
 * clipboard carries only pixels.</p>
 */
public final class TakePicture {

	private static final Logger LOGGER = Logger.getLogger(TakePicture.class.getName());

	private static final String OPTION_SAVE = "Save to File...";
	private static final String OPTION_CLIPBOARD = "Copy to Clipboard";
	private static final String OPTION_CANCEL = "Cancel";

	/** The last choices, offered again for the rest of the session. */
	private static boolean lastEntireView;
	private static int lastScale = 2;

	private TakePicture() {
	}

	/**
	 * Asks what to capture, at what size and where to put it, then does so.
	 *
	 * <p>If the user chooses a file, a file chooser follows; the name is given a
	 * {@code .png} extension and an existing file is replaced only after
	 * confirmation. Failures are logged and reported in a dialog.</p>
	 *
	 * <p>Call on the event dispatch thread.</p>
	 *
	 * @param canvas the component to capture; if {@code null} or of zero size and
	 *               outside any view, nothing is done
	 */
	public static void takePicture(Component canvas) {
		if (canvas == null) {
			return;
		}

		Component view = (canvas instanceof BaseView) ? canvas
				: SwingUtilities.getAncestorOfClass(BaseView.class, canvas);
		SaveImagePanel panel = new SaveImagePanel(canvas == view ? null : canvas, view,
				lastEntireView, lastScale);
		if (panel.area() == null) {
			return;
		}

		JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE);
		pane.setOptions(new Object[] { OPTION_SAVE, OPTION_CLIPBOARD, OPTION_CANCEL });
		pane.setInitialValue(OPTION_SAVE);
		JDialog dialog = pane.createDialog(SwingUtilities.getWindowAncestor(canvas), "Save Image");
		dialog.setVisible(true);
		Object choice = pane.getValue();
		dialog.dispose();

		if (!OPTION_SAVE.equals(choice) && !OPTION_CLIPBOARD.equals(choice)) {
			return; // cancelled, or the dialog was closed
		}

		Component area = panel.area();
		int scale = panel.scale();
		if (panel.hasAreaChoice()) {
			lastEntireView = (area == view);
		}
		lastScale = scale;

		File file = null;
		if (OPTION_SAVE.equals(choice)) {
			file = getSavePngFile(canvas);
			if (file == null) {
				return; // user cancelled
			}
		}

		try {
			BufferedImage image = GraphicsUtils.getComponentImage(area, scale);
			if (file == null) {
				copyToClipboard(image);
			} else {
				writePng(image, file, scale == 1 ? 0 : assumedDpi() * scale);
			}
		}
		catch (Exception | OutOfMemoryError e) {
			LOGGER.log(Level.WARNING, "Unable to save component image.", e);
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(canvas),
					"The image could not be saved. See the log for details.", "Save Image",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Writes {@code image} to {@code file} as a PNG, replacing any file there.
	 *
	 * @param dpi the resolution to record, or 0 to record none
	 */
	static void writePng(BufferedImage image, File file, int dpi) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
		try {
			IIOMetadata metadata = null;
			if (dpi > 0) {
				metadata = writer.getDefaultImageMetadata(
						ImageTypeSpecifier.createFromRenderedImage(image), null);
				String format = "javax_imageio_png_1.0";
				IIOMetadataNode size = new IIOMetadataNode("pHYs");
				String perMeter = Long.toString(Math.round(dpi / 0.0254));
				size.setAttribute("pixelsPerUnitXAxis", perMeter);
				size.setAttribute("pixelsPerUnitYAxis", perMeter);
				size.setAttribute("unitSpecifier", "meter");
				IIOMetadataNode root = new IIOMetadataNode(format);
				root.appendChild(size);
				metadata.mergeTree(format, root);
			}

			// An image stream does not truncate: a longer old file would keep its tail.
			Files.deleteIfExists(file.toPath());
			try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
				if (ios == null) {
					throw new IOException("Cannot write to " + file);
				}
				writer.setOutput(ios);
				writer.write(null, new IIOImage(image, null, metadata), null);
			}
		}
		finally {
			writer.dispose();
		}
	}

	// What programs assume for a picture that records no resolution, which is how
	// a picture at screen size is saved. Not the monitor's own figure.
	private static int assumedDpi() {
		return System.getProperty("os.name", "").startsWith("Mac") ? 72 : 96;
	}

	private static void copyToClipboard(BufferedImage image) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new TransferableImage(image), null);
	}

	/**
	 * Captures the provided component as an image and places it on the system clipboard,
	 * ready to paste into any application that accepts a pasted image (an email, a document,
	 * an image editor, a chat window, ...) — with no Save/Copy/Cancel prompt.
	 *
	 * <p>The image is the size of the component on screen. Most callers should use
	 * {@link #takePicture(Component)} instead, so the user gets a choice; call this directly only when clipboard-only behavior is specifically wanted
	 * (for example, a dedicated "Copy Image" keyboard shortcut).</p>
	 *
	 * <p>This uses the standard AWT image transfer ({@link DataFlavor#imageFlavor} via
	 * {@link Toolkit#getSystemClipboard()}), which every major desktop platform's AWT
	 * implementation supports identically — there is no per-OS branch here. No file is
	 * written and no dialog is shown; the method either succeeds silently or logs a warning.</p>
	 *
	 * <p><strong>Error handling:</strong> Exceptions during capture or clipboard access are
	 * recorded through the platform logger (for example {@code HeadlessException} in an
	 * environment with no display, or a clipboard owned by a security-restricted process
	 * refusing access).</p>
	 *
	 * @param canvas the component to capture; if {@code null}, nothing is done
	 */
	public static void copyToClipboard(Component canvas) {
		if (canvas == null) {
			return;
		}

		try {
			copyToClipboard(GraphicsUtils.getComponentImage(canvas));
		}
		catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to copy component image to clipboard.", e);
		}
	}

	/**
	 * Minimal {@link Transferable} wrapping a single {@link Image} as
	 * {@link DataFlavor#imageFlavor} — the standard, platform-independent way to place an
	 * image on the AWT clipboard.
	 */
	private static final class TransferableImage implements Transferable {

		private static final DataFlavor[] FLAVORS = { DataFlavor.imageFlavor };

		private final Image image;

		TransferableImage(Image image) {
			this.image = image;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return FLAVORS.clone();
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!DataFlavor.imageFlavor.equals(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return image;
		}
	}

	/**
	 * Prompts the user for a PNG file destination.
	 *
	 * <p>This method enforces a <code>.png</code> extension on the returned file and asks for
	 * overwrite confirmation if the final file already exists.</p>
	 *
	 * @param canvas a component used to locate the owning window for dialog parenting
	 * @return a file guaranteed to end with <code>.png</code>, or {@code null} if the user cancels
	 */
	private static File getSavePngFile(Component canvas) {
		FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG File", "png");

		JFileChooser chooser = new JFileChooser(Environment.getInstance().getHomeDirectory());
		chooser.setSelectedFile(null);
		chooser.setFileFilter(filter);

		java.awt.Window owner = SwingUtilities.getWindowAncestor(canvas);
		int returnVal = chooser.showSaveDialog(owner);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		File selectedFile = chooser.getSelectedFile();
		if (selectedFile == null) {
			return null;
		}

		// Enforce .png extension (the chooser filter does not enforce this by itself).
		selectedFile = enforcePngExtension(selectedFile);

		// Confirm overwrite if needed.
		if (selectedFile.exists()) {
			int answer = JOptionPane.showConfirmDialog(
					owner,
					selectedFile.getAbsolutePath() + " already exists. Do you want to overwrite it?",
					"Overwrite Existing File?",
					JOptionPane.YES_NO_OPTION);

			if (answer != JOptionPane.YES_OPTION) {
				return null;
			}
		}

		return selectedFile;
	}

	/**
	 * Ensures the returned {@link File} ends with the <code>.png</code> extension.
	 *
	 * <p>If the supplied file already ends with <code>.png</code> (case-insensitive), it is
	 * returned unchanged. Otherwise, a new {@link File} is returned with <code>.png</code>
	 * appended to the name in the same directory.</p>
	 *
	 * @param selectedFile the file chosen by the user (must not be {@code null})
	 * @return a file whose name ends with <code>.png</code>
	 */
	static File enforcePngExtension(File selectedFile) {
		if (selectedFile == null) {
			throw new IllegalArgumentException("selectedFile must not be null");
		}
		String name = selectedFile.getName();
		if (name.toLowerCase(Locale.ROOT).endsWith(".png")) {
			return selectedFile;
		}
		return new File(selectedFile.getParentFile(), name + ".png");
	}

}
