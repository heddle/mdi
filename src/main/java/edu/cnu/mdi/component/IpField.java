package edu.cnu.mdi.component;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import edu.cnu.mdi.ui.fonts.Fonts;

/** Text field for an IPv4 address whose octets may be wildcards. */
@SuppressWarnings("serial")
public class IpField extends JFormattedTextField implements DocumentListener {

	public static final String EVERYADDRESS = IpAddressSupport.ANY_ADDRESS;

	private boolean goodDocument = true;

	public IpField(String initialAddress) {
		this();
		setText(initialAddress);
	}

	public IpField() {
		setFont(Fonts.mono);
		FontMetrics metrics = getFontMetrics(getFont());
		Dimension preferredSize = getPreferredSize();
		preferredSize.width = metrics.stringWidth("255.255.255.255") + 6;
		setPreferredSize(preferredSize);
		setText(EVERYADDRESS);
		getDocument().addDocumentListener(this);
		setInputVerifier(new InputVerifier() {
			@Override
			public boolean shouldYieldFocus(JComponent input, JComponent target) {
				if (verify(input)) {
					return true;
				}
				Toolkit.getDefaultToolkit().beep();
				return false;
			}

			@Override
			public boolean verify(JComponent input) {
				return IpAddressSupport.validateSimpleWildcard(((JTextField) input).getText());
			}
		});
	}

	public void reset() {
		setText(EVERYADDRESS);
	}

	public boolean inResetState() {
		return EVERYADDRESS.equals(getText());
	}

	@Override
	public void setText(String text) {
		super.setText(IpAddressSupport.validateSimpleWildcard(text) ? text : "127.0.0.1");
	}

	public boolean validText() {
		return goodDocument;
	}

	private void checkDocument(DocumentEvent event) {
		try {
			String text = event.getDocument().getText(0, event.getDocument().getLength());
			goodDocument = IpAddressSupport.validateSimpleWildcard(text);
		} catch (BadLocationException exception) {
			goodDocument = false;
		}
	}

	@Override
	public void changedUpdate(DocumentEvent event) {
		checkDocument(event);
	}

	@Override
	public void insertUpdate(DocumentEvent event) {
		checkDocument(event);
	}

	@Override
	public void removeUpdate(DocumentEvent event) {
		checkDocument(event);
	}
}
