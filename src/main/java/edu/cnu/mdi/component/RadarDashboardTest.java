package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Demonstrates animated drawn toggle buttons using a radar-style vector painter. */
public class RadarDashboardTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RadarDashboardTest::createAndShowGui);
    }

    private static void createAndShowGui() {
        try {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            } catch (Exception e) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }

            JFrame frame = new JFrame("MDI Radar Dashboard");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 24));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            ButtonPainter radarPainter = new RadarPainter();

            contentPanel.add(new LabeledButtonContainer(new DrawnToggleButton(true, radarPainter), "PATRIOT"));
            contentPanel.add(new LabeledButtonContainer(new DrawnToggleButton(true, radarPainter), "THAAD"));

            frame.add(contentPanel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (Exception ex) {
            edu.cnu.mdi.log.Log.getInstance().exception(ex);
        }
    }
}

/** Paints a rounded tactical radar-sector button. */
class RadarPainter implements ButtonPainter {

    private static final Dimension PREFERRED_SIZE = new Dimension(32, 32);

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREFERRED_SIZE);
    }

    @Override
    public void draw(Graphics2D g2, AbstractButton button, Rectangle bounds, long frameCount) {
        g2.setColor(button.isSelected() ? new Color(20, 45, 75)
                : button.getModel().isRollover() ? new Color(40, 50, 65) : new Color(25, 30, 40));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 20, 20);

        g2.setColor(button.isSelected() ? Color.CYAN : new Color(12, 25, 34));
        g2.setStroke(new java.awt.BasicStroke(2.0f));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 20, 20);

        int sectorWidth = (int) (bounds.width * 0.75);
        int sectorHeight = (int) (bounds.height * 0.65);
        int sectorX = bounds.x + (bounds.width - sectorWidth) / 2;
        int sectorY = bounds.y + (bounds.height - sectorHeight) / 2 + 5;

        double originX = sectorX + (sectorWidth / 2.0);
        double originY = sectorY + (sectorHeight / 1.1);

        double minAngle = 35.0;
        double maxAngle = 145.0;
        double angleExtent = maxAngle - minAngle;

        Arc2D sectorArc = new Arc2D.Double(sectorX, sectorY, sectorWidth, sectorHeight,
                minAngle, angleExtent, Arc2D.PIE);
        g2.setColor(new Color(43, 84, 114, 220));
        g2.fill(sectorArc);

        g2.setColor(new Color(12, 25, 34));
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        g2.draw(sectorArc);

        double angleDegrees = 90.0;
        double radiusScale = 0.45;

        if (button.isSelected()) {
            double wave = Math.sin(frameCount * 0.05);
            angleDegrees = minAngle + ((wave + 1.0) / 2.0) * angleExtent;
            radiusScale = 0.55;
            g2.setColor(new Color(185, 30, 115));
            g2.setStroke(new java.awt.BasicStroke(2.5f));
        } else {
            g2.setColor(new Color(100, 110, 120, 100));
            g2.setStroke(new java.awt.BasicStroke(1.5f));
        }

        double angleRadians = Math.toRadians(angleDegrees);
        double radius = Math.max(sectorWidth, sectorHeight) * radiusScale;
        double targetX = originX + radius * Math.cos(angleRadians);
        double targetY = originY - radius * Math.sin(angleRadians);
        g2.draw(new Line2D.Double(originX, originY, targetX, targetY));
    }
}
