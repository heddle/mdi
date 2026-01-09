package edu.cnu.mdi.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.ui.colors.ColorLabel;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Dialog for entering a text label and selecting its font style/size and colors.
 * <p>
 * FlatLaf-compatible: this dialog no longer offers font-family selection. The
 * family is derived from the current LookAndFeel's base font (typically
 * {@link Fonts#defaultFont}) via the input font.
 * </p>
 *
 * Typical use: TextTool / text annotation creation.
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public class LabelDialog extends JDialog implements ListSelectionListener, ItemListener {

	private static final int MINWIDTH = 300;

    /**
     * Last selected font (size/style), used as the default in the null constructor.
     * <p>
     * Note: family is not user-selectable; it is preserved from this font.
     * </p>
     */
    private static Font lastFont = Fonts.defaultFont;

    // italics checkbox
    private JCheckBox italicCb;

    // bold checkbox
    private JCheckBox boldCb;

    // sample display text
    protected String displayText = "Sample Text 123";

    /**
     * Font size list.
     */
    private JList<String> fontSizeList;

    /**
     * The return font.
     */
    private Font returnFont;

    /**
     * The input font (determines the font family we keep).
     */
    private final Font inputFont;

    // text foreground color
    private ColorLabel _textForeground;

    // text background color
    private ColorLabel _textBackground;

    // possible font sizes
    private final String[] fontSizes = {
            " 8 ", " 10 ", " 11 ", " 12 ", " 14 ", " 16 ", " 18 ",
            " 20 ", " 24 ", " 30 ", " 36 ", " 40 ", " 48 ", " 60 ", " 72 "
    };

    /**
     * Display area.
     */
    private JLabel previewArea;

    /**
     * Text entry field.
     */
    private JTextField textField;

    /**
     * Result string (null if cancelled).
     */
    private String resultString = null;

    /**
     * Null constructor uses the last font.
     */
    public LabelDialog() {
        this(lastFont);
    }

    /**
     * Construct the dialog using an input font as the base.
     * The font family is taken from {@code inFont} and is not user-selectable.
     */
    public LabelDialog(Font inFont) {
        setTitle("String Parameters");
        setModal(true);

        // Defensive: ensure we always have a sane base font.
        inputFont = (inFont != null) ? inFont : Fonts.defaultFont;

        Container cp = getContentPane();

        JPanel top = new JPanel(new FlowLayout());
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Create and add the scroll list (size only)
        createScrollLists(top);

        JPanel subPanel = new JPanel(new BorderLayout(2, 2));
        subPanel.add(top, BorderLayout.CENTER);
        subPanel.add(createTextAndColorBox(), BorderLayout.NORTH);

        cp.add(subPanel, BorderLayout.NORTH);

        // Add the checkboxes for bold and italics
        top.add(createCheckBoxPanel());

        // Create the preview area and place it in the center
        previewArea = new JLabel(displayText, SwingConstants.CENTER);
        previewArea.setSize(200, 50);
        cp.add(previewArea, BorderLayout.CENTER);

        // Add the button panel in the south
        cp.add(createButtonPanel(), BorderLayout.SOUTH);

        // Initialize controls from input font
        initFromInputFont();

        previewFont(); // ensure view is up to date!
        pack();
        DialogUtils.centerDialog(this);
    }

	@Override
    public Dimension getMinimumSize() {
		Dimension d = super.getMinimumSize();
		if (d.width < MINWIDTH) {
			d.width = MINWIDTH;
		}
		return d;
	}
	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		if (d.width < MINWIDTH) {
			d.width = MINWIDTH;
		}
		return d;
	}
    /**
     * Initialize UI controls (size, bold, italic) based on the input font.
     */
    private void initFromInputFont() {
        if (boldCb != null) {
            boldCb.setSelected(inputFont.isBold());
        }
        if (italicCb != null) {
            italicCb.setSelected(inputFont.isItalic());
        }
        if (fontSizeList != null) {
            fontSizeList.setSelectedValue(" " + inputFont.getSize() + " ", true);
        }
    }

    /**
     * Create the panel that holds the checkboxes for bold and italic.
     */
    private JPanel createCheckBoxPanel() {
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 1));

        boldCb = new JCheckBox("Bold", false);
        italicCb = new JCheckBox("Italic", false);
        boldCb.addItemListener(this);
        italicCb.addItemListener(this);

        checkBoxPanel.add(boldCb);
        checkBoxPanel.add(italicCb);
        return checkBoxPanel;
    }

    /**
     * Create the scroll lists (size only) and add them to the given panel.
     */
    private void createScrollLists(JPanel panel) {
        fontSizeList = new JList<>(fontSizes);
        fontSizeList.setVisibleRowCount(8);
        JScrollPane scrollPane = new JScrollPane(fontSizeList);
        fontSizeList.addListSelectionListener(this);

        panel.add(scrollPane);
    }

    /**
     * Create the text entry field.
     */
    private void createTextEntryField() {
        textField = new JTextField();
        textField.setBorder(new CommonBorder("Enter the text"));
    }

    private Box createTextAndColorBox() {
        Box box = Box.createVerticalBox();

        createTextEntryField();
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);   // optional, keeps label/field consistent
        box.add(textField);
        box.add(Box.createVerticalStrut(8));

        JPanel subbox = new JPanel();
        subbox.setLayout(new BoxLayout(subbox, BoxLayout.Y_AXIS));
        subbox.setAlignmentX(Component.LEFT_ALIGNMENT);      // <-- key line

        _textForeground = new ColorLabel(null, Color.black, "Foreground", 200);
        _textForeground.setAlignmentX(Component.LEFT_ALIGNMENT); // optional
        subbox.add(_textForeground);
        subbox.add(Box.createVerticalStrut(8));

        _textBackground = new ColorLabel(null, null, "Background", 200);
        _textBackground.setAlignmentX(Component.LEFT_ALIGNMENT); // optional
        subbox.add(_textBackground);

        subbox.setBorder(new CommonBorder("Colors"));
        box.add(subbox);
        box.add(Box.createVerticalStrut(8));

        return box;
    }

    /**
     * Create the button panel.
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();

        // OK Button
        JButton okButton = new JButton(" OK ");
        panel.add(okButton);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previewFont();
                // Preserve the family from returnFont; that's the LAF-consistent family.
                lastFont = returnFont;
                resultString = textField.getText();
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
                returnFont = inputFont;
                resultString = null;
                dispose();
                setVisible(false);
            }
        });

        return panel;
    }

    /**
     * Build and apply the preview/return font based on the selected size and style.
     * <p>
     * Font family is preserved from {@link #inputFont} (FlatLaf-consistent).
     * </p>
     */
    protected void previewFont() {
        String resultSizeName = fontSizeList.getSelectedValue();
        if (resultSizeName == null) {
            resultSizeName = " " + inputFont.getSize() + " ";
        }
        int resultSize = Integer.parseInt(resultSizeName.trim());

        int style = Font.PLAIN;
        if (boldCb.isSelected()) {
            style |= Font.BOLD;
        }
        if (italicCb.isSelected()) {
            style |= Font.ITALIC;
        }

        // Preserve family from input font; this keeps typography consistent with FlatLaf.
        String family = inputFont.getFamily();
        returnFont = new Font(family, style, resultSize);

        previewArea.setFont(returnFont);
        pack();
    }

    /** Retrieve the selected font. */
    public Font getSelectedFont() {
        return returnFont;
    }

    /**
     * Gets the result string, which will be null if cancelled.
     */
    public String getText() {
        return resultString;
    }

    /**
     * List selection changed (size).
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        previewFont();
    }

    /**
     * Checkbox changed (bold/italic).
     */
    @Override
    public void itemStateChanged(ItemEvent ise) {
        previewFont();
    }

    /**
     * Get the text foreground.
     */
    public Color getTextForeground() {
        return _textForeground.getColor();
    }

    /**
     * Get the text background.
     */
    public Color getTextBackground() {
        return _textBackground.getColor();
    }

}
