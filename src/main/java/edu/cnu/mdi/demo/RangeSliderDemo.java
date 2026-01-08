package edu.cnu.mdi.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.cnu.mdi.component.RangeSlider;

/**
 * Demonstration frame for the {@link RangeSlider} component.
 * <p>
 * Shows:
 * <ul>
 *   <li>A normal slider with value label</li>
 *   <li>A compact slider with no label</li>
 *   <li>Continuous update vs. final update callbacks</li>
 * </ul>
 */
public class RangeSliderDemo extends JFrame {

    public RangeSliderDemo() {

        super("RangeSlider Demo");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(500, 300);

        // ================================
        // Title label
        // ================================
        JLabel header = new JLabel("RangeSlider Component Demo", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setForeground(new Color(20, 40, 120));
        add(header, BorderLayout.NORTH);

        // ================================
        // Main panel with two examples
        // ================================
        JPanel main = new JPanel(new GridLayout(2, 1, 10, 10));

        // --------------------------------
        // Example 1 – Normal slider
        // --------------------------------
        JPanel example1 = new JPanel(new BorderLayout());
        JLabel example1Label = new JLabel("Normal Slider (shows current value)", SwingConstants.CENTER);

        RangeSlider slider1 = new RangeSlider(0, 100, 30, 20, 5, true);

        // Continuous update callback
        slider1.setOnChange(val -> {
            System.out.println("Slider1 changing: " + val);
        });

        // Final change callback
        slider1.setOnFinalChange(val -> {
            System.out.println("Slider1 final value: " + val);
        });

        example1.add(example1Label, BorderLayout.NORTH);
        example1.add(slider1, BorderLayout.CENTER);

        // --------------------------------
        // Example 2 – Compact slider
        // --------------------------------
        JPanel example2 = new JPanel(new BorderLayout());
        JLabel example2Label = new JLabel("Compact Slider (no value label)", SwingConstants.CENTER);

        RangeSlider slider2 = new RangeSlider(0, 50, 10, false);

        slider2.setOnFinalChange(val -> {
            System.out.println("Slider2 final value: " + val);
        });

        example2.add(example2Label, BorderLayout.NORTH);
        example2.add(slider2, BorderLayout.CENTER);

        // Add both examples to the main panel
        main.add(example1);
        main.add(example2);

        add(main, BorderLayout.CENTER);
    }

    /**
     * Launch the demo frame.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RangeSliderDemo demo = new RangeSliderDemo();
            demo.setVisible(true);
        });
    }
}
