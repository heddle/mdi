package edu.cnu.mdi.graphics.drawable;

import java.awt.Graphics2D;

import edu.cnu.mdi.container.IContainer;

/**
 * A convenience base class that provides no-op (empty) implementations of all
 * {@link IDrawable} methods. Subclasses may override only the behaviors they
 * need, which makes this class useful when implementing the Adapter pattern.
 * <p>
 * The adapter also maintains standard properties such as visibility and enabled
 * status, and an optional name field for identification in debugging or UI
 * contexts.
 *
 * <p>
 * <b>Typical usage:</b>
 *
 * <pre>
 * class GridOverlay extends DrawableAdapter {
 *     {@literal @}Override
 *     public void draw(Graphics2D g, Container2D container) {
 *         // custom drawing code here
 *     }
 * }
 * </pre>
 *
 * @author heddle
 */
public class DrawableAdapter implements IDrawable {

	/** Optional display or debugging name for this drawable. */
	private String name;

	/** Whether this drawable should be rendered. */
	private boolean visible = true;

	/** Whether this drawable is enabled (app-defined semantics). */
	private boolean enabled = true;

	/**
	 * Creates a new adapter with no name.
	 */
	public DrawableAdapter() {
	}

	/**
	 * Creates a new adapter with the given name.
	 *
	 * @param name a human-readable identifier for this drawable
	 */
	public DrawableAdapter(String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation performs no drawing. Subclasses should override
	 * this method to render custom graphics.
	 */
	@Override
	public void draw(Graphics2D g, IContainer container) {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation does nothing, but subclasses may override to
	 * release resources or unregister listeners.
	 */
	@Override
	public void prepareForRemoval() {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The base adapter has no internal cache, but subclasses that cache computed
	 * shapes or pixel data may override this hook.
	 *
	 * @param dirty whether cached data should be invalidated
	 */
	@Override
	public void setDirty(boolean dirty) {
		// no-op
	}

	/**
	 * Returns the human-readable name for this drawable, or {@code null} if none
	 * was set.
	 *
	 * @return the drawable name, or null
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name for this drawable.
	 *
	 * @param name the new name, may be null
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isVisible() {
		return visible;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
