package edu.cnu.mdi.sim.ui;

import javax.swing.Icon;

import edu.cnu.mdi.graphics.ImageManager;

/**
 * Standard simulation toolbar icons loaded from MDI resources:
 * {@code /edu/cnu/mdi/images/svg/<name>.svg}
 */
public final class StandardSimIcons implements IconSimulationControlPanel.IconProvider {

	private static final int SIZE = 18; // tweak (16/18/20) to taste

	private static final String DIR =
			ImageManager.MDI_RESOURCE_PATH + "/images/svg/";

	private static Icon icon(String baseName) {
		String path = DIR + baseName + ".svg";
		return ImageManager.getInstance().loadUiIcon(path, SIZE);
	}

	@Override public Icon start()  { return icon("start"); }
	@Override public Icon run()    { return icon("run"); }
	@Override public Icon pause()  { return icon("pause"); }
	@Override public Icon resume() { return icon("resume"); }
	@Override public Icon stop()   { return icon("stop"); }
	@Override public Icon cancel() { return icon("cancel"); }
}
