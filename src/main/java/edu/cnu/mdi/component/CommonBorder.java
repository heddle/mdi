package edu.cnu.mdi.component;

/*
 * CommonBorder
 * Description: Border style stolen from Hv_Border
 */
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class CommonBorder extends TitledBorder {

	public Border etched = BorderFactory.createEtchedBorder();
	public CommonBorder() {
		super(BorderFactory.createEtchedBorder());
		setTitleColor(Color.blue);
		setTitleFont(titleFont());
	}

	private static Font titleFont() {
		Font font = Fonts.smallFont;
		if (font == null) {
			font = UIManager.getFont("Label.font");
		}
		if (font == null) {
			font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
		}
		return font.deriveFont(font.getSize2D() + 1.0f);
	}

	public CommonBorder(String title) {
		this();
		setTitle(title);
	}

	/**
	 * Create a common border with an empty border around it
	 *
	 * @param title the title
	 * @param size  the size of the empty border in pixels
	 * @return the compound border
	 */
	public static Border withEmptyBorder(String title, int size) {
		Border emptyBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);

		CommonBorder cborder = new CommonBorder(title);
		return BorderFactory.createCompoundBorder(emptyBorder, cborder);
	}
}
