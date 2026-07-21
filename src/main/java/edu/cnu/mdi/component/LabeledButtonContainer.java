package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A small compound component that places a centered label below a button.
 * <p>
 * The button is placed inside a centering panel. This keeps the label from
 * stretching the button horizontally, which is important for drawn/vector buttons
 * whose drawings assume a particular aspect ratio.
 * </p>
 */
@SuppressWarnings("serial")
public class LabeledButtonContainer extends JPanel {

    private static final int DEFAULT_VERTICAL_GAP = 4;

    private final AbstractButton button;
    private final JPanel buttonPanel;
    private final JLabel label;
    private final int verticalGap;

    /**
     * Creates a labeled button container using the default vertical gap.
     *
     * @param button the button to display
     * @param text the label text
     */
    public LabeledButtonContainer(AbstractButton button, String text) {
        this(button, text, DEFAULT_VERTICAL_GAP);
    }

    /**
     * Creates a labeled button container.
     *
     * @param button the button to display
     * @param text the label text
     * @param verticalGap the gap, in pixels, between the button and label
     */
    public LabeledButtonContainer(AbstractButton button, String text, int verticalGap) {
        super(new BorderLayout(0, Math.max(0, verticalGap)));

        this.button = button;
        this.verticalGap = Math.max(0, verticalGap);

        setOpaque(false);

        buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(button);

        label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(defaultLabelFont());

        add(buttonPanel, BorderLayout.CENTER);
        add(label, BorderLayout.SOUTH);
    }

    /**
     * Returns the contained button.
     *
     * @return the button
     */
    public AbstractButton getButton() {
        return button;
    }

    /**
     * Returns the label text.
     *
     * @return the label text
     */
    public String getLabelText() {
        return label.getText();
    }

    /**
     * Sets the label text.
     *
     * @param text the new label text
     */
    public void setLabelText(String text) {
        label.setText(text);
        revalidate();
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (label != null) {
            label.setFont(font);
        }
    }

    @Override
    public Font getFont() {
        return (label == null) ? super.getFont() : label.getFont();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (label != null) {
            label.setFont(defaultLabelFont());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension buttonSize = button.getPreferredSize();
        Dimension labelSize = label.getPreferredSize();

        int width = Math.max(buttonSize.width, labelSize.width);
        int height = buttonSize.height + verticalGap + labelSize.height;

        return new Dimension(width, height);
    }

    /**
     * Returns the default font for the label.
     * <p>
     * In a full MDI application, {@code Fonts.smallFont} is normally initialized
     * already. In small standalone tests, it may not be, so we refresh the font
     * cache once before falling back to a plain Swing font.
     * </p>
     *
     * @return the default label font
     */
    private static Font defaultLabelFont() {
        Font font = Fonts.smallFont;

        if (font == null) {
            Fonts.refresh();
            font = Fonts.smallFont;
        }

        return (font == null) ? new Font(Font.SANS_SERIF, Font.PLAIN, 10) : font;
    }}