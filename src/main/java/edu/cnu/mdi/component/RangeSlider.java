package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A simple utility component that wraps a {@link JSlider} and an optional
 * label that displays the current slider value.
 *
 * <p>The {@code RangeSlider} supports two callback hooks:
 * <ul>
 *   <li>{@link #setOnChange(Consumer)} – invoked continuously as the user moves the slider</li>
 *   <li>{@link #setOnFinalChange(Consumer)} – invoked once after the user stops adjusting</li>
 * </ul>
 *
 * This makes the component suitable for interactive UI (continuous updates)
 * and more expensive model / rendering updates (on final change).
 */
@SuppressWarnings("serial")
public class RangeSlider extends JPanel {

    /** The underlying Swing slider. */
    private final JSlider slider;

    /** Optional label that shows the current value. */
    private final JLabel valueLabel;

    /** Called on every change (including while dragging). */
    private Consumer<Integer> onChange;

    /** Called once when sliding ends. */
    private Consumer<Integer> onFinalChange;

    /** Font for slider and label. */
    private static final Font FONT = Fonts.tweenFont;

    /** Whether the value label is displayed. */
    private final boolean showValue;

    // ============================================================
    // Convenience Constructors
    // ============================================================

    /**
     * Constructs a slider with no tick marks and a visible value label.
     */
    public RangeSlider(int min, int max, int defaultVal) {
        this(min, max, defaultVal, 0, 0, true);
    }

    /**
     * Constructs a slider with optional value label and no ticks.
     *
     * @param showValue whether the current numeric value is shown
     */
    public RangeSlider(int min, int max, int defaultVal, boolean showValue) {
        this(min, max, defaultVal, 0, 0, showValue);
    }

    /**
     * Constructs a slider with ticks and a visible value label.
     */
    public RangeSlider(int min, int max, int defaultVal,
                       int majorTick, int minorTick) {
        this(min, max, defaultVal, majorTick, minorTick, true);
    }

    /**
     * Full constructor with complete customization.
     */
    public RangeSlider(int min, int max, int defaultVal,
                       int majorTick, int minorTick,
                       boolean showValue) {

        super(new BorderLayout());
        this.showValue = showValue;

        slider = new JSlider(SwingConstants.HORIZONTAL, min, max, defaultVal);

        if (majorTick > 0 && majorTick < max) {
            slider.setMajorTickSpacing(majorTick);
        }
        if (minorTick > 0 && minorTick < majorTick) {
            slider.setMinorTickSpacing(minorTick);
        }

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setFont(FONT);
        slider.setFocusable(false);

        if (showValue) {
            valueLabel = new JLabel(String.valueOf(defaultVal), SwingConstants.CENTER);
            valueLabel.setFont(FONT);
        } else {
            valueLabel = null;
        }

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                handleSliderChange();
            }
        });

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(slider, BorderLayout.CENTER);

        if (showValue && valueLabel != null) {
            sliderPanel.add(valueLabel, BorderLayout.SOUTH);
        }

        add(sliderPanel, BorderLayout.CENTER);
    }

    // ============================================================
    // Internal Utility
    // ============================================================

    private void handleSliderChange() {
        int value = slider.getValue();

        if (showValue && valueLabel != null) {
            valueLabel.setText(String.valueOf(value));
        }

        if (onChange != null) {
            onChange.accept(value);
        }

        if (!slider.getValueIsAdjusting() && onFinalChange != null) {
            onFinalChange.accept(value);
        }
    }

    // ============================================================
    // API Methods
    // ============================================================

    /** Registers a callback for continuous change notifications. */
    public void setOnChange(Consumer<Integer> callback) {
        this.onChange = callback;
    }

    /** Registers a callback for final change notifications. */
    public void setOnFinalChange(Consumer<Integer> callback) {
        this.onFinalChange = callback;
    }

    /** Returns the current slider value. */
    public int getValue() {
        return slider.getValue();
    }

    /** Programmatically sets the slider value. */
    public void setValue(int value) {
        slider.setValue(value);
    }

    /** Returns the slider's minimum value. */
    public int getMinimum() {
        return slider.getMinimum();
    }

    /** Returns the slider's maximum value. */
    public int getMaximum() {
        return slider.getMaximum();
    }

    /**
     * Returns the underlying JSlider for users who need to add
     * custom label tables or further tweak the component.
     */
    public JSlider getSlider() {
        return slider;
    }
}
