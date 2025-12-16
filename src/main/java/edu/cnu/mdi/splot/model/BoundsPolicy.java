package edu.cnu.mdi.splot.model;

/**
 * Plot bounds policy.
 * <ul>
 *   <li>{@link #AUTO}: view bounds are derived from data bounds</li>
 *   <li>{@link #MANUAL}: view bounds are locked to a user-specified rectangle</li>
 * </ul>
 */
public enum BoundsPolicy {
    AUTO,
    MANUAL
}
