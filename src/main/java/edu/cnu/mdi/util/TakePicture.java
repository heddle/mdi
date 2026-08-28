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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.dialog.DialogUtils;
import edu.cnu.mdi.graphics.GraphicsUtils;

/**
 * Utility for capturing a Swing {@link Component} as an image and either saving it as a
 * PNG file or copying it to the system clipboard.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>This class is deliberately agnostic about what kind of component it is given — it
 *       used to special-case {@code PlotPanel} internally, which silently broke plot capture
 *       (it dropped the title, legend, and axis labels down to just the inner canvas, undoing
 *       work the caller had already done correctly). That knowledge now lives where it
 *       belongs: on the view itself, via {@link edu.cnu.mdi.view.BaseView#getImageComponent()}.
 *       Callers should pass {@code view.getImageComponent()} rather than an arbitrary
 *       component plucked from the view's internals.</li>
 *   <li>{@link #takePicture(Component)} is the single entry point every existing caller
 *       already uses (the standard MDI toolbar's camera button, plot/histogram panels, and
 *       any custom "Save Image" action). It first asks the user to choose Save to File,
 *       Copy to Clipboard, or Cancel via {@link DialogUtils#yesNoDialog}, then performs
 *       whichever the user picked. This means every existing call site gets clipboard
 *       support automatically — no new menu item, toolbar button, or icon is needed
 *       anywhere.</li>
 *   <li>The file-save path writes <strong>PNG</strong> data. The chosen output filename is
 *       therefore forced to end with <code>.png</code> (case-insensitive) to avoid
 *       mismatches such as saving PNG data to <code>picture.jpg</code> or an extensionless
 *       file. Writing is performed via the configured PNG writer exposed by
 *       {@link Environment#getPngWriter()}. Access is synchronized and the writer's
 *       output is cleared after each capture. If the target file exists, the user is
 *       prompted to confirm overwrite.</li>
 *   <li>The clipboard path places the captured image on the system clipboard using the
 *       standard AWT {@code DataFlavor.imageFlavor} transfer, which is platform-independent:
 *       it works the same way on Windows, macOS, and Linux (with a running display server)
 *       because it goes through {@link Toolkit}'s clipboard, not any OS-specific API. It is
 *       also exposed directly as {@link #copyToClipboard(Component)}, for callers that want
 *       clipboard-only behavior without the Save/Copy/Cancel prompt.</li>
 * </ul>
 */
public final class TakePicture {

	private static final Logger LOGGER = Logger.getLogger(TakePicture.class.getName());

	private static final String OPTION_SAVE = "Save to File...";
	private static final String OPTION_CLIPBOARD = "Copy to Clipboard";
	private static final String OPTION_CANCEL = "Cancel";

	private TakePicture() {
	}

	/**
	 * Captures the provided component as an image, first asking the user whether to save it
	 * as a PNG file or copy it to the system clipboard.
	 *
	 * <p>This is the method every existing caller in the framework already uses (the
	 * standard toolbar camera button, plot and histogram panels, and MDI-3D's Image menu),
	 * so adding clipboard support here makes it available everywhere at once.</p>
	 *
	 * <p><strong>Save to File:</strong> the save dialog is parented to the window containing
	 * {@code canvas} (if any). The selected file is forced to end with <code>.png</code>
	 * (case-insensitive); if it already exists, the user is asked to confirm overwrite. If
	 * the user cancels the dialog, this method returns without writing a file.</p>
	 *
	 * <p><strong>Copy to Clipboard:</strong> delegates to {@link #copyToClipboard(Component)}.</p>
	 *
	 * <p><strong>Error handling:</strong> Exceptions during capture, writing, or clipboard
	 * access are recorded through the platform logger.</p>
	 *
	 * @param canvas the component to capture; if {@code null}, nothing is done
	 */
	public static void takePicture(Component canvas) {
		if (canvas == null) {
			return;
		}

		int choice = DialogUtils.yesNoDialog(
				"Save this image to a file, or copy it to the clipboard?",
				OPTION_SAVE, OPTION_CLIPBOARD, OPTION_CANCEL);

		if (choice != 0 && choice != 1) {
			return; // cancelled, or the dialog was closed without a selection (-1)
		}

		if (choice == 1) {
			copyToClipboard(canvas);
			return;
		}

		try {
			// Only proceed if we have a writer configured.
			if (Environment.getInstance().getPngWriter() == null) {
				return;
			}

			File file = getSavePngFile(canvas);
			if (file == null) {
				return; // user cancelled
			}

			BufferedImage bi = GraphicsUtils.getComponentImage(canvas);

			var writer = Environment.getInstance().getPngWriter();
			synchronized (writer) {
				try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
					writer.setOutput(ios);
					writer.write(bi);
				} finally {
					writer.setOutput(null);
				}
			}
		}
		catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to save component image.", e);
		}
	}

	/**
	 * Captures the provided component as an image and places it on the system clipboard,
	 * ready to paste into any application that accepts a pasted image (an email, a document,
	 * an image editor, a chat window, ...) — with no Save/Copy/Cancel prompt.
	 *
	 * <p>Most callers should use {@link #takePicture(Component)} instead, so the user gets a
	 * choice; call this directly only when clipboard-only behavior is specifically wanted
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
			BufferedImage bi = GraphicsUtils.getComponentImage(canvas);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new TransferableImage(bi), null);
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

		java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(canvas);
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
