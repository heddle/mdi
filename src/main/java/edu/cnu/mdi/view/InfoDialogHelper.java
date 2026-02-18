package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class InfoDialogHelper {

    public static JDialog showInfoDialog(Component parent, AbstractViewInfo info) {
        // Create the dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Information",
        		Dialog.ModalityType.MODELESS);

        dialog.setAlwaysOnTop(false);
        dialog.setAutoRequestFocus(false);
        dialog.setFocusableWindowState(false);
        dialog.setType(java.awt.Window.Type.UTILITY);
        dialog.setLayout(new BorderLayout());

        // Create the HTML viewer
        JEditorPane infoPane = new JEditorPane();
        infoPane.setContentType("text/html");
        infoPane.setText(info.getAsHTML());
        infoPane.setEditable(false);
        infoPane.setCaretPosition(0); // Ensure scroll is at top

        // Add padding/margins
        infoPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add to scroll pane
        JScrollPane scrollPane = new JScrollPane(infoPane);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Add Close button at the bottom
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Sizing and positioning
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return dialog;
    }
}