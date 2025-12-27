package edu.cnu.mdi.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JWindow;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.toolbar.IContainerToolBar;

/**
 * A floating magnifier window used to display a zoomed-in view of a
 * {@link BaseContainer} around the current mouse location.
 * <p>
 * Typical usage is to call {@link #magnify(BaseContainer, MouseEvent)} from a
 * mouse event handler in the source container. The magnified view is shown in a
 * small {@link JWindow} near the mouse cursor, and the magnification factor is
 * controlled via the menu returned by {@link #magnificationMenu()}.
 * </p>
 * <p>
 * The implementation is intentionally singleton-based: there is at most one
 * {@code MagnifyWindow} instance shared across the application.
 * </p>
 *
 * @author David
 */
@SuppressWarnings("serial")
public class MagnifyWindow extends JWindow {

	/** The singleton magnify window instance. */
	private static MagnifyWindow _magnifyWindow;

	/** Width of the magnify window in pixels. */
	private static int _WIDTH = 204;

	/** Height of the magnify window in pixels. */
	private static int _HEIGHT = 204;

	/** Pixel offset of the magnify window from the mouse pointer location. */
	private static final int OFFSET = 10;

	/** Mouse location in source container coordinates. */
	private static Point _mouseLocation;

	/** The source container being magnified. */
	private static BaseContainer _sourceContainer;

	/**
	 * World coordinates of the center of the magnified view. This is the world
	 * point under the mouse in the source container.
	 */
	private static final Point2D.Double _worldCenter = new Point2D.Double();

	/** The internal drawing container used to render the magnified view. */
	private static BaseContainer _container;

	/**
	 * Optional extra "after draw" hook used to overlay cross hairs and delegate to
	 * the parent container's after-draw handler.
	 */
	private static IDrawable _extraAfterDraw;


	/** Menu used to select the magnification factor. */
	private static JMenu _magMenu;

	/** Available magnification factors. */
	private static final Integer[] _mags = { 2, 3, 4, 5, 6, 7, 8, 9, 10 };

	/** Currently selected magnification factor. */
	private static int _selectedMag = 4;

	/**
	 * Creates the magnify window and its backing {@link BaseContainer}.
	 * <p>
	 * The constructor is private because this class is managed as a singleton via
	 * the static methods.
	 * </p>
	 */
	private MagnifyWindow() {

		setLayout(new BorderLayout(0, 0));
		setSize(_WIDTH, _HEIGHT);

		_container = new BaseContainer(new Rectangle2D.Double(0, 0, 1, 1)) {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
			}
		};

