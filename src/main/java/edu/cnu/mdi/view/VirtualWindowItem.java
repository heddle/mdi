package edu.cnu.mdi.view;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.world.WorldGraphicsUtils;
import edu.cnu.mdi.item.ItemModification.ModificationType;
import edu.cnu.mdi.item.RectangleItem;

//getAnnotationLayer()
public class VirtualWindowItem extends RectangleItem {

	// lives on the virtual view
	private VirtualView _vview;

	private BaseView _baseView;
	
	//margin for the virtual window border
	private static final int del = 20;

	/**
	 * Create a virtual window item that represents a base view inside a virtual
	 * view.
	 *
	 * @param vview    the virtual view this item is in.
	 * @param baseView the base view this item represents.
	 */
	public VirtualWindowItem(VirtualView vview, BaseView baseView) {
		super(vview.getContainer().getAnnotationLayer(), getWorldRect(baseView));
		_vview = vview;
		_baseView = baseView;
		_baseView.setVirtualItem(this);

		setRightClickable(true);
		setDraggable(true);
		setRotatable(false);
		setResizable(false);
		setDeletable(false);
		setLocked(false);
		setEnabled(true);
		setVisible(false);
	}

	/**
	 * Called when the item was double clicked. The default implementation is to
	 * edit the item's properties. Here we want a no-op.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void doubleClicked(MouseEvent mouseEvent) {
	}

	/**
	 * Sets whether this item is marked as selected.
	 * Here we want a no-op.
	 *
	 * @param selected the new value of the flag.
	 */
	@Override
	public void setSelected(boolean selected) {
	}

	/**
	 * A modification such as a drag, resize or rotate is continuing.
	 */
	@Override
	public void modify() {
		System.out.println("Modifying virtual window item...");

		if (_modification.getType() == ModificationType.DRAG) {
			_path = (Path2D.Double) (_modification.getStartPath().clone());

			Point2D.Double swp = _modification.getStartWorldPoint();
			Point2D.Double cwp = _modification.getCurrentWorldPoint();
			double dx = cwp.x - swp.x;
			double dy = cwp.y - swp.y;
			AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
			_path.transform(at);

			if (_secondaryPoints != null) {
				Path2D.Double path2 = (Path2D.Double) _modification.getSecondaryPath().clone();
				path2.transform(at);
				WorldGraphicsUtils.pathToWorldPolygon(path2, _secondaryPoints);
			}

			// fix focus
			Point2D.Double sf = _modification.getStartFocus();

			if ((sf != null) && (_focus != null)) {
				at.transform(sf, _focus);
			}

			_vview.getContainer().refresh();
		}
	}

	/**
	 * A modification such as a drag, resize or rotate has ended.
	 */
	@Override
	public void stopModification() {
		
		if (_modification == null) {
			return;
		}
		switch (_modification.getType()) {
		case DRAG:

			Point p = _modification.getCurrentMousePoint();
			if (!_vview.getContainer().getComponent().getBounds().contains(p)) {
				System.err.println("Cant drag out of vv!");
				_vview.getContainer().refresh();
				break;
			}

			Point2D.Double wp0 = _modification.getStartWorldPoint();
			Point2D.Double wp1 = _modification.getCurrentWorldPoint();
			int dh = (int) (wp1.x - wp0.x);
			int dv = (int) (wp0.y - wp1.y);

			_baseView.offset(dh, dv);
			setLocation();

			_vview.getContainer().refresh();
			break;

		default:
			break;
		}
		super.stopModification();
	}

	/**
	 * Custom drawer for the item.
	 *
	 * @param g         the graphics context.
	 * @param container the graphical container being rendered.
	 */
	@Override
	public void drawItem(Graphics g, IContainer container) {
		if (!_baseView.isVisible() || _baseView.isClosed() || _baseView.isIcon()) {
			setEnabled(false);
		} else {
			super.drawItem(g, container);
			setEnabled(true);
		}
	}

	private static Rectangle2D.Double getWorldRect(BaseView bv) {
		Rectangle b = bv.getBounds();
		Rectangle2D.Double wr = new Rectangle2D.Double();
		wr.x = b.x+del;
		wr.y = b.y + b.height-del;
		wr.width = b.width-2*del;
		wr.height = b.height;
		return wr;
	}

	public void setLocation() {
		Rectangle2D.Double world = _vview.getContainer().getWorldSystem();
		Rectangle bvBounds = _baseView.getBounds();
		Rectangle2D.Double wr = new Rectangle2D.Double();

		Point2D.Double offset = _vview.totalOffset();

		wr.x = offset.x + bvBounds.x + del;
		wr.y = offset.y + world.y + world.height - (bvBounds.y + bvBounds.height) - del;
		
		wr.width = bvBounds.width - 2*del;
		wr.height = bvBounds.height - 2*del;

		setPath(WorldGraphicsUtils.getPoints(wr));
		_vview.getContainer().refresh();
	}

	/**
	 * Get the base view that this item represents
	 *
	 * @return the base view
	 */
	public BaseView getBaseView() {
		return _baseView;
	}
}
