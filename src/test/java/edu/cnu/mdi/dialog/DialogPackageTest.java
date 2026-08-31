package edu.cnu.mdi.dialog;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Arrays;

import javax.swing.JButton;
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

    @Test
    void closeOutPanelComposesOnlyTheRequestedButtonsInOrder() {
        ButtonPanel panel = ButtonPanel.closeOutPanel(
                ButtonPanel.USE_OK | ButtonPanel.USE_APPLY | ButtonPanel.USE_DELETE,
                null, 5);

        String[] actualLabels = Arrays.stream(panel.getComponents())
                .filter(JButton.class::isInstance)
                .map(c -> ((JButton) c).getText())
                .toArray(String[]::new);

        assertArrayEquals(
                new String[] { ButtonPanel.OK_LABEL, ButtonPanel.APPLY_LABEL, ButtonPanel.DELETE_LABEL },
                actualLabels,
                "closeOutPanel should include exactly the requested buttons, in USE_OK/CANCEL/APPLY/DELETE order");
    }

    @Test
    void closeOutPanelReturnsNullWhenNoButtonsAreRequested() {
        assertNull(ButtonPanel.closeOutPanel(0, null, 5));
    }

    @Test
    void padStringNeverShortensAndOnlyAppendsSpaces() {
        JLabel c = new JLabel();
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        String padded = DialogUtils.padString(c, "AB", "ABCDEFGH");

        assertTrue(padded.startsWith("AB"), "padding must not alter the original content");
        assertTrue(padded.length() >= 2, "padding must not shorten the string");
        assertEquals(padded, padded.stripTrailing() + " ".repeat(padded.length() - padded.stripTrailing().length()),
                "only trailing spaces may be appended");
    }

    @Test
    void padStringNarrowsTheWidthGapToTheTargetString() {
        JLabel c = new JLabel();
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        FontMetrics fm = c.getFontMetrics(c.getFont());

        String target = "ABCDEFGHIJ";
        String input = "AB";
        String padded = DialogUtils.padString(c, input, target);

        int originalGap = Math.abs(fm.stringWidth(target) - fm.stringWidth(input));
        int paddedGap = Math.abs(fm.stringWidth(target) - fm.stringWidth(padded));
        assertTrue(paddedGap <= originalGap,
				"padding should not leave the width gap worse than before padding");
    }

    @Test
    void padStringOfEqualWidthStringsAddsNoPadding() {
        JLabel c = new JLabel();
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        assertEquals("ABCDE", DialogUtils.padString(c, "ABCDE", "ABCDE"));
    }

    @Test
    void padStringTreatsNullInputAsEmpty() {
        JLabel c = new JLabel();
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        String padded = DialogUtils.padString(c, null, "AB");
        assertTrue(padded.isBlank());
    }
}
