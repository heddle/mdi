package edu.cnu.mdi.dialog;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import edu.cnu.mdi.component.TextEditPanel;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.item.TextItem;
import edu.cnu.mdi.ui.fonts.Fonts;


@SuppressWarnings("serial")
public class TextEditDialog extends JDialog {

	// button labels
	protected static final String CLOSE  = "Close";
	protected static final String CANCEL = "Cancel";

    private TextEditPanel textEditPanel;

    private boolean cancelled;

 	/**
	 * Create a dialog for editing text with default title, empty initial text,
	 * default style, and default font.
	 */
	public TextEditDialog() {
		this("Edit Text", "", new Styled(), Fonts.plainFontDelta(2));

	}

	/**
	 * Create a dialog for editing text from the given TextItem.
	 *
	 * @param item the TextItem to edit
	 */
	public TextEditDialog(TextItem item) {
		this("Edit Text", item.getText(), item.getStyle(), item.getFont());
	}

	/**
	 * Create a dialog for editing text with the given initial text and style.
	 *
	 * @param title  the dialog title
	 * @param inText the initial text content
	 * @param inStyle the initial style to apply
	 * @param inFont  the initial font to apply
	 */
	public TextEditDialog(String title, String inText, IStyled inStyle,
			Font inFont) {
		setTitle(title == null ? "Edit Text" : title);
		setModal(true);

		textEditPanel = new TextEditPanel(inText, inFont, inStyle);
		add(textEditPanel, "Center");
		JPanel buttonPanel = createButtonPanel();
		add(buttonPanel, "South");
		pack();
	}

	/**
	 * Get the selected font.
	 * @return the selected font
	 */
	public Font getSelectedFont() {
		return textEditPanel.getSelectedFont();
	}

	/**
	 * Get the selected style.
	 * @return the selected style
	 */
	public IStyled getSelectedStyle() {
		return textEditPanel.getSelectedStyle();
	}

	/**
	 * Get the selected fill color.
	 * @return the selected fill color
	 */
	public Color getFillColor() {
		return getSelectedStyle().getFillColor();
	}

	/**
	 * Get the selected text color.
	 * @return the selected text color
	 */
	public Color getTextColor() {
		return getSelectedStyle().getTextColor();
	}

	/**
	 * Get the selected line color.
	 * @return the selected line color
	 */
	public Color getLineColor() {
		return getSelectedStyle().getLineColor();
	}

	/**
	 * Create the button panel.
	 */
	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

		// OK Button
		JButton okButton = new JButton(" OK ");
		panel.add(okButton);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelled = false;
				dispose();
				setVisible(false);
			}
		});

		// Cancel button
		JButton canButton = new JButton("Cancel");
		panel.add(canButton);
		canButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelled = true;
				dispose();
				setVisible(false);
			}
		});

		return panel;
	}

	/**
	 * Get the edited text.
	 *
	 * @return the edited text.
	 */
	public String getText() {
		String text = textEditPanel.getEditedText();
		return text == null ? "" : text;
	}

	/**
	 * Check if the dialog was cancelled.
	 *
	 * @return true if cancelled, false otherwise.
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Update the given TextItem with the edited text, font, and style.
	 *
	 * @param item the TextItem to update.
	 */
	public void updateTextItem(TextItem item) {
		if (item == null) {
			return;
		}

		item.setText(getText());
		item.setFont(getSelectedFont());
		item.setStyle(getSelectedStyle());
	}

}
