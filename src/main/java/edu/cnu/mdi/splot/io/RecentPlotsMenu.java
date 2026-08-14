package edu.cnu.mdi.splot.io;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JMenu;

import edu.cnu.mdi.io.RecentFilesMenu;

/** Plot-specific compatibility facade over MDI's general recent-files menu. */
public final class RecentPlotsMenu {

	private final RecentFilesMenu delegate;

	public RecentPlotsMenu(RecentPlotFiles recent, Consumer<File> opener) {
		Objects.requireNonNull(recent, "recent");
		delegate = new RecentFilesMenu(recent.delegate(), opener, "plot files");
	}

	public void rebuild(JMenu menu) {
		delegate.rebuild(menu);
	}
}
