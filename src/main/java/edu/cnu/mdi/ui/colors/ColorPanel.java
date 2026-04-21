package edu.cnu.mdi.ui.colors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Panel used by {@link ColorDialog} to present color-selection controls.
 * <p>
 * This panel embeds a simplified {@link JColorChooser}, retaining only the RGB
 * chooser panel, along with an MDI-specific preview area that shows:
 * </p>
 * <ul>
 * <li>an optional "No Color" checkbox,</li>
 * <li>the newly selected color, and</li>
 * <li>the original color.</li>
 * </ul>
 * <p>
 * In this refactored version, transparency is controlled only through the
 * chooser's built-in alpha support. The legacy custom transparency slider has
 * been removed.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class ColorPanel extends JPanel implements ItemListener, ChangeListener {

	/** The minimum width of the panel. */
	protected static int minw = 440;

	/** The minimum height of the panel. */
	protected static int minh = 340;

	/**
	 * Checkbox used to select "No Color" when that option is enabled.
	 */
	protected JCheckBox nocolorcb;

	/**
	 * Label used to preview the original color.
	 */
	protected JLabel oldcolorlabel;

	/**
	 * Label used to preview the currently selected color.
	 */
	protected JLabel newcolorlabel;

	/**
	 * The custom preview panel shown beneath the chooser.
	 */
	protected JPanel previewPanel;

	/**
	 * The embedded Swing color chooser.
	 */
	protected JColorChooser colorChooser;

	/**
	 * Creates a new color panel.
	 * <p>
	 * The chooser is simplified so that only the RGB panel is retained. The
	 * preview area is custom and includes the old/new color samples and the
	 * optional "No Color" control.
	 * </p>
	 */
	public ColorPanel() {
		setLayout(new BorderLayout());

		colorChooser = new JColorChooser();

		// Keep only the RGB chooser panel.
		AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
		for (AbstractColorChooserPanel panel : panels) {
			if (!"RGB".equals(panel.getDisplayName())) {
				colorChooser.removeChooserPanel(panel);
			}
		}

		colorChooser.getSelectionModel().addChangeListener(this);

		add(BorderLayout.CENTER, colorChooser);

		previewPanel = createPreviewPanel();
		colorChooser.setPreviewPanel(previewPanel);
	}

	/**
	 * Creates the preview panel shown at the bottom of the chooser.
	 * <p>
	 * The layout is intentionally close to the original MDI appearance, except
	 * that the legacy transparency slider is omitted.
	 * </p>
	 *
	 * @return the preview panel
	 */
	protected JPanel createPreviewPanel() {
		JPanel pp = new JPanel() {

			/**
			 * Provides slightly expanded insets for a cleaner appearance.
			 *
			 * @return the panel insets
			 */
			@Override
			public Insets getInsets() {
				Insets def = super.getInsets();
				return new Insets(def.top + 2, def.left + 2, def.bottom + 2, def.right + 2);
			}

			/**
			 * Returns the minimum size of the preview area.
			 *
			 * @return the minimum size
			 */
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(minw - 20, 48);
			}

			/**
			 * Returns the preferred size of the preview area.
			 *
			 * @return the preferred size
			 */
			@Override
			public Dimension getPreferredSize() {
				return getMinimumSize();
			}
		};

		pp.setLayout(new BorderLayout(20, 4));

		nocolorcb = new JCheckBox("No Color");
		nocolorcb.addItemListener(this);
		pp.add(BorderLayout.WEST, nocolorcb);

		JPanel swatchPanel = new JPanel(new GridLayout(2, 2, 10, 2));

		newcolorlabel = new JLabel("   ");
		oldcolorlabel = new JLabel("   ");

		newcolorlabel.setOpaque(true);
		oldcolorlabel.setOpaque(true);

		swatchPanel.add(new JLabel("New Color:"));
		swatchPanel.add(newcolorlabel);
		swatchPanel.add(new JLabel("Old Color:"));
		swatchPanel.add(oldcolorlabel);

		pp.add(BorderLayout.EAST, swatchPanel);

		return pp;
	}

	/**
	 * Returns the minimum size of the panel.
	 *
	 * @return the minimum size
	 */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(minw, minh);
	}

	/**
	 * Returns the preferred size of the panel.
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	/**
	 * Returns slightly expanded outer insets for the panel.
	 *
	 * @return the panel insets
	 */
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 6, def.left + 6, def.bottom + 6, def.right + 6);
	}

	/**
	 * Responds to changes in the "No Color" checkbox.
	 *
	 * @param e
	 *            the item event
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == nocolorcb) {
			boolean noColor = (e.getStateChange() == ItemEvent.SELECTED);
			setNoColor(noColor);

			if (noColor) {
				newcolorlabel.setBackground(getBackground());
			}
			else {
				Color newColor = colorChooser.getColor();
				newcolorlabel.setBackground(newColor);
			}
		}
	}

	/**
	 * Sets whether "No Color" is selected.
	 *
	 * @param nocol
	 *            {@code true} to select "No Color"
	 */
	public void setNoColor(boolean nocol) {
		if (nocolorcb.isSelected() != nocol) {
			nocolorcb.setSelected(nocol);
		}

		if (nocol) {
			newcolorlabel.setBackground(getBackground());
		}
		else {
			Color newColor = colorChooser.getColor();
			if (newColor != null) {
				newcolorlabel.setBackground(newColor);
			}
		}
	}

	/**
	 * Checks whether "No Color" is selected.
	 *
	 * @return {@code true} if "No Color" is selected
	 */
	public boolean noColor() {
		return nocolorcb.isSelected();
	}

	/**
	 * Enables or disables the user's ability to choose "No Color".
	 *
	 * @param anc
	 *            {@code true} to enable the "No Color" option
	 */
	public void enableNoColor(boolean anc) {
		nocolorcb.setEnabled(anc);
		if (!anc) {
			setNoColor(false);
		}
	}

	/**
	 * Enables or disables transparency selection.
	 * <p>
	 * Since the custom transparency slider has been removed, this method now
	 * controls transparency by normalizing the chooser's color to fully opaque
	 * when transparency is not allowed.
	 * </p>
	 *
	 * @param anc
	 *            {@code true} if transparency selection is allowed
	 */
	public void enableTransparency(boolean anc) {
		if (!anc) {
			Color c = colorChooser.getColor();
			if (c != null && c.getAlpha() != 255) {
				colorChooser.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
			}
		}
		colorChooser.putClientProperty("mdi.allowTransparency", Boolean.valueOf(anc));
	}

	/**
	 * Sets the current color and updates the preview.
	 *
	 * @param c
	 *            the color to set; may be {@code null}
	 */
	public void setColor(Color c) {
		if (c == null) {
			// Preserve chooser usability when the logical value is null.
			colorChooser.setColor(Color.black);
			oldcolorlabel.setBackground(getBackground());
			newcolorlabel.setBackground(getBackground());
		}
		else {
			colorChooser.setColor(c);
			oldcolorlabel.setBackground(c);
			newcolorlabel.setBackground(c);
		}
	}

	/**
	 * Responds to chooser color changes.
	 * <p>
	 * Any explicit chooser action clears "No Color" and updates the new-color
	 * preview.
	 * </p>
	 *
	 * @param e
	 *            the change event
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		setNoColor(false);

		Color newColor = colorChooser.getColor();
		if (newColor == null) {
			return;
		}

		Boolean allowTransparency = (Boolean) colorChooser.getClientProperty("mdi.allowTransparency");
		if ((allowTransparency != null) && !allowTransparency.booleanValue() && (newColor.getAlpha() != 255)) {
			newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
			colorChooser.setColor(newColor);
			return;
		}

		newcolorlabel.setBackground(newColor);
	}

	/**
	 * Checks whether "No Color" is currently selected.
	 *
	 * @return {@code true} if "No Color" is selected
	 */
	public boolean isNoColorSelected() {
		return nocolorcb.isSelected();
	}

	/**
	 * Gets the currently selected color.
	 * <p>
	 * If "No Color" is selected, this method returns a fully transparent color
	 * for compatibility with earlier behavior. In normal dialog use,
	 * {@link ColorDialog#getColor()} converts that state to {@code null}.
	 * </p>
	 *
	 * @return the selected color, or a fully transparent color when "No Color"
	 *         is selected
	 */
	public Color getColor() {
		if (nocolorcb.isEnabled() && nocolorcb.isSelected()) {
			return new Color(0, 0, 0, 0);
		}

		Color c = colorChooser.getColor();
		if (c == null) {
			return Color.black;
		}

		Boolean allowTransparency = (Boolean) colorChooser.getClientProperty("mdi.allowTransparency");
		if ((allowTransparency != null) && !allowTransparency.booleanValue() && (c.getAlpha() != 255)) {
			return new Color(c.getRed(), c.getGreen(), c.getBlue());
		}

		return c;
	}
}