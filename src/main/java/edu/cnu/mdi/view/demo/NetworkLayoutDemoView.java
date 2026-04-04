package edu.cnu.mdi.view.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JPanel;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.graphics.toolbar.AToolBar;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;

@SuppressWarnings("serial")
public class NetworkLayoutDemoView extends BaseView {

	/** Default title used both for the frame title and lazy menu registration. */
	private static final String TITLE = "Network Layout Demo View";

	// The drawing z layer to place the devices on
	private final Layer deviceLayer;

	// default side panel width (control panel + feedback)
	private static final int SIDE_PANEL_WIDTH = 220;

	// draws the snap to grid
	private final GridDrawer gridDrawer;

	// whether to show the node names
	private boolean showNodeNames = true;

	/**
	 * Construct the demo view using its canonical default properties.
	 */
	public NetworkLayoutDemoView() {
		this(createDefaultProperties());
	}

	/**
	 * Construct a Network Layout Demo View with the given properties.
	 * <p>
	 * This is a demo, not a serious application. It demonstrates placing network
	 * devices on a canvas, with feedback and some custom toolbar buttons.
	 * </p>
	 *
	 * @param props the properties used to configure the view
	 */
	public NetworkLayoutDemoView(Properties props) {
		super(props);
		deviceLayer = new Layer(getContainer(), "Devices");
		getContainer().getFeedbackControl().addFeedbackProvider(this);
		addToToolBar();

		// add an underlying snap-to grid drawer
		gridDrawer = new GridDrawer(getContainer(), 20, X11Colors.getX11Color("light gray"));

		// add an east side panel with a control panel and feedback
		initEastSidePanel();
	}

	/**
	 * Create the default properties for this view.
	 *
	 * @return the default view properties
	 */
	private static Properties createDefaultProperties() {
		long toolBits = ToolBits.NAVIGATIONTOOLS | ToolBits.DELETE | ToolBits.CONNECTOR;

		return new ViewPropertiesBuilder()
				.fraction(0.7)
				.aspect(1.2)
				.toolbarBits(toolBits)
				.visible(true)
				.wheelZoom(true)
				.background(X11Colors.getX11Color("alice blue"))
				.title(TITLE)
				.build();
	}

	/**
	 * Get the view configuration for lazy creation.
	 *
	 * @return the view configuration for lazy creation
	 */
	public static ViewConfiguration<NetworkLayoutDemoView> getConfiguration() {
		return ViewConfiguration.lazy(TITLE, NetworkLayoutDemoView::new, 4, 0, 0, VirtualView.CENTER);
	}

	// initialize the east side panel with feedback and controls
	private void initEastSidePanel() {
		// feedback control and provider (use custom coloring)
		FeedbackPane fbp = initFeedback(Color.white, X11Colors.getX11Color("dark green"), 10);

		ControlPanel cp = new ControlPanel(this);

		// container panel holding control panel (NORTH) and feedback (CENTER)
		JPanel sidePanel = new JPanel(new BorderLayout());

		// ensure a consistent preferred width for the whole side strip
		Dimension feedbackPref = fbp.getPreferredSize();
		fbp.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, feedbackPref.height));

		sidePanel.add(cp, BorderLayout.NORTH);
		sidePanel.add(fbp, BorderLayout.CENTER);
		sidePanel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, getHeight()));
		add(sidePanel, BorderLayout.EAST);
	}

	// add the custom buttons to the toolbar
	private void addToToolBar() {
		AToolBar tb = getContainer().getToolBar();
		if (tb == null) {
			return;
		}

		// add a space (in pixels) between existing "built in" buttons and our buttons
		tb.spacer(8);

		// this is a one-shot button that represents snapping the items to a grid
		new GridButton(this);

		// add a mutually exclusive toolbar toggle button for all known devices
		for (EDeviceSymbol ds : EDeviceSymbol.values()) {
			new DeviceButton(this, ds);
		}
	}

	/**
	 * Whether to show the node names on the device items.
	 *
	 * @return true if node names should be shown, false otherwise
	 */
	public boolean showNames() {
		return showNodeNames;
	}

	/**
	 * Set whether to show the node names on the device items.
	 *
	 * @param show true to show node names, false to hide them
	 */
	public void setShowNames(boolean show) {
		this.showNodeNames = show;
		refresh();
	}

	/**
	 * Get the device layer where device items are placed.
	 *
	 * @return the device layer
	 */
	public Layer getDeviceLayer() {
		return deviceLayer;
	}

	// Provide feedback strings showing screen and world coordinates
	// items will add to the feedback when they are mouse-overed
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Double wp, List<String> feedbackStrings) {

		String coordStrPx = String.format("Screen Coordinates: (%d, %d)", pp.x, pp.y);
		String coordStr = String.format("World Coordinates: (%.2f, %.2f)", wp.x, wp.y);

		int numDevices = getAllDevices().size();
		String deviceCountStr = String.format("Number of Devices: %d", numDevices);

		feedbackStrings.add(coordStrPx);
		feedbackStrings.add(coordStr);
		feedbackStrings.add(deviceCountStr);
		feedbackStrings.add("$orange$Zoom Level: " + String.format("%.2f%%", container.approximateZoomFactor() * 100));
	}

	/**
	 * Get the grid drawer used in this view.
	 *
	 * @return the grid drawer
	 */
	public GridDrawer getGridDrawer() {
		return gridDrawer;
	}

	/**
	 * Get all the device items currently on the device layer.
	 *
	 * @return list of all device items
	 */
	public List<DeviceItem> getAllDevices() {
		ArrayList<DeviceItem> devices = new ArrayList<>();
		for (AItem item : deviceLayer.getAllItems()) {
			if (item instanceof DeviceItem) {
				devices.add((DeviceItem) item);
			}
		}
		return devices;
	}
}