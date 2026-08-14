package edu.cnu.mdi.splot.io;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

import edu.cnu.mdi.io.RecentFiles;

/**
 * Plot-specific compatibility facade over MDI's general recent-files store.
 */
public final class RecentPlotFiles {

	private final RecentFiles delegate;

	public RecentPlotFiles(Preferences preferences, int maximumSize) {
		delegate = new RecentFiles(preferences, maximumSize, "recentPlotFile");
	}

	public RecentPlotFiles() {
		this(Preferences.userNodeForPackage(RecentPlotFiles.class), 10);
	}

	public int getMaxSize() {
		return delegate.getMaximumSize();
	}

	public void add(File file) {
		delegate.add(file);
	}

	public void remove(File file) {
		delegate.remove(file);
	}

	public void clear() {
		delegate.clear();
	}

	public List<File> getRecentFiles() {
		return delegate.getRecentFiles();
	}

	public List<String> getRecentPaths() {
		return delegate.getRecentPaths();
	}

	RecentFiles delegate() {
		return delegate;
	}
}
