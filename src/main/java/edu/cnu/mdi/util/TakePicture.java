package edu.cnu.mdi.util;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.splot.plot.PlotPanel;

/**
 * Utility for capturing a Swing {@link Component} and saving it as a PNG image.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>This class writes <strong>PNG</strong> data. The chosen output filename is therefore
 *       forced to end with <code>.png</code> (case-insensitive) to avoid mismatches such as
 *       saving PNG data to <code>picture.jpg</code> or an extensionless file.</li>
 *   <li>Writing is performed via the configured PNG writer exposed by
 *       {@link Environment#getPngWriter()}. If no writer is available, the method returns
 *       without showing an error dialog (legacy behavior).</li>
 *   <li>If the target file exists, the user is prompted to confirm overwrite.</li>
 * </ul>
 */
public class TakePicture {

	/**
	 * Captures the provided component as an image and prompts the user to save it as a PNG file.
	 *
	 * <p>The save dialog is parented to the window containing {@code canvas} (if any). If the user
	 * cancels the dialog, this method returns without writing a file.</p>
	 *
	 * <p><strong>Filename enforcement:</strong> The selected file is forced to end with
	 * <code>.png</code> (case-insensitive). If the user selects a name without an extension
	 * (or with a different extension), <code>.png</code> is appended.</p>
	 *
	 * <p><strong>Error handling:</strong> Exceptions during capture or write are printed to
	 * stderr via {@link Throwable#printStackTrace()} (legacy behavior).</p>
	 *
	 * @param canvas the component to capture; if {@code null}, nothing is done
	 */
	public static void takePicture(Component canvas) {
		if (canvas == null) {
			return;
		}
		
		if (canvas instanceof PlotPanel) {
			canvas = ((PlotPanel) canvas).getPlotCanvas();
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

			try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
				Environment.getInstance().getPngWriter().setOutput(ios);
				Environment.getInstance().getPngWriter().write(bi);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
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
	private static File enforcePngExtension(File selectedFile) {
		String name = selectedFile.getName();
		if (name.toLowerCase(Locale.ROOT).endsWith(".png")) {
			return selectedFile;
		}
		return new File(selectedFile.getParentFile(), name + ".png");
	}

}
