package edu.cnu.mdi.graphics;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class SliderFactory {

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
	 */
	public static JSlider createLabeledSlider(Container parent, int min, int max, 
			int initial, int majorTick, int minorTick, Font font,
			boolean showValue) {
       
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
            valueLabel.setHorizontalAlignment(JLabel.CENTER);
            
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
	 */
	  public static JSlider createLabeledSlider(Container parent, float min, float max, 
	            float initial, float majorTick, float minorTick, Font font,
	            boolean showValue, int numDec) {
	        
	        // 1. Calculate scaling factor (e.g., 2 decimal places = 100)
	        final float scale = (float) Math.pow(10, numDec);
	        
	        // 2. Convert float bounds to integer bounds for the JSlider
	        int iMin = Math.round(min * scale);
	        int iMax = Math.round(max * scale);
	        int iInitial = Math.round(initial * scale);
	        int iTick = Math.round(majorTick * scale);
	        int iMinorTick = Math.round(minorTick * scale);

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
	            valueLabel.setHorizontalAlignment(JLabel.CENTER);
	            
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
        return String.format("%." + numDec + "f", value);
    }
 

	// Format the slider value for display
    private static String formatValue(int value) {
        return "Value: " + value;
    }
}
