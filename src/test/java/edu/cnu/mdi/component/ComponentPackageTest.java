package edu.cnu.mdi.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class ComponentPackageTest {

    @Test
    void fontChooserPreservesInitialStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Font initial = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 17);
            Font selected = new FontChoosePanel("Font", initial).getSelectedFont();

            assertTrue(selected.isBold());
            assertTrue(selected.isItalic());
            assertEquals(17, selected.getSize());
        });
    }

    @Test
    void rangeSliderValidatesAndDispatchesValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertThrows(IllegalArgumentException.class,
                    () -> new RangeSlider(10, 0, 5));
            assertThrows(IllegalArgumentException.class,
                    () -> new RangeSlider(0, 10, 5, -1, 0));

            RangeSlider slider = new RangeSlider(0, 10, 5);
            int[] observed = { -1 };
            slider.setOnChange(value -> observed[0] = value);
            slider.setValue(8);
            assertEquals(8, observed[0]);
        });
    }

    @Test
    void enumComboSupportsExtraChoiceAndLabels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EnumComboBox<Choice> combo = new EnumComboBox<>(
                    Choice.class, null, "Any", value -> value.name().toLowerCase());
            assertEquals(null, combo.getSelectedEnum());
            combo.setSelectedItem(Choice.SECOND);
            assertEquals(Choice.SECOND, combo.getSelectedEnum());
        });
    }

    private enum Choice { FIRST, SECOND }
}
