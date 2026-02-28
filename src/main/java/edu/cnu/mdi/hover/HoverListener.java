package edu.cnu.mdi.hover;

/**
 * Listener interface for receiving hover events. Implementations can define
 * behavior for when a hover starts (hoverUp) and when it ends (hoverDown).
 */
public interface HoverListener {
	
	/**
	 * Called when a hover event starts. Implementations can use the provided
	 * HoverEvent to access the source component and hover location.
	 * @param he the HoverEvent containing details about the hover action
	 */
    public void hoverUp(HoverEvent he);
    
    /**
	 * Called when a hover event ends. Implementations can perform cleanup or
	 * state updates as needed when the hover is no longer active.
	 * @param he the HoverEvent containing details about the hover action that is ending
	 */
    public void hoverDown(HoverEvent he);
}