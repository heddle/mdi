package edu.cnu.mdi.view;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Properties;

import javax.swing.JComponent;

import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.graphics.rubberband.ARubberband;

/**
 * Fluent builder for constructing the {@link Properties} used to initialize
 * a {@link BaseView}.
 * <p>
 * This is intended primarily for default view configurations, replacing the
 * older pattern of assembling raw {@code Object... keyVals} arrays.
 * </p>
 * <p>
 * The builder stores values directly into a {@link Properties} instance using
 * the same keys consumed by {@link PropertyUtils} and {@link BaseView}.
 * Because {@link Properties} ultimately extends {@link java.util.Hashtable},
 * non-String values such as colors, classes, or components may be stored
 * directly when appropriate.
 * </p>
 * <p>
 * Only a commonly used subset of properties is given dedicated fluent methods
 * here. For less common properties, use {@link #put(String, Object)}.
 * </p>
 */
public class ViewPropertiesBuilder {

	/** Backing properties instance being built. */
	private final Properties props = new Properties();

	/**
	 * Create an empty builder.
	 */
	public ViewPropertiesBuilder() {
		super();
	}

	/**
	 * Store an arbitrary property value.
	 *
	 * @param key   the property key
	 * @param value the property value
	 * @return this builder
	 */
	public ViewPropertiesBuilder put(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("Property key must not be null.");
		}
		if (value == null) {
			props.remove(key);
		} else {
			props.put(key, value);
		}
		return this;
	}

	/**
	 * Set the view title.
	 *
	 * @param title the title
	 * @return this builder
	 */
	public ViewPropertiesBuilder title(String title) {
		return put(PropertyUtils.TITLE, title);
	}

	/**
	 * Set the initial visibility of the view.
	 *
	 * @param visible {@code true} if initially visible
	 * @return this builder
	 */
	public ViewPropertiesBuilder visible(boolean visible) {
		return put(PropertyUtils.VISIBLE, visible);
	}

	/**
	 * Set the fraction of the application height to use for the view.
	 *
	 * @param fraction the height fraction
	 * @return this builder
	 */
	public ViewPropertiesBuilder fraction(double fraction) {
		return put(PropertyUtils.FRACTION, fraction);
	}

	/**
	 * Set the aspect ratio used with fractional sizing.
	 *
	 * @param aspect the aspect ratio
	 * @return this builder
	 */
	public ViewPropertiesBuilder aspect(double aspect) {
		return put(PropertyUtils.ASPECT, aspect);
	}

	/**
	 * Set the toolbar bit mask.
	 *
	 * @param toolBits the toolbar bits, typically assembled from {@link ToolBits}
	 * @return this builder
	 */
	public ViewPropertiesBuilder toolbarBits(long toolBits) {
		return put(PropertyUtils.TOOLBARBITS, toolBits);
	}

	/**
	 * Set whether mouse-wheel zoom is enabled.
	 *
	 * @param enabled {@code true} to enable mouse-wheel zoom
	 * @return this builder
	 */
	public ViewPropertiesBuilder wheelZoom(boolean enabled) {
		return put(PropertyUtils.WHEELZOOM, enabled);
	}

	/**
	 * Set the background color.
	 *
	 * @param color the background color
	 * @return this builder
	 */
	public ViewPropertiesBuilder background(Color color) {
		return put(PropertyUtils.BACKGROUND, color);
	}

	/**
	 * Set the initial world system.
	 *
	 * @param worldSystem the world system rectangle
	 * @return this builder
	 */
	public ViewPropertiesBuilder worldSystem(Rectangle2D.Double worldSystem) {
		return put(PropertyUtils.WORLDSYSTEM, worldSystem);
	}
	
	/**
	 * Set whether to use a container for the view.
	 *
	 * @param useContainer {@code true} to use a container
	 * @return this builder
	 */
	public ViewPropertiesBuilder useContainer(boolean useContainer) {
		return put(PropertyUtils.USECONTAINER, useContainer);
	}

	/**
	 * Set the explicit container class to instantiate.
	 *
	 * @param containerClass the container class
	 * @return this builder
	 */
	public ViewPropertiesBuilder containerClass(Class<?> containerClass) {
		return put(PropertyUtils.CONTAINERCLASS, containerClass);
	}

	/**
	 * Set whether the view should be scrollable.
	 *
	 * @param scrollable {@code true} if scrollable
	 * @return this builder
	 */
	public ViewPropertiesBuilder scrollable(boolean scrollable) {
		return put(PropertyUtils.SCROLLABLE, scrollable);
	}

	/**
	 * Set the left position.
	 *
	 * @param left the left pixel coordinate
	 * @return this builder
	 */
	public ViewPropertiesBuilder left(int left) {
		return put(PropertyUtils.LEFT, left);
	}

	/**
	 * Set the top position.
	 *
	 * @param top the top pixel coordinate
	 * @return this builder
	 */
	public ViewPropertiesBuilder top(int top) {
		return put(PropertyUtils.TOP, top);
	}

	/**
	 * Set the initial width.
	 *
	 * @param width the width in pixels
	 * @return this builder
	 */
	public ViewPropertiesBuilder width(int width) {
		return put(PropertyUtils.WIDTH, width);
	}

	/**
	 * Set the initial height.
	 *
	 * @param height the height in pixels
	 * @return this builder
	 */
	public ViewPropertiesBuilder height(int height) {
		return put(PropertyUtils.HEIGHT, height);
	}

	/**
	 * Set standard view decorations.
	 *
	 * @param enabled {@code true} to use standard decorations
	 * @return this builder
	 */
	public ViewPropertiesBuilder standardDecorations(boolean enabled) {
		return put(PropertyUtils.STANDARDVIEWDECORATIONS, enabled);
	}

	/**
	 * Set whether the view is iconifiable.
	 *
	 * @param enabled {@code true} if iconifiable
	 * @return this builder
	 */
	public ViewPropertiesBuilder iconifiable(boolean enabled) {
		return put(PropertyUtils.ICONIFIABLE, enabled);
	}

	/**
	 * Set whether the view is maximizable.
	 *
	 * @param enabled {@code true} if maximizable
	 * @return this builder
	 */
	public ViewPropertiesBuilder maximizable(boolean enabled) {
		return put(PropertyUtils.MAXIMIZABLE, enabled);
	}

	/**
	 * Set whether the view is resizable.
	 *
	 * @param enabled {@code true} if resizable
	 * @return this builder
	 */
	public ViewPropertiesBuilder resizable(boolean enabled) {
		return put(PropertyUtils.RESIZABLE, enabled);
	}

	/**
	 * Set whether the view is closable.
	 *
	 * @param enabled {@code true} if closable
	 * @return this builder
	 */
	public ViewPropertiesBuilder closable(boolean enabled) {
		return put(PropertyUtils.CLOSABLE, enabled);
	}

	/**
	 * Set a split-pane west component.
	 *
	 * @param component the west component
	 * @return this builder
	 */
	public ViewPropertiesBuilder splitWestComponent(JComponent component) {
		return put(PropertyUtils.SPLITWESTCOMPONENT, component);
	}

	/**
	 * Set the rubber-band box zoom policy.
	 *
	 * @param policy the rubber-band policy
	 * @return this builder
	 */
	public ViewPropertiesBuilder boxZoomPolicy(ARubberband.Policy policy) {
		return put(PropertyUtils.BOXZOOMRBPOLICY, policy);
	}

	/**
	 * Build a new {@link Properties} object containing the configured values.
	 * <p>
	 * A defensive copy is returned so the builder may continue to be reused
	 * independently if desired.
	 * </p>
	 *
	 * @return a new properties object
	 */
	public Properties build() {
		Properties copy = new Properties();
		copy.putAll(props);
		return copy;
	}
}