package edu.cnu.mdi.graphics;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

public final class SliderFactory {

	private SliderFactory() {
		throw new AssertionError("No SliderFactory instances");
	}

	/**
	 * Creates a self-contained panel containing a slider and an optional value
	 * label. This avoids hardcoding sizes and preserves the parent's layout.
	 * @param parent The container to which the slider panel will be added.
	 * @param min The minimum value of the slider.
	 * @param max The maximum value of the slider.
	 * @param initial The initial value of the slider.
	 * @param majorTick The major tick spacing for the slider.
	 * @param minorTick The minor tick spacing for the slider, or 0 for none.
	 * @param font The font to use for the slider and label.
	 * @throws NullPointerException     if {@code parent} is {@code null}
	 * @throws IllegalArgumentException if {@code min >= max}, if
	 *                                  {@code initial} is outside
	 *                                  {@code [min, max]}, if
	 *                                  {@code majorTick <= 0}, or if
	 *                                  {@code minorTick < 0}
	 */
	public static JSlider createLabeledSlider(Container parent, int min, int max,
			int initial, int majorTick, int minorTick, Font font,
			boolean showValue) {
		validateArguments(parent, min, max, initial, majorTick, minorTick);

        // Create a container for this specific slider group
        JPanel wrapper = new JPanel(new BorderLayout(5, 5));
        JSlider slider = new JSlider(min, max, initial);

        // Configure Slider
        slider.setMajorTickSpacing(majorTick);
        if (minorTick > 0) {
			slider.setMinorTickSpacing(minorTick);
		}
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setFont(font);

        // Add to wrapper instead of the main panel directly
        wrapper.add(slider, BorderLayout.CENTER);

        if (showValue) {
            JLabel valueLabel = new JLabel(formatValue(slider.getValue()));
            valueLabel.setFont(font);
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

            wrapper.add(valueLabel, BorderLayout.NORTH);

            // Add listener for real-time updates
            slider.addChangeListener(e -> valueLabel.setText(formatValue(slider.getValue())));
        }

        // Add the wrapped component to the provided parent
        parent.add(wrapper);

        return slider;
    }

	/**
	 * Creates a self-contained panel containing a slider and an optional value
	 * label. This avoids hardcoding sizes and preserves the parent's layout.
	 * This method is for float values.
	 * @param parent The container to which the slider panel will be added.
	 * @param min The minimum value of the slider.
	 * @param max The maximum value of the slider.
	 * @param initial The initial value of the slider.
	 * @param majorTick The major tick spacing for the slider.
	 * @param minorTick The minor tick spacing for the slider, or 0 for none.
	 * @param font The font to use for the slider and label.
	 * @param numDec The number of decimal places to display.
	 * @throws NullPointerException     if {@code parent} is {@code null}
	 * @throws IllegalArgumentException if any of {@code min}, {@code max},
	 *                                  {@code initial}, {@code majorTick}, or
	 *                                  {@code minorTick} is not finite; if
	 *                                  {@code min >= max}, {@code initial} is
	 *                                  outside {@code [min, max]},
	 *                                  {@code majorTick <= 0}, or
	 *                                  {@code minorTick < 0}; if
	 *                                  {@code numDec} is outside
	 *                                  {@code [0, 6]}; or if {@code numDec} is
	 *                                  high enough relative to the range that
	 *                                  scaling to an integer-backed
	 *                                  {@code JSlider} would collapse the
	 *                                  range or tick spacing to zero
	 */
	  public static JSlider createLabeledSlider(Container parent, float min, float max,
	            float initial, float majorTick, float minorTick, Font font,
	            boolean showValue, int numDec) {
	        Objects.requireNonNull(parent, "parent");
	        if (!Float.isFinite(min) || !Float.isFinite(max) || !Float.isFinite(initial)
	                || !Float.isFinite(majorTick) || !Float.isFinite(minorTick)) {
	            throw new IllegalArgumentException("Slider values and tick spacing must be finite");
	        }
	        if (min >= max || initial < min || initial > max || majorTick <= 0 || minorTick < 0) {
	            throw new IllegalArgumentException("Invalid slider range, initial value, or tick spacing");
	        }
	        if (numDec < 0 || numDec > 6) {
	            throw new IllegalArgumentException("numDec must be between 0 and 6");
	        }

	        // 1. Calculate scaling factor (e.g., 2 decimal places = 100)
	        final float scale = (float) Math.pow(10, numDec);

	        // 2. Convert float bounds to integer bounds for the JSlider
	        int iMin = Math.round(min * scale);
	        int iMax = Math.round(max * scale);
	        int iInitial = Math.round(initial * scale);
	        int iTick = Math.round(majorTick * scale);
	        int iMinorTick = Math.round(minorTick * scale);
	        if (iMin >= iMax || iTick <= 0 || (minorTick > 0 && iMinorTick <= 0)) {
	            throw new IllegalArgumentException(
	                    "Precision is too low to represent the slider range or tick spacing");
	        }

	        JPanel wrapper = new JPanel(new BorderLayout(5, 5));
	        JSlider slider = new JSlider(iMin, iMax, iInitial);

	        // 3. Setup Ticks and Labels
	        slider.setMajorTickSpacing(iTick);
	        if (iMinorTick > 0) {
	            slider.setMinorTickSpacing(iMinorTick);
	        }
	        slider.setPaintTicks(true);
	        slider.setPaintLabels(true);
	        slider.setFont(font);

	        // 4. Custom Label Table (so ticks show "0.5" instead of "50")
	        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
	        for (int i = iMin; i <= iMax; i += iTick) {
	            JLabel label = new JLabel(formatFloatValue(i / scale, numDec));
	            label.setFont(font);
	            labelTable.put(i, label);
	        }
	        slider.setLabelTable(labelTable);

	        wrapper.add(slider, BorderLayout.CENTER);

	        if (showValue) {
	            // Display the current value as a float
	            JLabel valueLabel = new JLabel("Value: " + formatFloatValue(initial, numDec));
	            valueLabel.setFont(font);
	            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

	            wrapper.add(valueLabel, BorderLayout.NORTH);

	            // Update label by dividing integer value by scale
	            slider.addChangeListener(e -> {
	                float currentVal = slider.getValue() / scale;
	                valueLabel.setText("Value: " + formatFloatValue(currentVal, numDec));
	            });
	        }

	        parent.add(wrapper);
	        return slider;
	    }

	/**
     * Helper to format floats consistently based on decimal precision.
     */
    private static String formatFloatValue(float value, int numDec) {
        return String.format(Locale.ROOT, "%." + numDec + "f", value);
    }

	private static void validateArguments(Container parent, int min, int max,
			int initial, int majorTick, int minorTick) {
		Objects.requireNonNull(parent, "parent");
		if (min >= max || initial < min || initial > max || majorTick <= 0 || minorTick < 0) {
			throw new IllegalArgumentException("Invalid slider range, initial value, or tick spacing");
		}
	}


	// Format the slider value for display
    private static String formatValue(int value) {
        return "Value: " + value;
    }
}