		add(_container, BorderLayout.CENTER);
	}

	/**
	 * Returns a menu that allows the user to select the magnification factor used
	 * by the magnifier window.
	 * <p>
	 * The menu is lazily created on first call and then reused. It is intended to
	 * be inserted into the application's main menu bar.
	 * </p>
	 *
	 * @return the magnification factor menu
	 */
	public static JMenu magnificationMenu() {
		if (_magMenu != null) {
			return _magMenu;
		}

		_magMenu = new JMenu("Magnification Factor");
		ButtonGroup bga = new ButtonGroup();

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				_selectedMag = Integer.parseInt(e.getActionCommand());
				// System.err.println("New mag selection " + _selectedMag);
			}

		};

		for (Integer _mag : _mags) {
			JRadioButtonMenuItem mitem = new JRadioButtonMenuItem(_mag.toString(), _selectedMag == _mag);
			bga.add(mitem);
			mitem.addActionListener(al);
			_magMenu.add(mitem);
		}

		return _magMenu;
	}

	/**
	 * Shows the magnifier window for a given source container at the location of
	 * the supplied mouse event.
	 * <p>
	 * The magnifier window is positioned near the mouse pointer and configured to
	 * display a zoomed-in view of the region under the cursor. If the source
	 * container provides a custom magnification draw hook, that will be used;
	 * otherwise the source container's model is shared and the magnifier behaves
	 * like a zoomed copy.
	 * </p>
	 *
	 * @param sourceContainer the {@link BaseContainer} to magnify (must not be
	 *                        {@code null})
	 * @param event           the mouse event that triggered the magnification (must
	 *                        not be {@code null})
	 */
	public static synchronized void magnify(final BaseContainer sourceContainer, MouseEvent event) {

		if (sourceContainer == null || event == null) {
			return;
		}

		_sourceContainer = sourceContainer;

		if (_magnifyWindow == null) {
			_magnifyWindow = new MagnifyWindow();
		}

		// Mouse location in source-container coordinates
		_mouseLocation = new Point(event.getPoint());

		// Get the mouse pointer location in screen coordinates
		Point mouseScreenPoint = MouseInfo.getPointerInfo() != null
				? MouseInfo.getPointerInfo().getLocation()
				: new Point(0, 0);

		// Compute where to place the magnify window, trying to avoid going off-screen
		int screenX = mouseScreenPoint.x;
		int screenY = mouseScreenPoint.y;

		int x = screenX - _WIDTH - OFFSET;
		if (x < 0) {
			x = screenX + OFFSET;
		}
		int y = screenY - _HEIGHT - OFFSET;
		if (y < 0) {
			y = screenY + OFFSET;
		}

		_magnifyWindow.setLocation(x, y);

		// Set the world system for the internal container to the magnified region
		_container.setWorldSystem(getMagWorld(sourceContainer));

		// Share items or use specialized draw?
			_container.shareModel(sourceContainer);

			final IDrawable parentAfterDraw = sourceContainer.getAfterDraw();

			_extraAfterDraw = new DrawableAdapter() {

				@Override
				public void draw(Graphics2D g, edu.cnu.mdi.container.IContainer container) {
					// Delegate to parent's after-draw, if any
					if (parentAfterDraw != null) {
						parentAfterDraw.draw(g, _container);
					}

					// Draw a crosshair at the center of the magnify window
					Rectangle bounds = container.getComponent().getBounds();
					int xc = bounds.x + bounds.width / 2;
					int yc = bounds.y + bounds.height / 2;

					int S2 = 8;
					g.setColor(Color.cyan);
					g.drawLine(xc - S2, yc - 1, xc - 1, yc - 1);
					g.drawLine(xc - 1, yc - S2, xc - 1, yc - 1);
					g.drawLine(xc + S2, yc + 1, xc + 1, yc + 1);
					g.drawLine(xc + 1, yc + S2, xc + 1, yc + 1);
					g.setColor(Color.red);
					g.drawLine(xc - S2, yc, xc + S2, yc);
					g.drawLine(xc, yc - S2, xc, yc + S2);
				}
			};

			_container.setAfterDraw(_extraAfterDraw);

		_container.setDirty(true);
		_container.refresh();

		_magnifyWindow.setVisible(true);
		_magnifyWindow.toFront();
	}

	/**
	 * Computes the world-coordinate rectangle that should be displayed in the
	 * magnify window, based on the current mouse location and magnification factor.
	 *
	 * @param sourceContainer the source container being magnified
	 * @return the world rectangle to display in the magnifier
	 */
	private static Rectangle2D.Double getMagWorld(BaseContainer sourceContainer) {

		Rectangle magBounds = _container.getBounds();
		Rectangle srcBounds = sourceContainer.getBounds();

		Rectangle2D.Double srcWorld = sourceContainer.getWorldSystem();
		Rectangle2D.Double worldRegion = new Rectangle2D.Double(srcWorld.x, srcWorld.y, srcWorld.width,
				srcWorld.height);

		// Convert the current mouse location to world coordinates in the source
		// container
		Point pp = new Point(_mouseLocation.x, _mouseLocation.y);
		sourceContainer.localToWorld(pp, _worldCenter);

		// Scale factors between the source container and the magnifier window
		double scaleX = ((double) srcBounds.width) / ((double) magBounds.width);
		double scaleY = ((double) srcBounds.height) / ((double) magBounds.height);

		// Determine the width and height of the magnified region in world coordinates
		double ww = srcWorld.width / (scaleX * _selectedMag);
		double hh = srcWorld.height / (scaleY * _selectedMag);

		worldRegion.setFrame(_worldCenter.x - ww / 2.0, _worldCenter.y - hh / 2.0, ww, hh);

		return worldRegion;
	}

	/**
	 * Returns the world coordinates of the center of the magnifier window.
	 * <p>
	 * This is the world point currently under the mouse in the source container and
	 * corresponds to the center of the magnified region.
	 * </p>
	 *
	 * @return the world coordinates of the center of the magnified region
	 */
	public static Point2D.Double getWorldCenter() {
		return _worldCenter;
	}

	/**
	 * Hides the magnify window if it is currently visible.
	 */
	public static void closeMagnifyWindow() {

	    if (_magnifyWindow == null) {
	        return;
	    }

	    if (_magnifyWindow.isVisible()) {
	        _magnifyWindow.setVisible(false);

	        if (_sourceContainer != null) {
	            IContainerToolBar tb = _sourceContainer.getToolBar();
	            if (tb != null) {
	                tb.resetDefaultSelection();
	            }
	            _sourceContainer = null;
	        }
	    }
	}

	/**
	 * Returns the insets to use for this window.
	 * <p>
	 * This implementation slightly increases the default insets, which can help
	 * provide a small visual margin around the magnified content.
	 * </p>
	 *
	 * @return the insets for this window
	 */
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2, def.right + 2);
	}
}
