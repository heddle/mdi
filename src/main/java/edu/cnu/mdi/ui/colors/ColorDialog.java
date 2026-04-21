package edu.cnu.mdi.ui.colors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

import edu.cnu.mdi.dialog.ButtonPanel;
import edu.cnu.mdi.dialog.DialogUtils;

/**
 * Modal dialog used to select a color.
 * <p>
 * This dialog is a thin wrapper around {@link ColorPanel}. It preserves the
 * original MDI usage pattern and API while delegating all color selection logic
 * to the panel.
 * </p>
 * <p>
 * In this refactored version, transparency is controlled only by the embedded
 * {@code JColorChooser}. The legacy custom transparency slider has been
 * removed from {@code ColorPanel}, eliminating conflicting alpha controls.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class ColorDialog extends JDialog {

	/** The embedded color selection panel. */
	protected ColorPanel colorPanel = null;

	/** The dialog close status. */
	protected int answer = -1;

	/**
	 * Creates a modal color-selection dialog.
	 *
	 * @param owner
	 *            the parent window; may be {@code null}
	 * @param initColor
	 *            the initial color; may be {@code null}
	 * @param allowNoColor
	 *            if {@code true}, the user may select "No Color"
	 * @param allowTransparency
	 *            if {@code true}, transparency selection is allowed through the
	 *            chooser's built-in alpha support
	 */
	public ColorDialog(Window owner, Color initColor, boolean allowNoColor, boolean allowTransparency) {
		super(owner, "Color Selection", ModalityType.DOCUMENT_MODAL);
		setup();
		addColorPanel(initColor, allowNoColor, allowTransparency);
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Displays a modal color dialog and returns the resulting color.
	 *
	 * @param owner
	 *            the parent window; may be {@code null}
	 * @param initColor
	 *            the initial color; may be {@code null}
	 * @param allowNoColor
	 *            if {@code true}, the user may select "No Color"
	 * @param allowTransparency
	 *            if {@code true}, transparency selection is allowed through the
	 *            chooser's built-in alpha support
	 * @return the selected color if the user presses OK; otherwise the original
	 *         {@code initColor}
	 */
	public static Color showDialog(Window owner, Color initColor, boolean allowNoColor, boolean allowTransparency) {
		ColorDialog dialog = new ColorDialog(owner, initColor, allowNoColor, allowTransparency);
		dialog.setVisible(true);

		if (dialog.getAnswer() == DialogUtils.OK_RESPONSE) {
			return dialog.getColor();
		}

		return initColor;
	}

	/**
	 * Creates and installs the central {@link ColorPanel}.
	 *
	 * @param initColor
	 *            the initial color; may be {@code null}
	 * @param allowNoColor
	 *            if {@code true}, the user may select "No Color"
	 * @param allowTransparency
	 *            if {@code true}, chooser alpha is enabled
	 */
	private void addColorPanel(Color initColor, boolean allowNoColor, boolean allowTransparency) {
		Container cp = getContentPane();

		colorPanel = new ColorPanel();
		cp.add(BorderLayout.CENTER, colorPanel);

		colorPanel.enableNoColor(allowNoColor);
		colorPanel.enableTransparency(allowTransparency);
		colorPanel.setColor(initColor);
		colorPanel.setNoColor(initColor == null);
	}

	/**
	 * Builds the static dialog layout, including the OK and Cancel buttons.
	 */
	protected void setup() {
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout(2, 2));

		ActionListener alist = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();

				if (ButtonPanel.OK_LABEL.equals(command)) {
					doClose(DialogUtils.OK_RESPONSE);
				}
				else if (ButtonPanel.CANCEL_LABEL.equals(command)) {
					doClose(DialogUtils.CANCEL_RESPONSE);
				}
			}
		};

		ButtonPanel bp = ButtonPanel.closeOutPanel(ButtonPanel.USE_OKCANCEL, alist, 6);
		cp.add(BorderLayout.SOUTH, bp);
	}

	/**
	 * Gets the selected color.
	 *
	 * @return the selected color, or {@code null} if "No Color" was selected
	 */
	public Color getColor() {
		if (colorPanel.isNoColorSelected()) {
			return null;
		}
		return colorPanel.getColor();
	}

	/**
	 * Closes the dialog with the given reason code.
	 *
	 * @param reason
	 *            either {@link DialogUtils#OK_RESPONSE} or
	 *            {@link DialogUtils#CANCEL_RESPONSE}
	 */
	protected void doClose(int reason) {
		answer = reason;
		setVisible(false);
	}

	/**
	 * Gets the answer code indicating how the dialog was dismissed.
	 *
	 * @return the dialog response code
	 */
	public int getAnswer() {
		return answer;
	}
}