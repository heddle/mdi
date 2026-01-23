package edu.cnu.mdi.dialog;

import java.awt.Color;
import java.awt.Component;
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
import edu.cnu.mdi.ui.fonts.Fonts;


public class TextEditDialog extends JDialog {
	
	// button labels
	protected static final String CLOSE  = "Close";
	protected static final String CANCEL = "Cancel";
	
    private TextEditPanel textEditPanel;
    
    private boolean cancelled;
    
 	
	public TextEditDialog() {
		this("Edit Text", "", new Styled(), Fonts.plainFontDelta(2));
		
	} 

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
	
	public Font getSelectedFont() {
		return textEditPanel.getSelectedFont();
	}
	
	public IStyled getSelectedStyle() {
		return textEditPanel.getSelectedStyle();
	}
	
	public Color getFillColor() {
		return getSelectedStyle().getFillColor();
	}
	
	public Color getTextColor() {
		return getSelectedStyle().getTextColor();
	}
	
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

	
	public String getText() {
		String text = textEditPanel.getEditedText();
		return text == null ? "" : text;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}

}
