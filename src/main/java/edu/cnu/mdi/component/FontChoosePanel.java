package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A dialog for a label and a font.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class FontChoosePanel extends JPanel implements ListSelectionListener, ItemListener {

	/**
	 * The last selected family, used for default in null constructor.
	 */
//	private static Font lastFont = Fonts.plainFontDelta(2);

	// italics checkbox
	JCheckBox italicCb;

	// bold checkbox
	JCheckBox boldCb;

	// sample display text
	protected String displayText = "Sample Text 123";

	/**
	 * The font family name list
	 */
	private JList fontFamilyList;

	/**
	 * The return font.
	 */
	private Font returnFont;

	/**
	 * The input font
	 */
	private final Font inputFont;

	// Create the font size spinner
	private JSpinner fontSpinner;

	/**
	 * The display area. Use a JLabel as the AWT label doesn't always honor
	 * setFont() in a timely fashion :-)
	 */
	private JLabel previewArea;

	/**
	 * The result string.
	 */
	private String resultString = null;

	/**
	 * Construct a FontChooser -- Sets title and gets array of fonts on the system.
	 * Builds a GUI to let the user choose one font at one size.
	 *
	 * @param inFont the initial font to display.
	 */
	public FontChoosePanel(String title, Font inFont) {
		inputFont = inFont == null ? Fonts.plainFontDelta(2) : inFont;
		setLayout(new BorderLayout());

		// add the components
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());

		// create and add the font scroll list
		createScrollLists(top);
		// add the checkboxes for bold and italics
		top.add(createCheckBoxPanel(), BorderLayout.EAST);
		top.add(createSizeSpinner(inputFont.getSize()), BorderLayout.SOUTH);

		add(top, BorderLayout.WEST);

		// create the preview area and place it in the center
		previewArea = new JLabel(displayText, SwingConstants.LEFT);
		previewArea.setSize(200, 50);
		add(previewArea, BorderLayout.SOUTH);

		previewFont(); // ensure view is up to date!
		setBorder(new CommonBorder(title));
	}

	private JPanel createSizeSpinner(int initialSize) {
		JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel sizeLabel = new JLabel("Size: ");
		sizeLabel.setFont(Fonts.plainFontDelta(0));
		sizePanel.add(sizeLabel);
		initialSize = initialSize < 6 ? 6 : initialSize > 72 ? 72 : initialSize;
		SpinnerNumberModel model = new SpinnerNumberModel(initialSize, 6, 72, 1);
		fontSpinner = new JSpinner(model);
		fontSpinner.addChangeListener(e -> previewFont());
		sizePanel.add(fontSpinner);
		return sizePanel;
	}

	/**
	 * Create the panel that holds the checkboxes for bold and italic.
	 *
	 * @return the panel that holds the checkboxes for bold and italic.
	 */
	private JPanel createCheckBoxPanel() {
		JPanel checkBoxPanel = new JPanel();

		boldCb = new JCheckBox("Bold", false);
		italicCb = new JCheckBox("Italic", false);
		boldCb.addItemListener(this);
		italicCb.addItemListener(this);

		checkBoxPanel.setLayout(new GridLayout(0, 1));
		checkBoxPanel.add(boldCb);
		checkBoxPanel.add(italicCb);
		return checkBoxPanel;
	}

	/**
	 * Create the font scroll lists *family and size) and add them to the given
	 * panel.
	 *
	 * @param panel the panel to hold the lists.
	 */
	public void createScrollLists(JPanel panel) {
		// create the list of font families
		fontFamilyList = createFontList();
		JScrollPane scrollPane1 = new JScrollPane(fontFamilyList);
		fontFamilyList.setSelectedValue(inputFont.getFamily(), true);
		fontFamilyList.addListSelectionListener(this);

		panel.add(scrollPane1, BorderLayout.CENTER);
	}

	/**
	 * Create the font list.
	 *
	 * @return the font family selection list.
	 */
	private JList<?> createFontList() {
		String[] fontList = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		JList<?> list = new JList(fontList);
		list.setVisibleRowCount(8);
		return list;
	}

	/**
	 * Called from the action handlers to get the font info, build a font, and set
	 * it.
	 */
	protected void previewFont() {
		String resultName = (String) (fontFamilyList.getSelectedValue());
		int resultSize = ((Integer) fontSpinner.getValue());

		boolean isBold = boldCb.isSelected();
		boolean isItalic = italicCb.isSelected();

		int attrs = Font.PLAIN;
		if (isBold) {
			attrs = Font.BOLD;
		}
		if (isItalic) {
			attrs |= Font.ITALIC;
		}
		returnFont = new Font(resultName, attrs, resultSize);
		previewArea.setFont(returnFont);
	}

	/** Retrieve the selected font */
	public Font getSelectedFont() {
		return returnFont;
	}

	/**
	 * Gets the result string, which will be null if cancelled.
	 *
	 * @return the result string.
	 */
	public String getText() {
		return resultString;
	}

	/**
	 * One of the list's was selected. Redo the preview.
	 *
	 * @param lse the list selection event.
	 */
	@Override
	public void valueChanged(ListSelectionEvent lse) {
		previewFont();
	}

	/**
	 * One of check boxes was selected.
	 *
	 * @param ise the state changed event.
	 */
	@Override
	public void itemStateChanged(ItemEvent ise) {
		previewFont();
	}

}
