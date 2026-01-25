package edu.cnu.mdi.splot.io;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * Helper to populate a "Recent Plots" menu.
 * <p>
 * You provide an {@code opener} callback that receives the chosen File.
 */
public final class RecentPlotsMenu {

    private final RecentPlotFiles recent;
    private final Consumer<File> opener;

    /**
     * @param recent MRU manager
     * @param opener callback invoked when user selects a recent file (should open it)
     */
    public RecentPlotsMenu(RecentPlotFiles recent, Consumer<File> opener) {
        this.recent = Objects.requireNonNull(recent, "recent");
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    /**
     * Rebuilds menu items for the current recent file list.
     * Adds a "Clear" item at bottom.
     */
    public void rebuild(JMenu menu) {
        Objects.requireNonNull(menu, "menu");
        menu.removeAll();

        List<File> files = recent.getRecentFiles();
        if (files.isEmpty()) {
            JMenuItem none = new JMenuItem("(none)");
            none.setEnabled(false);
            menu.add(none);
        } else {
            int idx = 1;
            for (File f : files) {
                String label = idx + "  " + f.getName();
                JMenuItem item = new JMenuItem(label);
                item.setToolTipText(f.getAbsolutePath());
                item.addActionListener(e -> opener.accept(f));
                menu.add(item);
                idx++;
            }
        }

        menu.addSeparator();

        JMenuItem clear = new JMenuItem("Clear Recent");
        clear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                    menu,
                    "Clear the recent plot file list?",
                    "Clear Recent",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (ok == JOptionPane.OK_OPTION) {
                recent.clear();
                rebuild(menu);
            }
        });
        menu.add(clear);
    }
}
