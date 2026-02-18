package edu.cnu.mdi.splot.io;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * File filter for persisted splot plot-spec JSON files.
 * <p>
 * Suggested extensions:
 * <ul>
 *   <li>.plot.json (preferred)</li>
 *   <li>.splot.json (accepted)</li>
 * </ul>
 */
public final class PlotFileFilter extends FileFilter {

    /** Preferred extension (including leading dot). */
    public static final String EXT_PREFERRED = ".plot.json";

    /** Alternate accepted extension (including leading dot). */
    public static final String EXT_ALT = ".splot.json";

    @Override
    public boolean accept(File f) {
        if (f == null) {
			return false;
		}
        if (f.isDirectory()) {
			return true;
		}

        String name = f.getName().toLowerCase();
        return name.endsWith(EXT_PREFERRED) || name.endsWith(EXT_ALT);
    }

    @Override
    public String getDescription() {
        return "SPlot plot files (*" + EXT_PREFERRED + ", *" + EXT_ALT + ")";
    }

    /** Returns true if the file name matches a supported plot extension. */
    public static boolean isPlotFile(File f) {
        if (f == null) {
			return false;
		}
        String name = f.getName().toLowerCase();
        return name.endsWith(EXT_PREFERRED) || name.endsWith(EXT_ALT);
    }

    /**
     * If {@code f} already has a supported extension, returns it unchanged;
     * otherwise appends {@link #EXT_PREFERRED}.
     */
    public static File ensurePlotExtension(File f) {
        if (f == null) {
			return null;
		}
        if (isPlotFile(f)) {
			return f;
		}
        return new File(f.getParentFile(), f.getName() + EXT_PREFERRED);
    }
}
