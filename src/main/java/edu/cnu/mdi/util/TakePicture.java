package edu.cnu.mdi.util;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class TakePicture {

	/**
	 * Take a picture, save as a png
	 */
	public static void takePicture(Component canvas) {
		try {

			// try making a png
			if (Environment.getInstance().getPngWriter() != null) {

				File file = getSavePngFile(canvas);
				if (file == null) {
					return;
				}

				// Buffered image object to be written
				BufferedImage bi;

				ImageOutputStream ios = ImageIO.createImageOutputStream(file);
				Environment.getInstance().getPngWriter().setOutput(ios);

				bi = GraphicsUtils.getComponentImage(canvas);

				Environment.getInstance().getPngWriter().write(bi);
				ios.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get file to save png
	private static File getSavePngFile(Component canvas) {
		FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG File", "png", "PNG");

		File selectedFile = null;
		JFileChooser chooser = new JFileChooser(Environment.getInstance().getHomeDirectory());
		chooser.setSelectedFile(null);
		chooser.setFileFilter(filter);
		java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(canvas);
		int returnVal = chooser.showSaveDialog(owner);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedFile = chooser.getSelectedFile();
			if (selectedFile != null) {

				if (selectedFile.exists()) {
					int answer = JOptionPane.showConfirmDialog(null,
							selectedFile.getAbsolutePath() + "  already exists. Do you want to overwrite it?",
							"Overwite Existing File?", JOptionPane.YES_NO_OPTION);

					if (answer != JFileChooser.APPROVE_OPTION) {
						selectedFile = null;
					}
				} // end file exists check
			}
		}

		return selectedFile;
	}

}
