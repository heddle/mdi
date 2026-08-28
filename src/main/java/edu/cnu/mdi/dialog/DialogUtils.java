package edu.cnu.mdi.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.swing.WindowPlacement;

/** Utility methods and response constants used by MDI dialogs. */
public final class DialogUtils {

	/**
	 * Dialog "Reason" constant
	 */

	public static final int OK_RESPONSE = 0;

	/**
	 * Dialog "Reason" constant
	 */

	public static final int CANCEL_RESPONSE = 1;

	/**
	 * Current answer string
	 */

	/**
	 * Dialog "Reason" constant
	 */

	public static final int APPLY_RESPONSE = 2;

	/**
	 * Dialog "Reason" constant
	 */

	public static final int DONE_RESPONSE = 0;

	/**
	 * Dialog "Reason" constant
	 */

	public static final int YES_RESPONSE = 0;

	/**
	 * Dialog "Reason" constant
	 */

	public static final int NO_RESPONSE = 1;
	/**
	 * Private constructor to prevent instantiation.
	 */
	private DialogUtils() {
	}

	/**
	 * Center a dialog
	 *
	 * @param dialog the dialog to center
	 */

	public static void centerDialog(JDialog dialog) {
		WindowPlacement.centerComponent(dialog);
	}

	/**
	 * Place a component in the upper right
	 *
	 * @param component The Component to center.
	 */
	public static void upperRightComponent(Component component, int dh, int dv) {

		if (component == null) {
			return;
		}

		try {

			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] allScreens = env.getScreenDevices();
			GraphicsConfiguration gc = allScreens[0].getDefaultConfiguration();

			Rectangle bounds = gc.getBounds();
			Dimension componentSize = component.getSize();
			if (componentSize.height > bounds.height) {
				componentSize.height = bounds.height;
			}
			if (componentSize.width > bounds.width) {
				componentSize.width = bounds.width;
			}

			int x = bounds.x + bounds.width - componentSize.width - dh;
			int y = bounds.y + dv;

			component.setLocation(x, y);

		} catch (Exception e) {
			Log.getInstance().exception(e);
			component.setLocation(200, 200);
		}
	}

	/**
	 * Convenience routine for padding a string using the default font.
	 *
	 * @param inp  The string to be padded.
	 * @param tstr The test string-- try to return a string the same length
	 */

	public static String padString(Component c, String inp, String tstr) {

		String str;
		int oldgap;
		int newgap;

		if (inp == null) {
			str = new String("");
		} else {
			str = new String(inp);
		}

		FontMetrics fm = c.getFontMetrics(c.getFont());

		int sw = fm.stringWidth(tstr);
		oldgap = Math.abs(sw - fm.stringWidth(str));

		while (true) {
			String str2 = str + " ";
			newgap = Math.abs(sw - fm.stringWidth(str2));
			if (newgap < oldgap) {
				str = str2;
				oldgap = newgap;
			} else {
				break;
			}
		}

		return str;
	}

	/**
	 * Create a nice padded panel.
	 *
	 * @param hpad      the pixel pad on the left and right
	 * @param vpad      the pixel pad on the top and bottom
	 * @param component the main component placed in the center.
	 * @return the padded panel
	 */
	public static JPanel paddedPanel(int hpad, int vpad, Component component) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		if (hpad > 0) {
			panel.add(Box.createHorizontalStrut(hpad), BorderLayout.WEST);
			panel.add(Box.createHorizontalStrut(hpad), BorderLayout.EAST);
		}
		if (vpad > 0) {
			panel.add(Box.createVerticalStrut(vpad), BorderLayout.NORTH);
			panel.add(Box.createVerticalStrut(vpad), BorderLayout.SOUTH);
		}

		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	/**
	 * Create a dialog with a prompt and a set of options
	 *
	 * @param prompt the message displayed to the user
	 * @param options the available response labels
	 * @return the selected option index, or {@code -1} if the dialog was closed
	 */
	public static int yesNoDialog(String prompt, String... options) {

		JOptionPane pane = new JOptionPane(prompt);

		pane.setOptions(options);
		JDialog dialog = pane.createDialog(null, "Dialog");
		dialog.setVisible(true);
		Object obj = pane.getValue();
		for (int k = 0; k < options.length; k++) {
			if (options[k].equals(obj)) {
				return k;
			}
		}
		return -1;
	}
	
	/**
	 * Attempts to place a file chooser in details view.
	 *
	 * <p>
	 * This relies on a look-and-feel action name and therefore is not guaranteed to
	 * work with every Swing look and feel.
	 * </p>
	 *
	 * @param chooser the file chooser
	 */
	public static void requestDetailsView(JFileChooser chooser) {

		Action action = chooser.getActionMap().get("viewTypeDetails");

		if (action != null) {
			action.actionPerformed(new ActionEvent(chooser, ActionEvent.ACTION_PERFORMED, "viewTypeDetails"));
		}
	}

}
