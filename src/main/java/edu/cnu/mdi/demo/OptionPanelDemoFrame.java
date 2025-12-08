package edu.cnu.mdi.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.OptionPanel;

@SuppressWarnings("serial")
public class OptionPanelDemoFrame extends JFrame {

    public OptionPanelDemoFrame() {
        super("OptionPanel Demo");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Sample options
        //  { label, enabled, selected }
        String[][] options = {
                {"Show Grid",     "true", "true"},
                {"Snap to Grid",  "true", "false"},
                {"Show Axes",     "true", "true"},
                {"Enable Zoom",   "true", "true"},
                {"Allow Drag",    "true", "false"},
                {"Dark Mode",     "true", "false"},
                {"Verbose Logs",  "true", "false"},
                {"Show Tooltips", "true", "true"}
        };

        // Build the panel
        OptionPanel panel = new OptionPanel(
                (src, label, selected) -> {
                    System.out.println("Changed: " + label + " -> " + selected);

                    // Demo of reacting to specific options:
                    if ("Dark Mode".equals(label)) {
                        System.out.println("Switching dark mode to: " + selected);
                    }
                },
                3,                               // number of columns
                new Font("SansSerif", Font.PLAIN, 14),
                Color.BLACK,                     // foreground
                new Color(240, 240, 240),        // background
                OptionPanel.MINSIZE,             // layout option
                options
        );

        add(panel, BorderLayout.CENTER);

        //
        // Bottom controls for testing enable/disable, querying, etc.
        //
        JPanel bottom = new JPanel();

        JButton testSelected = new JButton("Print Selected");
        testSelected.addActionListener(e -> {
            String[] sel = panel.getSelectedOptionLabels();
            System.out.println("Selected:");
            for (String s : sel) {
                System.out.println("  " + s);
            }
        });

        JButton toggleGrid = new JButton("Toggle \"Show Grid\"");
        toggleGrid.addActionListener(e -> {
            boolean current = panel.isOptionSelected("Show Grid");
            panel.setOptionSelected("Show Grid", !current);
        });

        JButton disableZoom = new JButton("Disable Zoom");
        disableZoom.addActionListener(e -> {
            panel.setOptionEnabled("Enable Zoom", false);
        });

        bottom.add(testSelected);
        bottom.add(toggleGrid);
        bottom.add(disableZoom);

        add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Demo entry point.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OptionPanelDemoFrame());
    }
}
