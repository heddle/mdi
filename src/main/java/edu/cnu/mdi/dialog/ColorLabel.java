package edu.cnu.mdi.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A clickable color swatch + label that allows choosing a color (optionally with alpha).
 * <p>
 * - If the color is {@code null}, a red X is drawn to indicate "no color".
 * - Clicking opens a standard Swing {@link JColorChooser} dialog.
 * - An optional alpha slider enables transparency.
 * - A "None" button allows setting the color to {@code null}.
 * </p>
 *
 * @author heddle (refactor)
 */
@SuppressWarnings("serial")
public class ColorLabel extends JComponent {

    /** Current color; {@code null} means "no color". */
    private Color currentColor;

    /** Listener for color changes (may be null). */
    private IColorChangeListener colorChangeListener;

    /** Prompt text displayed to the right of swatch. */
    private String prompt;

    /** Preferred total width for the label (<= 0 means compute). */
    private int desiredWidth = -1;

    /** Size of the color box. */
    private int rectSize = 12;

    /** Cached preferred size. */
    private Dimension size;

    /** Optional font override for the prompt. */
    private Font fontOverride;

    /** Whether to allow selecting alpha (transparency). */
    private boolean allowAlpha = true;

    /** Whether to allow selecting "None" (null color). */
    private boolean allowNone = true;

    /**
     * Create a clickable color label.
     *
     * @param colorChangeListener listener for color changes (nullable)
     * @param initialColor initial color (nullable)
     * @param prompt prompt string
     */
    public ColorLabel(IColorChangeListener colorChangeListener, Color initialColor, String prompt) {
        this(colorChangeListener, initialColor, prompt, -1);
    }

    public ColorLabel(IColorChangeListener colorChangeListener, Color initialColor, Font font, String prompt) {
        this(colorChangeListener, initialColor, prompt, -1);
        setFont(font);
    }



    /**
     * Create a clickable color label.
     *
     * @param colorChangeListener listener for color changes (nullable)
     * @param initialColor initial color (nullable)
     * @param prompt prompt string
     * @param desiredWidth if positive, sets preferred total width
     */
    public ColorLabel(IColorChangeListener colorChangeListener, Color initialColor, String prompt, int desiredWidth) {
        this.colorChangeListener = colorChangeListener;
        this.currentColor = initialColor;
        this.prompt = prompt;
        this.desiredWidth = desiredWidth;

        if (this.desiredWidth > 10) {
            this.size = new Dimension(this.desiredWidth, 18);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showChooserDialog();
            }
        });
    }

    /**
     * Create an inert (non-clickable) color label. (Kept for compatibility.)
     *
     * @param color color (nullable)
     * @param font font override (nullable)
     * @param prompt prompt string
     */
    public ColorLabel(Color color, Font font, String prompt) {
        this.currentColor = color;
        this.fontOverride = font;
        this.prompt = prompt;

        Font f = (fontOverride != null) ? fontOverride : getFont();
        FontMetrics fm = getFontMetrics(f);
        int sw = fm.stringWidth(prompt);
        this.size = new Dimension(sw + rectSize + 10, 18);
    }

    /**
     * Configure whether alpha selection is allowed.
     */
    public ColorLabel setAllowAlpha(boolean allowAlpha) {
        this.allowAlpha = allowAlpha;
        return this;
    }

    /**
     * Configure whether "None" (null) is allowed.
     */
    public ColorLabel setAllowNone(boolean allowNone) {
        this.allowNone = allowNone;
        return this;
    }

    private void showChooserDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);

        // Start from currentColor, or default to opaque white for chooser UI.
        Color start = (currentColor != null) ? currentColor : Color.white;
        int startAlpha = (currentColor != null) ? currentColor.getAlpha() : 255;

        JColorChooser chooser = new JColorChooser(new Color(start.getRed(), start.getGreen(), start.getBlue()));

        // Alpha controls (optional)
        JSlider alphaSlider = new JSlider(0, 255, startAlpha);
        JLabel alphaLabel = new JLabel("Alpha: " + startAlpha, SwingConstants.LEFT);

        if (allowAlpha) {
            alphaSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    alphaLabel.setText("Alpha: " + alphaSlider.getValue());
                }
            });
        }

        // Dialog content
        JPanel content = new JPanel(new java.awt.BorderLayout(8, 8));
        content.add(chooser, java.awt.BorderLayout.CENTER);

        if (allowAlpha) {
            Box alphaBox = Box.createVerticalBox();
            alphaBox.setBorder(BorderFactory.createTitledBorder("Transparency"));
            alphaBox.add(alphaLabel);
            alphaBox.add(alphaSlider);
            content.add(alphaBox, java.awt.BorderLayout.SOUTH);
        }

        // Buttons
        final JDialog dialog = new JDialog(owner, "Choose Color");
        dialog.setModal(true);
        dialog.getContentPane().setLayout(new java.awt.BorderLayout());

        dialog.getContentPane().add(content, java.awt.BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        if (allowNone) {
            JButton noneBtn = new JButton("None");
            noneBtn.addActionListener(ae -> {
                setColor(null);
                dialog.dispose();
            });
            buttons.add(noneBtn);
        }

        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(ae -> {
            Color rgb = chooser.getColor();
            if (rgb == null) {
                // Very unlikely, but keep behavior safe.
                setColor(null);
            } else if (allowAlpha) {
                int a = alphaSlider.getValue();
                setColor(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), a));
            } else {
                setColor(rgb);
            }
            dialog.dispose();
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(ae -> dialog.dispose());

        buttons.add(okBtn);
        buttons.add(cancelBtn);

        dialog.getContentPane().add(buttons, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        DialogUtils.centerDialog(dialog);
        dialog.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        Font f = (fontOverride != null) ? fontOverride : getFont();
        g.setFont(f);

        FontMetrics fm = getFontMetrics(f);

        // swatch
        if (currentColor == null) {
            g.setColor(Color.red);
            g.drawLine(4, 4, rectSize, rectSize);
            g.drawLine(4, rectSize, rectSize, 4);
        } else {
            g.setColor(currentColor);
            g.fillRect(2, 2, rectSize, rectSize);
        }
        g.setColor(Color.black);
        g.drawRect(2, 2, rectSize, rectSize);

        // prompt
        g.drawString(prompt, rectSize + 6, fm.getHeight() - 4);
    }

    public void setColorListener(IColorChangeListener listener) {
		this.colorChangeListener = listener;
	}

    @Override
    public Dimension getPreferredSize() {
        if (size != null) {
            return size;
        }
        return super.getPreferredSize();
    }

    /** Return the current color (nullable). */
    public Color getColor() {
        return currentColor;
    }

    /**
     * Set a new color (nullable). Notifies listener and repaints.
     *
     * @param newColor the new color, or null for none
     */
    public void setColor(Color newColor) {
        // Keep background in sync for potential LAF painting / tooltips
        setBackground(newColor);

        this.currentColor = newColor;

        if (colorChangeListener != null) {
            colorChangeListener.colorChanged(this, newColor);
        }
        repaint();
    }
}
