package edu.cnu.mdi.graphics;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class SliderFactoryTest {

    @Test
    void rejectsTickSpacingThatWouldCauseNonterminatingLabelLoop() {
        JPanel parent = new JPanel();
        assertThrows(IllegalArgumentException.class, () ->
                SliderFactory.createLabeledSlider(parent, 0f, 1f, 0.5f,
                        0f, 0f, parent.getFont(), true, 2));
        assertThrows(IllegalArgumentException.class, () ->
                SliderFactory.createLabeledSlider(parent, 0f, 1f, 0.5f,
                        0.001f, 0f, parent.getFont(), true, 2));
    }

    @Test
    void rejectsInvalidIntegerRangeAndInitialValue() {
        JPanel parent = new JPanel();
        assertThrows(IllegalArgumentException.class, () ->
                SliderFactory.createLabeledSlider(parent, 10, 0, 5,
                        1, 0, parent.getFont(), false));
        assertThrows(IllegalArgumentException.class, () ->
                SliderFactory.createLabeledSlider(parent, 0, 10, 11,
                        1, 0, parent.getFont(), false));
    }
}
