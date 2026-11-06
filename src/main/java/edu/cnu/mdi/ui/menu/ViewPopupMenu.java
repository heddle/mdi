package edu.cnu.mdi.ui.menu;

import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.view.BaseView;

@SuppressWarnings("serial")
public class ViewPopupMenu extends JPopupMenu {

	/** Lazily created submenu containing view-specific zoom destinations. */
	private JMenu quickZoomMenu;

    /**
	 * The view's popup menu
	 * @param view
	 */
	public ViewPopupMenu(BaseView view) {
		super("Options");
		setLightWeightPopupEnabled(false);
        // base view owner

    }

	/**
	 * Puts "Undo" and "Redo" at the top of the menu, labelled with the edit they
	 * would act on (for example "Undo Move") and enabled only when there is
	 * something to undo or redo. The labels refresh each time the menu opens.
	 *
	 * @param container the container whose history the items drive; must not be
	 *                  {@code null}
	 */
	public void addUndoRedo(IContainer container) {
		Objects.requireNonNull(container, "container");

		JMenuItem undo = new JMenuItem("Undo");
		undo.addActionListener(e -> {
			if (container.undo()) {
				container.refresh();
			}
		});
		JMenuItem redo = new JMenuItem("Redo");
		redo.addActionListener(e -> {
			if (container.redo()) {
				container.refresh();
			}
		});

		insert(undo, 0);
		insert(redo, 1);
		insert(new JPopupMenu.Separator(), 2);

		addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				undo.setText(container.getUndoPresentationName());
				undo.setEnabled(container.canUndo());
				redo.setText(container.getRedoPresentationName());
				redo.setEnabled(container.canRedo());
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});
	}

	/**
	 * Adds a view-specific zoom destination to the {@code Quick Zoom} submenu.
	 *
	 * <p>The submenu and its preceding separator are created only when the first
	 * destination is registered. This keeps views without quick zooms unchanged
	 * and visually distinguishes navigation shortcuts from ordinary view
	 * commands.</p>
	 *
	 * @param item menu item that performs the zoom; must not be {@code null}
	 */
	public void addQuickZoom(JMenuItem item) {
		Objects.requireNonNull(item, "item");
		if (quickZoomMenu == null) {
			addSeparator();
			quickZoomMenu = new JMenu("Quick Zoom");
			add(quickZoomMenu);
		}
		quickZoomMenu.add(item);
	}

}
