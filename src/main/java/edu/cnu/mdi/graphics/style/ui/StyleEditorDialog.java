package edu.cnu.mdi.graphics.style.ui;

import java.awt.*;
import javax.swing.*;

import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.graphics.style.*;

public final class StyleEditorDialog {

    private StyleEditorDialog() {}

    /** Returns a new Styled based on user edits, or null if cancelled. */
    public static Styled edit(Component parent, IStyled initial, boolean showSymbolControls) {
        Styled seed = (initial == null) ? new Styled() : new Styled(initial);

        AlphaColorButton fillBtn = new AlphaColorButton("Fill", seed.getFillColor());
        AlphaColorButton lineBtn = new AlphaColorButton("Line", seed.getLineColor());

        EnumComboBox<LineStyle> lineStyleBox =
                new EnumComboBox<>(LineStyle.class, null, LineStyle::getName);

        lineStyleBox.setSelectedItem(seed.getLineStyle());

        JSpinner lineWidthSpin = new JSpinner(new SpinnerNumberModel(seed.getLineWidth(), 1, 20, 1));

        EnumComboBox<SymbolType> symbolBox =
                new EnumComboBox<>(SymbolType.class, null, SymbolType::getName);

        symbolBox.setSelectedItem(seed.getSymbolType());

        JSpinner symbolSizeSpin = new JSpinner(new SpinnerNumberModel(seed.getSymbolSize(), 1, 60, 1));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        addRow(form, gc, row++, "Fill color:", fillBtn);
        addRow(form, gc, row++, "Line color:", lineBtn);
        addRow(form, gc, row++, "Line style:", lineStyleBox);
        addRow(form, gc, row++, "Line width:", lineWidthSpin);

        if (showSymbolControls) {
            addRow(form, gc, row++, "Symbol type:", symbolBox);
            addRow(form, gc, row++, "Symbol size:", symbolSizeSpin);
        }

        int result = JOptionPane.showConfirmDialog(parent, form, "Edit Style",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return null;

        Styled out = new Styled();
        out.setFillColor(fillBtn.getColor());
        out.setLineColor(lineBtn.getColor());
        out.setLineStyle(lineStyleBox.getSelectedEnum());
        out.setLineWidth((Integer) lineWidthSpin.getValue());

        if (showSymbolControls) {
            out.setSymbolType(symbolBox.getSelectedEnum());
            out.setSymbolSize((Integer) symbolSizeSpin.getValue());
        } else {
            out.setSymbolType(seed.getSymbolType());
            out.setSymbolSize(seed.getSymbolSize());
        }

        return out;
    }

    private static void addRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, gc);
    }
}
