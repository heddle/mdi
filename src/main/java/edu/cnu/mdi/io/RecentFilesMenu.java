package edu.cnu.mdi.io;

import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/** Binds a {@link RecentFiles} list to any application- or view-level menu. */
public final class RecentFilesMenu {

	private final RecentFiles recentFiles;
	private final Consumer<File> opener;
	private final String itemDescription;

	public RecentFilesMenu(RecentFiles recentFiles, Consumer<File> opener) {
		this(recentFiles, opener, "files");
	}

	public RecentFilesMenu(RecentFiles recentFiles, Consumer<File> opener,
			String itemDescription) {
		this.recentFiles = Objects.requireNonNull(recentFiles, "recentFiles");
		this.opener = Objects.requireNonNull(opener, "opener");
		this.itemDescription = itemDescription == null || itemDescription.isBlank()
				? "files" : itemDescription;
	}

	/** Rebuild the supplied menu from the current persistent list. */
	public void rebuild(JMenu menu) {
		Objects.requireNonNull(menu, "menu");
		menu.removeAll();
		List<File> files = recentFiles.getRecentFiles();
		if (files.isEmpty()) {
			JMenuItem empty = new JMenuItem("(none)");
			empty.setEnabled(false);
			menu.add(empty);
		} else {
			for (int index = 0; index < files.size(); index++) {
				File file = files.get(index);
				JMenuItem item = new JMenuItem((index + 1) + "  " + file.getName());
				item.setToolTipText(file.getAbsolutePath());
				item.addActionListener(event -> opener.accept(file));
				menu.add(item);
			}
		}
		menu.addSeparator();
		JMenuItem clear = new JMenuItem("Clear Recent");
		clear.setEnabled(!files.isEmpty());
		clear.addActionListener(event -> clearWithConfirmation(menu));
		menu.add(clear);
	}

	private void clearWithConfirmation(Component parent) {
		int answer = JOptionPane.showConfirmDialog(parent,
				"Clear the recent " + itemDescription + " list?", "Clear Recent",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (answer == JOptionPane.OK_OPTION) {
			recentFiles.clear();
			if (parent instanceof JMenu menu) rebuild(menu);
		}
	}
}
