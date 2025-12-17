package edu.cnu.mdi.splot.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.cnu.mdi.splot.model.FitResult;

/** Simple dialog that displays a fit summary. */
@SuppressWarnings("serial")
public class FitResultDialog extends JDialog {

    public FitResultDialog(JFrame owner, FitResult fit) {
        super(owner, "Fit Details", true);

        JTextArea area = new JTextArea();
        area.setEditable(false);

        List<String> lines = fit.summaryLines();
        area.setText(String.join("\n", lines));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 360));

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}
