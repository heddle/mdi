package edu.cnu.mdi.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class DialogPackageTest {

    @Test
    void verticalPaddingDoesNotDependOnHorizontalPadding() {
        JPanel panel = DialogUtils.paddedPanel(0, 7, new JLabel("content"));
        BorderLayout layout = (BorderLayout) panel.getLayout();

        assertEquals(new Dimension(0, 7),
                layout.getLayoutComponent(BorderLayout.NORTH).getPreferredSize());
        assertEquals(new Dimension(0, 7),
                layout.getLayoutComponent(BorderLayout.SOUTH).getPreferredSize());
    }

    @Test
    void buttonPanelGracefullyHandlesNoLabelsAndInvalidTooltipIndex() {
        ButtonPanel empty = new ButtonPanel(null);
        assertDoesNotThrow(() -> empty.setEnabled(-1, false));
        assertDoesNotThrow(() -> empty.setToolTip(0, "tip"));

        ButtonPanel one = new ButtonPanel(new String[] { "One" });
        assertDoesNotThrow(() -> one.setToolTip(-1, "tip"));
        assertDoesNotThrow(() -> one.setToolTip(2, "tip"));
    }
}
