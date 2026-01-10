package edu.cnu.mdi.item;

import java.util.EventListener;

public interface ItemChangeListener extends EventListener {
	/**
	 * One of the drawables in the set has changed.
	 *
	 * @param layer the z layer containing the chanhed item.
	 * @param item  the AItem in question.
	 * @param type  the type of change.
	 */
	public void itemChanged(Layer layer, AItem item, ItemChangeType type);
}
