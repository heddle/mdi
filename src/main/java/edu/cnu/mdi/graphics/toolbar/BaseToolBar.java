package edu.cnu.mdi.graphics.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;

import edu.cnu.mdi.component.MagnifyWindow;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Bits;


/**
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class BaseToolBar extends CommonToolBar implements MouseListener, MouseMotionListener {


	/**
	 * Text field used for messages
	 */
	private JTextField _textField;

	// the clone button
	private CloneButton _cloneButton;

	// Zoom int by a fixed percentage
	private ZoomInButton _zoomInButton;

	// Zoom out by a fixed percentage
	private ZoomOutButton _zoomOutButton;

	// undo last zoom
	private UndoZoomButton _undoZoomButton;

	// refresh the container
	private RefreshButton _refreshButton;

	// zoom to whole world
	private WorldButton _worldButton;

	// default pointer tool
	private PointerButton _pointerButton;

	// rubber-band zoom
	private BoxZoomButton _boxZoomButton;

	// magnifying glass
	private MagnifyButton _magnifyButton;

	// center the view
	private CenterButton _centerButton;

	// create an ellipse
	private EllipseButton _ellipseButton;

	// add text to the view
	private TextButton _textButton;

	// pan the view
	private PanButton _panButton;

	// create a polygon
	private PolygonButton _polygonButton;

	// create a polygon
	private PolylineButton _polylineButton;

	// range (u r here) button
	private RangeButton _rangeButton;

	// draw a world rectangle
	private RectangleButton _rectangleButton;

	// draw a world rad arc
	private RadArcButton _radarcButton;

	// draw a world line
	private LineButton _lineButton;

	// delete selected items
	private DeleteButton _deleteButton;

	// toggle control panel
	private ControlPanelButton _cpButton;

	// the owner container
	private IContainer _container;

	// are there ANY bits set
	private boolean notNothing;

	/**
	 * Create a toolbar with all the buttons.
	 *
	 * @param container the container this toolbar controls.
	 */
	public BaseToolBar(IContainer container) {
		this(container, ToolBarBits.EVERYTHING);
	}

	/**
	 * Create a tool bar.
	 *
	 * @param container the container this toolbar controls.
	 * @param bits      controls which tools are added.
	 */
	public BaseToolBar(IContainer container, long bits) {
		// box layout needed for user component to work
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		_container = container;
		_container.setToolBar(this);
		notNothing = bits != ToolBarBits.NOTHING;
		makeButtons(bits);

		Component c = _container.getComponent();
		if (c != null) {
			c.addMouseListener(this);
			c.addMouseMotionListener(this);
		}
		setBorder(BorderFactory.createEtchedBorder());
		setFloatable(false);
	}

	/**
	 * Makes all the buttons.
	 *
	 * @param bits Bitwise test of which annotation buttons to add.
	 */
	protected void makeButtons(long bits) {

		if (notNothing) {
			if (!Bits.check(bits, ToolBarBits.NOZOOM)) {
				_zoomInButton = new ZoomInButton(_container);
				_zoomOutButton = new ZoomOutButton(_container);

				if (Bits.check(bits, ToolBarBits.UNDOZOOMBUTTON)) {
					_undoZoomButton = new UndoZoomButton(_container);
				}
				_worldButton = new WorldButton(_container);
				_boxZoomButton = new BoxZoomButton(_container);
			}
			_refreshButton = new RefreshButton(_container);
			if (Bits.check(bits, ToolBarBits.CENTERBUTTON)) {
				_centerButton = new CenterButton(_container);
			}

			if (Bits.check(bits, ToolBarBits.PANBUTTON)) {
				_panButton = new PanButton(_container);
			}

			if (Bits.check(bits, ToolBarBits.CLONEBUTTON)) {
				_cloneButton = new CloneButton(_container);
			}
		}

		if (notNothing && Bits.check(bits, ToolBarBits.RANGEBUTTON)) {
			_rangeButton = new RangeButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.MAGNIFYBUTTON)) {
			_magnifyButton = new MagnifyButton(_container);
		}

		// if (Bits.check(bits, POINTERBUTTON)) {
		_pointerButton = new PointerButton(_container);
		// }

		if (notNothing && Bits.check(bits, ToolBarBits.DELETEBUTTON)) {
			_deleteButton = new DeleteButton(_container);
			_deleteButton.setEnabled(false);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.CONTROLPANELBUTTON)) {
			_cpButton = new ControlPanelButton(_container);
		}

		// check if drawing tools are requested
		if (notNothing && Bits.check(bits, ToolBarBits.TEXTBUTTON)) {
			_textButton = new TextButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.ELLIPSEBUTTON)) {
			_ellipseButton = new EllipseButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.RECTANGLEBUTTON)) {
			_rectangleButton = new RectangleButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.RADARCBUTTON)) {
			_radarcButton = new RadArcButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.LINEBUTTON)) {
			_lineButton = new LineButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.POLYGONBUTTON)) {
			_polygonButton = new PolygonButton(_container);
		}

		if (notNothing && Bits.check(bits, ToolBarBits.POLYLINEBUTTON)) {
			_polylineButton = new PolylineButton(_container);
		}

		// add the pointer button and make it the default
		add(_pointerButton);

		if (_pointerButton != null) {
			setDefaultToggleButton(_pointerButton);
		}

		add(_cpButton, false); // false to prevent it joining button group
		add(_boxZoomButton);
		add(_zoomInButton);
		add(_zoomOutButton);
		add(_undoZoomButton);
		add(_panButton);
		add(_magnifyButton);
		add(_centerButton);
		add(_worldButton);
		add(_rangeButton);
		add(_refreshButton);
		add(_rectangleButton);
		add(_radarcButton);
		add(_lineButton);
		add(_ellipseButton);
		add(_polygonButton);
		add(_polylineButton);
		add(_textButton);
		add(_deleteButton);

		if (_cloneButton != null) {
			add(Box.createHorizontalStrut(8));
		}
		add(_cloneButton);

		// add the text field?

		if (notNothing && Bits.check(bits, ToolBarBits.TEXTFIELD)) {

			_textField = new JTextField(" ");

			_textField.setFont(Fonts.commonFont(Font.PLAIN, 11));
			_textField.setEditable(false);
			_textField.setBackground(Color.black);
			_textField.setForeground(Color.cyan);

			FontMetrics fm = getFontMetrics(_textField.getFont());
			Dimension d = _textField.getPreferredSize();
			d.width = fm.stringWidth(" ( 9999.99999 , 9999.99999 ) XXXXXXXXXXX");
			_textField.setPreferredSize(d);
			_textField.setMaximumSize(d);

			add(_textField);
		}


		enableDrawingButtons(true);

		// set the default button to on
		if (getDefaultToggleButton() != null) {
			resetDefaultSelection();
	//		getDefaultToggleButton().setSelected(true);
		}

	}

	@Override
	public Component add(Component c) {
		if (c == null) {
			return null;
		}
		return super.add(c);
	}

	/**
	 * Sets the text in the text field widget.
	 *
	 * @param text the new text.
	 */
	public void setText(String text) {
		if (_textField == null) {
			return;
		}

		if (text == null) {
			_textField.setText("");
		} else {
			_textField.setText(text);
		}
	}

	/**
	 * Enable/disable the drawing buttons
	 *
	 * @param enabled the desired stated.
	 */
	public void enableDrawingButtons(boolean enabled) {
		if (_ellipseButton != null) {
			_ellipseButton.setEnabled(enabled);
		}
		if (_rectangleButton != null) {
			_rectangleButton.setEnabled(enabled);
		}
		if (_radarcButton != null) {
			_radarcButton.setEnabled(enabled);
		}
		if (_lineButton != null) {
			_lineButton.setEnabled(enabled);
		}
		if (_polygonButton != null) {
			_polygonButton.setEnabled(enabled);
		}
		if (_polylineButton != null) {
			_polylineButton.setEnabled(enabled);
		}
		if (_textButton != null) {
			_textButton.setEnabled(enabled);
		}
	}

	/**
	 * Reset the default toggle button selection
	 */
	@Override
	public void resetDefaultSelection() {
		super.resetDefaultSelection();
		ToolBarToggleButton defaultButton = (ToolBarToggleButton) getDefaultToggleButton();

		if (defaultButton != null) {
			defaultButton.setSelected(true);
			_container.getComponent().setCursor(defaultButton.canvasCursor());
	//		defaultButton.requestFocus();
		}
	}

	/**
	 * Get the button used for a box (rubberband) zoom.
	 *
	 * @return the button used for a box (rubberband) zoom.
	 */
	public BoxZoomButton getBoxZoomButton() {
		return _boxZoomButton;
	}

	/**
	 * Get the button used for magnification.
	 *
	 * @return the button used for magnification.
	 */
	public MagnifyButton getMagnifyButton() {
		return _magnifyButton;
	}


	/**
	 * Get the button used for recentering.
	 *
	 * @return the button used for recentering.
	 */
	public CenterButton getCenterButton() {
		return _centerButton;
	}

	/**
	 * Get the toolbar's delete button.
	 *
	 * @return the toolbar's delete button.
	 */
	public DeleteButton getDeleteButton() {
		return _deleteButton;
	}

	/**
	 * Get the toolbar's control panel button.
	 *
	 * @return the toolbar's control panel button.
	 */
	public ControlPanelButton getControlPanelButton() {
		return _cpButton;
	}

	/**
	 * Get the toolbar's ellipse button.
	 *
	 * @return the toolbar's ellipse button.
	 */
	public EllipseButton getEllipseButton() {
		return _ellipseButton;
	}

	/**
	 * Get the toolbar's pan button.
	 *
	 * @return the toolbar's pan button.
	 */
	public PanButton getPanButton() {
		return _panButton;
	}

	/**
	 * Get the toolbar's point button.
	 *
	 * @return the toolbar's pointer button.
	 */
	public PointerButton getPointerButton() {
		return _pointerButton;
	}

	/**
	 * Get the toolbar's polygon button.
	 *
	 * @return the toolbar's polygon button.
	 */
	public PolygonButton getPolygonButton() {
		return _polygonButton;
	}

	/**
	 * Get the toolbar's polyline button.
	 *
	 * @return the toolbar's polyline button.
	 */
	public PolylineButton getPolylineButton() {
		return _polylineButton;
	}

	/**
	 * Get the toolbar's range button.
	 *
	 * @return the toolbar's range button.
	 */
	public RangeButton getRangeButton() {
		return _rangeButton;
	}

	/**
	 * Get the toolbar's rectangle button.
	 *
	 * @return the toolbar's rectangle button.
	 */
	public RectangleButton getRectangleButton() {
		return _rectangleButton;
	}

	/**
	 * Get the toolbar's radarc button.
	 *
	 * @return the toolbar's radarc button.
	 */
	public RadArcButton getRadArcButton() {
		return _radarcButton;
	}

	/**
	 * Get the toolbar's line button.
	 *
	 * @return the toolbar's line button.
	 */
	public LineButton getLineButton() {
		return _lineButton;
	}

	/**
	 * Get the toolbar's refresh button.
	 *
	 * @return the toolbar's refresh button.
	 */
	public RefreshButton getRefreshButton() {
		return _refreshButton;
	}

	/**
	 * Get the toolbar's text button.
	 *
	 * @return the toolbar's text button.
	 */
	public TextButton getTextButton() {
		return _textButton;
	}

	/**
	 * Get the toolbar's undozoom button.
	 *
	 * @return the toolbar's undozoom button.
	 */
	public UndoZoomButton getUndoZoomButton() {
		return _undoZoomButton;
	}

	/**
	 * Get the toolbar's world (default world zoom) button.
	 *
	 * @return the toolbar's world button.
	 */
	public WorldButton getWorldButton() {
		return _worldButton;
	}

	/**
	 * Get the toolbar's clone button.
	 *
	 * @return the toolbar's zoom-in button.
	 */
	public CloneButton getCloneButton() {
		return _cloneButton;
	}

	/**
	 * Get the toolbar's zoom-in button.
	 *
	 * @return the toolbar's zoom-in button.
	 */
	public ZoomInButton getZoomInButton() {
		return _zoomInButton;
	}

	/**
	 * Get the toolbar's zoom-out button.
	 *
	 * @return the toolbar's zoom-out button.
	 */
	public ZoomOutButton getZoomOutButton() {
		return _zoomOutButton;
	}

	/**
	 * The mouse was clicked. Note that the order the events will come is PRESSED,
	 * RELEASED, CLICKED. And a CLICKED will happen only if the mouse was not moved
	 * between press and release.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseClicked(MouseEvent mouseEvent) {

		if (!_container.getComponent().isEnabled()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		ToolBarToggleButton mtb = getActiveButton();
		if (mtb == null) {
			return;
		}

		boolean mb1 = (mouseEvent.getButton() == MouseEvent.BUTTON1) && !mouseEvent.isControlDown();
		boolean mb3 = (mouseEvent.getButton() == MouseEvent.BUTTON3)
				|| ((mouseEvent.getButton() == MouseEvent.BUTTON1) && mouseEvent.isControlDown());

		if (mb1) {
			if (mouseEvent.getClickCount() == 1) { // single click
				mtb.mouseClicked(mouseEvent);
			} else { // double (or more) clicks
				mtb.mouseDoubleClicked(mouseEvent);
			}
		} else if (mb3) {
			// mtb.mouseButton3Click(mouseEvent);
		}

	}

	/**
	 * The mouse has entered the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseEntered(MouseEvent mouseEvent) {

		ToolBarToggleButton mtb = getActiveButton();
		if (mtb != null) {
			_container.getComponent().setCursor(mtb.canvasCursor());
			mtb.mouseEntered(mouseEvent);
		}
	}

	/**
	 * The mouse has exited the container.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseExited(MouseEvent mouseEvent) {
		ToolBarToggleButton mtb = getActiveButton();

		if (mtb == null) {
			return;
		}

		mtb.mouseExited(mouseEvent);
	}

	/**
	 * The mouse was pressed. Note that the order the events will come is PRESSED,
	 * RELEASED, CLICKED. And a CLICKED will happen only if the mouse was not moved
	 * between press and release.
	 *
	 * @param me the causal event.
	 */
	@Override
	public void mousePressed(MouseEvent me) {

		if (!_container.getComponent().isEnabled()) {
			return;
		}

		ToolBarToggleButton mtb = getActiveButton();

		if (mtb == null) {
			return;
		}

		switch (me.getClickCount()) {
		case 1:

			// hack, if mouse button 2
			if (mtb == _pointerButton) {
				if ((_boxZoomButton != null) && (me.getButton() == MouseEvent.BUTTON2)) {
					mtb = _boxZoomButton;
				}
			}

			mtb.mousePressed(me);
			break;
		}

	}

	/**
	 * The mouse was clicked. Note that the order the events will come is PRESSED,
	 * RELEASED, CLICKED. And a CLICKED will happen only if the mouse was not moved
	 * between press and release. Also, the RELEASED will come even if the mouse was
	 * dragged off the container.
	 *
	 * @param me the causal event.
	 */
	@Override
	public void mouseReleased(MouseEvent me) {
		if (!_container.getComponent().isEnabled()) {
			return;
		}

		ToolBarToggleButton mtb = getActiveButton();

		if (mtb == null) {
			return;
		}

		// hack, if mouse button 2 treat as box zoom
		if (mtb == _pointerButton) {
			if (me.getButton() == MouseEvent.BUTTON2) {
				mtb = _boxZoomButton;
			}
		}

		mtb.mouseReleased(me);

	}

	/**
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
		if (!_container.getComponent().isEnabled()) {
			return;
		}

		ToolBarToggleButton mtb = getActiveButton();

		if (mtb == null) {
			return;
		}

		mtb.mouseDragged(mouseEvent);
	}

	/**
	 * The mouse has moved. Note will not come here if mouse button pressed, will go
	 * to DRAG instead.
	 *
	 * @param me the causal event.
	 */
	@Override
	public void mouseMoved(MouseEvent me) {
		ToolBarToggleButton mtb = getActiveButton();

		if (mtb == null) {
			return;
		}

		mtb.mouseMoved(me);
	}

	/**
	 * Convenience routine to get the active button.
	 *
	 * @return the active toggle button.
	 */
	@Override
	public ToolBarToggleButton getActiveButton() {
		return (ToolBarToggleButton) super.getActiveButton();
	}

	/**
	 * Called after each item event to give the toolbar a chance to reflect the
	 * correct state.
	 */
	public void checkButtonState() {
		if (_deleteButton != null) {
			_deleteButton.setEnabled(_container.anySelectedItems());
		}
	}
	/**
	 * @return the _textField
	 */
	public JTextField getTextField() {
		return _textField;
	}

	/**
	 * The active toggle button has changed
	 */
	@Override
	protected void activeToggleButtonChanged() {
		if (getActiveButton() != _magnifyButton) {
			MagnifyWindow.closeMagnifyWindow();
		}
		if (_container != null) {
			_container.activeToolBarButtonChanged(getActiveButton());
		}
	}

}
