package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.ui.colors.ColorLabel;
import edu.cnu.mdi.ui.fonts.Fonts;

public class TextEditPanel extends JPanel {

	//cached original text (immutable)
	public final String inText;

	//cached original style (immutable)
	public final Styled inStyle;

	//cached original font (immutable)
	public final Font inFont;

	//text area for editing
	private JTextArea textArea;

	//font selection panel
	private FontChoosePanel fontPanel;

	//color labels
	private ColorLabel _fill;
	private ColorLabel _text;
	private ColorLabel _stroke;

	//output text style
	private Styled outStyle;


	/**
	 * Create a panel for editing text with the given initial text and style.
	 *
	 * @param text  the initial text content
	 * @param style the initial style to apply
	 */

	public TextEditPanel(String text, Font font, IStyled style) {
		super();
		if (text == null) {
			text = "";
		}
		Objects.requireNonNull(style, "style");

		//cache inputs
		inText = new String(text);
		inFont = font; //fonts are immutable
		inStyle = new Styled(style);
		outStyle = new Styled(style);

		setLayout(new BorderLayout());
		createTextArea();
		createFontArea();
		createColorPanel();
	}

	//create the text area
	private void createTextArea() {
		textArea = new JTextArea(6, 20);
		textArea.setText(inText);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setFont(Fonts.plainFontDelta(0));
		add(textArea, BorderLayout.NORTH);
	}

	//create the font selection area
	private void createFontArea() {
		fontPanel = new FontChoosePanel("Font Selection", inFont);
		add(fontPanel, BorderLayout.CENTER);
	}

	//
	private JPanel createColorPanel() {
		Font font = Fonts.plainFontDelta(0);
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		_fill = new ColorLabel((src, col) -> {
		}, inStyle.getFillColor(), font, "Fill");


		_text = new ColorLabel((src, col) -> {
		}, inStyle.getTextColor(), font, "Text");

		_stroke = new ColorLabel((src, col) -> {
		}, inStyle.getLineColor(), font, "Border");

		panel.add(_fill);
		panel.add(_text);
		panel.add(_stroke);
		panel.setBorder(new CommonBorder("Text colors"));
		add(panel, BorderLayout.SOUTH);
		return panel;
	}

	/**
	 * Get the selected font.
	 *
	 * @return the selected font.
	 */
	public Font getSelectedFont() {
		return fontPanel.getSelectedFont();
	}

	/**
	 * Get the edited text.
	 *
	 * @return the edited text.
	 */
	public String getEditedText() {
		return textArea.getText();
	}

	/**
	 * Get the edited text style.
	 *
	 * @return the edited text style.
	 */
	public IStyled getSelectedStyle() {
		outStyle.setFillColor(_fill.getColor());
		outStyle.setTextColor(_text.getColor());
		outStyle.setLineColor(_stroke.getColor());
		return outStyle;
	}


	/**
	 * Returns the insets to use for this panel.
	 * <p>
	 * This implementation slightly increases the default insets, which can help
	 * provide a small visual margin around the content.
	 * </p>
	 *
	 * @return the insets for this panel
	 */
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2, def.right + 2);
	}

}
