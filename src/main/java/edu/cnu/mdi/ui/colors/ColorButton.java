package edu.cnu.mdi.ui.colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * A button that displays a color and allows the user to change it.
 */
@SuppressWarnings("serial")
public class ColorButton extends JButton {

    private Color color;

    private final String dialogTitle;
    private final boolean allowNoColor;
    private final boolean allowTransparency;

    /**
     * Creates a color button using the original default behavior.
     *
     * @param label   button label
     * @param initial initial color
     */
    public ColorButton(String label, Color initial) {
        this(
                label,
                "Color Selection",
                initial,
                true,
                true);
    }

    /**
     * Creates a configurable color button.
     *
     * @param label             button label
     * @param dialogTitle       color-dialog title
     * @param initial           initial color
     * @param allowNoColor      whether null/no color is permitted
     * @param allowTransparency whether alpha may be selected
     */
    public ColorButton(
            String label,
            String dialogTitle,
            Color initial,
            boolean allowNoColor,
            boolean allowTransparency) {

        super(label);

        this.dialogTitle =
                (dialogTitle == null)
                        ? "Color Selection"
                        : dialogTitle;

        this.allowNoColor = allowNoColor;
        this.allowTransparency = allowTransparency;

        color = initial;

        setFocusPainted(false);
        updateSwatch();
        addActionListener(e -> chooseColor());
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        updateSwatch();
    }

    private void updateSwatch() {
        if (color == null) {
            setIcon(null);
            setText("x");
            return;
        }

        setIcon(
                new ImageIcon(
                        makeSwatch(color, 28, 14)));
    }

    private void chooseColor() {
        java.awt.Window owner =
                SwingUtilities.getWindowAncestor(this);

        Color chosen =
                ColorDialog.showDialog(
                        owner,
                        dialogTitle,
                        color,
                        allowNoColor,
                        allowTransparency);

        setColor(chosen);
    }

    private static Image makeSwatch(
            Color color,
            int width,
            int height) {

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();

        int square = 4;

        for (int y = 0; y < height; y += square) {
            for (int x = 0; x < width; x += square) {
                boolean dark =
                        ((x / square) + (y / square)) % 2 == 0;

                g2.setColor(
                        dark
                                ? new Color(200, 200, 200)
                                : new Color(240, 240, 240));

                g2.fillRect(
                        x,
                        y,
                        square,
                        square);
            }
        }

        g2.setColor(color);
        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.black);
        g2.drawRect(
                0,
                0,
                width - 1,
                height - 1);

        g2.dispose();
        return image;
    }
}