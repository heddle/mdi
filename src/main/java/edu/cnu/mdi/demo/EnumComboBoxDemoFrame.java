package edu.cnu.mdi.demo;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.mapping.EProjection;

/**
 * Harness frame for testing EnumComboBox with the EProjection enum.
 */
@SuppressWarnings("serial")
public class EnumComboBoxDemoFrame extends JFrame {

    private final JLabel statusLabel;
    private final EnumComboBox<EProjection> combo;

    public EnumComboBoxDemoFrame() {
        super("EnumComboBox Demo");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        //
        // Create the EnumComboBox using your new API
        // Extra choice "Any Projection" mapped to null
        // and a custom label provider.
        //
        combo = new EnumComboBox<>(
                EProjection.class,
                "Any Projection",
                proj -> {
                    // Customize labels as you like
                    switch (proj) {
                        case MERCATOR:
                            return "Mercator";
                        case ORTHOGRAPHIC:
                            return "Orthographic";
                        case MOLLWEIDE:
                            return "Mollweide";
                        case LAMBERT_EQUAL_AREA:
                            return "Lambert Equal-Area";
                        default:
                            return proj.name();
                    }
                }
        );
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel center = new JPanel();
        center.add(combo);
        add(center, BorderLayout.CENTER);

        //
        // Status label at the top showing the current selection
        //
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        updateStatusLabel(); // initialize text
        add(statusLabel, BorderLayout.NORTH);

        //
        // Listener: use getSelectedEnum() (null == extra choice)
        //
        combo.addActionListener(e -> updateStatusLabel());

        //
        // Bottom panel with a few test buttons
        //
        JPanel bottom = new JPanel();

        JButton printSelected = new JButton("Print Selected");
        printSelected.addActionListener(e -> {
            EProjection proj = combo.getSelectedEnum();
            if (proj == null) {
                System.out.println("Currently selected: <Any Projection>");
            } else {
                System.out.println("Currently selected enum: " + proj);
            }
        });

        JButton selectMollweide = new JButton("Select Mollweide");
        selectMollweide.addActionListener(e -> combo.setSelectedItem(EProjection.MOLLWEIDE));

        JButton selectAny = new JButton("Select Any Projection");
        selectAny.addActionListener(e -> combo.setSelectedItem(null)); // null => extraChoiceLabel

        bottom.add(printSelected);
        bottom.add(selectMollweide);
        bottom.add(selectAny);

        add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Update the status label based on current selection.
     */
    private void updateStatusLabel() {
        EProjection proj = combo.getSelectedEnum();
        if (proj == null) {
            statusLabel.setText("Selected: Any Projection");
        } else {
            statusLabel.setText("Selected: " + proj);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EnumComboBoxDemoFrame::new);
    }
}
