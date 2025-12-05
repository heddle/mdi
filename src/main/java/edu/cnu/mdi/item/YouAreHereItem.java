package edu.cnu.mdi.item;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.ImageManager;


public class YouAreHereItem extends PointItem {

	/**
	 * Constructor for a YouAreHereItem which is like a reference point.
	 *
	 * @param itemList the item list
	 */
	public YouAreHereItem(ItemList itemList) {
		super(itemList, ImageManager.getInstance().loadImageIcon("images/youarehere.gif"));

		this.setAlignmentV(BOTTOM);
	}

	/**
	 * Convenience function to create a YouAreHere item
	 *
	 * @param container the container whose one and only YouAreHereItem is being
	 *                  created.
	 * @param location  the default location.
	 * @return the create YouAreHereItem
	 */
	public static YouAreHereItem createYouAreHereItem(IContainer container, Point2D.Double location) {
		ItemList glassLayer = container.getGlassList();
		YouAreHereItem item = new YouAreHereItem(glassLayer);

		container.setYouAreHereItem(item);

		item.setDraggable(true);
		item.setDeletable(true);
		item.setLocked(false);
		item.setFocus(location);
		return item;
	}

	/**
	 * Add any appropriate feedback strings panel.
	 *
	 * @param container       the Base container.
	 * @param pp              the mouse location.
	 * @param Point2D         .Double the corresponding world point.
	 * @param feedbackStrings the List of feedback strings to add to.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {

		if (contains(container, pp)) {
			String s = "Anchor at " + container.getLocationString(_focus);
			feedbackStrings.add(s);
		}
	}

}
