package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.RectangleItem;
import edu.cnu.mdi.ui.menu.ViewPopupMenu;
import edu.cnu.mdi.undo.ItemEdits;

/**
 * The user-facing side of undo/redo on a {@link BaseView}: keyboard shortcuts
 * and the Undo/Redo entries in the view's right-click menu.
 */
class BaseViewUndoTest {

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	private static void onEdt(Runnable body) {
		onEdt(() -> {
			body.run();
			return null;
		});
	}

	private static BaseView newView(String title) {
		return onEdt(() -> new BaseView(new ViewPropertiesBuilder().title(title).visible(false).width(400)
				.height(300).worldSystem(new Rectangle2D.Double(0, 0, 10, 5)).build()));
	}

	/** Puts one recorded move on the view's container; returns the item and its original x. */
	private static RectangleItem recordAMove(BaseView view) {
		return onEdt(() -> {
			BaseContainer c = (BaseContainer) view.getIContainer();
			Layer layer = c.getDefaultLayer();
			RectangleItem item = new RectangleItem(layer, new Rectangle2D.Double(1, 1, 1, 1));
			item.setLocked(false);
			var list = java.util.List.of(item);
			var before = ItemEdits.capture(list);
			item.translateWorld(2, 0);
			c.recordEdit(ItemEdits.changedStates("Move", c, list, before));
			return item;
		});
	}

	private static String bindingFor(BaseView view, int keyCode, int requiredModifiers, int forbiddenModifiers) {
		InputMap im = view.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for (KeyStroke ks : im.allKeys()) {
			if (ks.getKeyCode() == keyCode && (ks.getModifiers() & requiredModifiers) == requiredModifiers
					&& (ks.getModifiers() & forbiddenModifiers) == 0) {
				Object name = im.get(ks);
				return name == null ? null : name.toString();
			}
		}
		return null;
	}

	private static void fire(BaseView view, String actionName) {
		Action action = view.getRootPane().getActionMap().get(actionName);
		assertNotNull(action, actionName);
		onEdt(() -> action.actionPerformed(new ActionEvent(view, ActionEvent.ACTION_PERFORMED, actionName)));
	}

	// ----------------------------------------------------------------- keys

	@Test
	void zIsBoundToUndoAndShiftZAndYToRedo() {
		BaseView view = newView("Key View");
		int shift = InputEvent.SHIFT_DOWN_MASK;

		assertEquals("mdi.undo", bindingFor(view, KeyEvent.VK_Z, 0, shift));
		assertEquals("mdi.redo", bindingFor(view, KeyEvent.VK_Z, shift, 0));
		assertEquals("mdi.redo", bindingFor(view, KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, 0));
	}

	@Test
	void theUndoShortcutUsesThePlatformCommandKey() {
		BaseView view = newView("Command Key View");
		InputMap im = view.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		int expected = java.awt.GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
				: java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

		assertEquals("mdi.undo", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_Z, expected)));
	}

	@Test
	void theActionsUndoAndRedoTheContainer() {
		BaseView view = newView("Action View");
		RectangleItem item = recordAMove(view);
		BaseContainer c = (BaseContainer) view.getIContainer();
		double moved = item.getFocus().x;

		fire(view, "mdi.undo");
		assertEquals(moved - 2, item.getFocus().x, 1e-9);
		assertFalse(c.canUndo());

		fire(view, "mdi.redo");
		assertEquals(moved, item.getFocus().x, 1e-9);
		assertTrue(c.canUndo());
	}

	@Test
	void theActionsDoNothingWhenThereIsNothingToUndoOrRedo() {
		BaseView view = newView("Empty History View");

		fire(view, "mdi.undo");
		fire(view, "mdi.redo");

		assertFalse(((BaseContainer) view.getIContainer()).canUndo());
	}

	@Test
	void aViewWithoutAContainerHasNoUndoBindings() {
		BaseView view = onEdt(() -> new BaseView(
				new ViewPropertiesBuilder().title("No Container").visible(false).useContainer(false).width(300)
						.height(200).build()));

		assertEquals(null, view.getRootPane().getActionMap().get("mdi.undo"));
	}

	// ----------------------------------------------------------- popup menu

	private static JMenuItem menuItem(ViewPopupMenu menu, int index) {
		return (JMenuItem) menu.getComponent(index);
	}

	private static void openPopup(ViewPopupMenu menu) {
		for (PopupMenuListener l : menu.getPopupMenuListeners()) {
			l.popupMenuWillBecomeVisible(new PopupMenuEvent(menu));
		}
	}

	@Test
	void thePopupStartsWithUndoAndRedoDisabledWhileThereIsNoHistory() {
		BaseView view = newView("Popup Empty View");
		ViewPopupMenu menu = view.getViewPopupMenu();

		openPopup(menu);

		assertEquals("Undo", menuItem(menu, 0).getText());
		assertFalse(menuItem(menu, 0).isEnabled());
		assertEquals("Redo", menuItem(menu, 1).getText());
		assertFalse(menuItem(menu, 1).isEnabled());
	}

	@Test
	void thePopupNamesTheEditAndEnablesTheRightItem() {
		BaseView view = newView("Popup Named View");
		recordAMove(view);
		ViewPopupMenu menu = view.getViewPopupMenu();

		openPopup(menu);
		assertEquals("Undo Move", menuItem(menu, 0).getText());
		assertTrue(menuItem(menu, 0).isEnabled());
		assertFalse(menuItem(menu, 1).isEnabled());

		onEdt(() -> menuItem(menu, 0).doClick());
		openPopup(menu);
		assertFalse(menuItem(menu, 0).isEnabled());
		assertEquals("Redo Move", menuItem(menu, 1).getText());
		assertTrue(menuItem(menu, 1).isEnabled());

		onEdt(() -> menuItem(menu, 1).doClick());
		openPopup(menu);
		assertTrue(menuItem(menu, 0).isEnabled());
		assertFalse(menuItem(menu, 1).isEnabled());
	}

	@Test
	void undoAndRedoComeBeforeTheViewsOtherPopupItems() {
		BaseView view = newView("Popup Order View");
		ViewPopupMenu menu = view.getViewPopupMenu();

		assertTrue(menu.getComponentCount() >= 4);
		assertEquals("Undo", menuItem(menu, 0).getText());
		assertEquals("Redo", menuItem(menu, 1).getText());
		assertTrue(menu.getComponent(2) instanceof javax.swing.JPopupMenu.Separator);
	}
}
