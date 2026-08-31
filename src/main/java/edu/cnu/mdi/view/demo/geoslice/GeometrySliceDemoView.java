package edu.cnu.mdi.view.demo.geoslice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.geometry.Plane;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.PointItem;
import edu.cnu.mdi.item.PolygonItem;
import edu.cnu.mdi.swing.SwingSizingUtils;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewPropertiesBuilder;

/**
 * Demonstration view showing a two-dimensional slice through a synthetic
 * three-dimensional geometry model.
 *
 * <p>
 * The view is intentionally built from ordinary MDI 2D items. The underlying
 * model is three-dimensional: it contains region shells and many wire-like line
 * segments. The vertical slider controls the azimuthal angle {@code phi} of a
 * constant-phi slicing plane. For each slider value, the 3D shells and wires
 * are intersected with that plane, projected into 2D slice coordinates, and
 * displayed as {@link PolygonItem}s and {@link PointItem}s.
 * </p>
 *
 * <p>
 * The purpose of this demo is not to replace the MDI 3D extension. Instead, it
 * illustrates a common scientific and engineering use case: a diagnostic 2D
 * slice of a 3D object can be easier to inspect, pick, and interrogate than the
 * full 3D rendering.
 * </p>
 *
 * <h2>Display convention</h2>
 * <pre>
 *     slice x = z
 *     slice y = radial distance in the selected phi plane
 * </pre>
 *
 * <p>
 * At {@code phi = 0}, the view shows {@code z} horizontally to the right,
 * {@code x} vertically, and {@code y} out of the screen. As the slider changes,
 * the slice plane rotates in azimuth.
 * </p>
 */
@SuppressWarnings("serial")
public class GeometrySliceDemoView extends BaseView {

    private static final String TITLE = "Geometry Slice Demo View";

    /** Width of the east-side feedback panel, in pixels. */
    private static final int SIDE_PANEL_WIDTH = 320;

    /** Minimum slider phi angle, in degrees. */
    private static final int PHI_MIN = -25;

    /** Maximum slider phi angle, in degrees. */
    private static final int PHI_MAX = 25;

    /** Initial slider phi angle, in degrees. */
    private static final int PHI_INITIAL = 0;

    /** Readable wrap width for the west-panel explanation. */
    private static final int HELP_TEXT_COLUMNS = 24;

    /** Approximate number of wrapped help lines reserved below the slider. */
    private static final int HELP_TEXT_ROWS = 8;

    /** Current slice angle, in degrees. */
    private double phiDeg = PHI_INITIAL;

    /** Synthetic 3D model used by this demo view. */
    private final GeometrySliceModel model = GeometrySliceModel.createDefault();

    /**
     * All MDI items created during the most recent slice rebuild.
     *
     * <p>
     * This list is used to remove the old slice before drawing the next one.
     * It contains both shell polygons and wire points.
     * </p>
     */
    private final List<AItem> sliceItems = new ArrayList<>();

    /**
     * Shell polygon items created during the most recent rebuild.
     *
     * <p>
     * Kept separately so feedback can identify region hits even when wire
     * points are drawn above the shell.
     * </p>
     */
    private final List<AItem> shellSliceItems = new ArrayList<>();

    /**
     * Wire point items created during the most recent rebuild, with their
     * associated source wire metadata.
     */
    private final List<WireSliceItem> wireSliceItems = new ArrayList<>();

    /**
     * Identity map from rendered shell polygon item back to the 3D shell model
     * object that produced it.
     */
    private final Map<AItem, Shell3D> shellByItem = new IdentityHashMap<>();

    /** Label showing the current phi value above the slider. */
    private JLabel phiLabel;

    /**
     * Construct the demo view.
     *
     * <p>
     * The constructor is private so that the public factory method follows the
     * same pattern as other lazily-created MDI demo views.
     * </p>
     */
    private GeometrySliceDemoView() {
        super(createDefaultProperties());

        addWestPanel(createControlPanel());
        initEastSidePanel();

        // Defer item creation until the container has had a chance to settle.
        javax.swing.SwingUtilities.invokeLater(this::rebuildSlice);
    }

    /**
     * Factory method used by the view manager / lazy-view machinery.
     *
     * @return a new geometry slice demo view
     */
    public static GeometrySliceDemoView create() {
        return new GeometrySliceDemoView();
    }

    /**
     * Create the default construction properties for this view.
     *
     * @return the default view properties
     */
    private static Properties createDefaultProperties() {
        long toolBits = ToolBits.INFO | ToolBits.ZOOMTOOLS;

        return new ViewPropertiesBuilder()
                .fraction(0.7)
                .aspect(1.6)
                .toolbarBits(toolBits)
                .visible(true)
                .wheelZoom(true)
                .background(X11Colors.getX11Color("honey dew"))
                .title(TITLE)

                // Required: without a world system, BaseView will not create a
                // container-backed view.
                .worldSystem(new Rectangle2D.Double(-20.0, -20.0, 720.0, 620.0))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public AbstractViewInfo getViewInfo() {
        return new GeometrySliceViewInfo();
    }

    /**
     * Initialize the east-side feedback panel.
     */
    private void initEastSidePanel() {
        FeedbackPane fbp = initFeedback(
                Color.white,
                X11Colors.getX11Color("dark red"),
                10);

        fbp.setPreferredSize(SwingSizingUtils.preferredSizeAtLeast(
                fbp, SIDE_PANEL_WIDTH, 1));

        add(fbp, BorderLayout.EAST);
    }

    /**
     * Build the west-side control panel containing the phi slider and help text.
     *
     * @return the control panel
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        phiLabel = new JLabel(labelText(), SwingConstants.CENTER);

        JSlider slider = new JSlider(
                SwingConstants.VERTICAL,
                PHI_MIN,
                PHI_MAX,
                PHI_INITIAL);

        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setOpaque(true);
        slider.setBackground(Color.white);
        slider.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        slider.addChangeListener((ChangeEvent e) -> {
            phiDeg = slider.getValue();
            phiLabel.setText(labelText());

            // Rebuild continuously while dragging. If this ever feels too slow,
            // guard with: if (!slider.getValueIsAdjusting()) rebuildSlice();
            rebuildSlice();
        });

        panel.add(phiLabel, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(createHelpText(), BorderLayout.SOUTH);

        panel.setPreferredSize(SwingSizingUtils.preferredSizeAtLeast(panel, 150, 200));

        return panel;
    }

    /**
     * Create the explanatory help text shown below the slider.
     *
     * @return configured help text component
     */
    private JTextArea createHelpText() {
        JTextArea text = new JTextArea(
                "Slider changes the \u03c6 slice angle. The 2D view shows "
              + "z horizontally and radial distance vertically.\n\n"
              + "White shells are 3D region slices. Blue dots are wires. "
              + "Red dots are fake hits.");

        text.setBackground(Color.white);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(true);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Fonts.smallFont);

        // Columns are measured using the installed font. Unlike embedded line
        // breaks or a fixed pixel width, this gives the parent panel a useful
        // scale-aware preferred width before BorderLayout performs wrapping.
        text.setColumns(HELP_TEXT_COLUMNS);
        text.setRows(HELP_TEXT_ROWS);

        text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        return text;
    }

    /**
     * Return the label text for the current phi slider value.
     *
     * @return phi label text
     */
    private String labelText() {
        return String.format("\u03c6 = %.0f\u00b0", phiDeg);
    }

    /**
     * Recompute the current 2D slice and redraw it as ordinary MDI items.
     */
    private void rebuildSlice() {
        Layer wireLayer = getDefaultLayer();
        if (wireLayer == null) {
            return;
        }

        clearSliceItems();
        drawShellSlices();

        Plane plane = Plane.constantPhiPlane(phiDeg);

        for (Wire3D wire : model.getWires()) {
            edu.cnu.mdi.geometry.Point hit3D =
                    new edu.cnu.mdi.geometry.Point();

            double t = plane.lineIntersection(wire.getLine(), hit3D);

            if (java.lang.Double.isNaN(t) || t < 0.0 || t > 1.0) {
                continue;
            }

            Point2D.Double wp = SliceProjection.projectToSlice(hit3D, phiDeg);

            boolean fakeHit = wire.isFakeHit();

            PointItem item = new PointItem(wireLayer, wp,
                    PropertyUtils.LOCKED, true,
                    PropertyUtils.SYMBOL, SymbolType.CIRCLE,
                    PropertyUtils.SYMBOLSIZE, fakeHit ? 6 : 4,
                    PropertyUtils.LINECOLOR,
                    fakeHit ? Color.RED : new Color(40, 120, 220),
                    PropertyUtils.FILLCOLOR,
                    fakeHit ? Color.RED : new Color(40, 120, 220),
                    PropertyUtils.TITLE, wire.toString());

            sliceItems.add(item);
            wireSliceItems.add(new WireSliceItem(item, wire));
        }

        refresh();
    }

    /**
     * Draw the current phi-slice of each synthetic 3D shell.
     *
     * <p>
     * Shell slice polygons are placed on the connection layer so they draw
     * beneath the wire points. They are also tracked separately for region
     * feedback and nested wire hit-testing.
     * </p>
     */
    private void drawShellSlices() {
        Layer shellLayer = getConnectionLayer();
        if (shellLayer == null) {
            shellLayer = getDefaultLayer();
        }
        if (shellLayer == null) {
            return;
        }

        for (Shell3D shell : model.getShells()) {
            Point2D.Double[] poly = shell.slicePolygon(phiDeg);
            if (poly == null) {
                continue;
            }

            PolygonItem item = new PolygonItem(shellLayer, poly,
                    PropertyUtils.LOCKED, true,
                    PropertyUtils.FILLCOLOR, Color.WHITE,
                    PropertyUtils.LINECOLOR, Color.BLACK,
                    PropertyUtils.LINEWIDTH, 1.5f,
                    PropertyUtils.TITLE, shell.label());

            sliceItems.add(item);
            shellSliceItems.add(item);
            shellByItem.put(item, shell);
        }
    }

    /**
     * Remove all items created during the previous slice rebuild.
     */
    private void clearSliceItems() {
        for (AItem item : new ArrayList<>(sliceItems)) {
            Layer layer = item.getLayer();
            if (layer != null) {
                layer.remove(item);
            }
        }

        sliceItems.clear();
        shellSliceItems.clear();
        shellByItem.clear();
        wireSliceItems.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void getFeedbackStrings(IContainer container, Point pp,
            Point2D.Double wp, List<String> feedbackStrings) {

        if (wp == null || feedbackStrings == null) {
            return;
        }

        edu.cnu.mdi.geometry.Point p3 =
                SliceProjection.sliceToCartesian(wp, phiDeg);

        double r = SliceProjection.radius(p3);
        double theta = SliceProjection.thetaDeg(p3);
        double phi = SliceProjection.phiDeg(p3);

        feedbackStrings.add(String.format(
                "slice: z = %-10.4f   s = %-10.4f",
                wp.x, wp.y));

        feedbackStrings.add(String.format(
                "cartesian: x = %-10.4f   y = %-10.4f   z = %-10.4f",
                p3.x, p3.y, p3.z));

        feedbackStrings.add(String.format(
                "spherical: r = %-10.4f   \u03b8 = %-8.3f\u00b0   \u03c6 = %-8.3f\u00b0",
                r, theta, phi));

        Shell3D shell = shellHit(container, pp);
        if (shell != null) {
            feedbackStrings.add("region: " + shell.label());

            WireSliceItem wireHit = wireHit(container, pp, shell);
            if (wireHit != null) {
                Wire3D wire = wireHit.getWire();
                feedbackStrings.add("wire: " + wire);

                if (wire.isFakeHit()) {
                    feedbackStrings.add(String.format(
                            "ADC = %d   TDC = %d",
                            wire.getAdc(), wire.getTdc()));
                }
            }
        }
    }

    /**
     * Return the shell slice under the mouse, if any.
     *
     * <p>
     * This deliberately uses {@link IContainer#getItemsAtPoint(Point)} rather
     * than {@code getItemAtPoint}. Wire points live on the default layer and
     * draw above the shells, while the shells live on the connection layer
     * below them. Asking for all items lets us still discover the containing
     * shell even when the mouse is also over a wire point.
     * </p>
     *
     * @param container the active container
     * @param pp mouse position in local/screen coordinates
     * @return the hit shell, or {@code null}
     */
    private Shell3D shellHit(IContainer container, Point pp) {
        if (container == null || pp == null || shellSliceItems.isEmpty()) {
            return null;
        }

        ArrayList<AItem> items = container.getItemsAtPoint(pp);
        if (items == null || items.isEmpty()) {
            return null;
        }

        for (AItem item : items) {
            Shell3D shell = shellByItem.get(item);
            if (shell != null) {
                return shell;
            }
        }

        return null;
    }

    /**
     * Return the wire point under the mouse, restricted to wires belonging to
     * the already-hit shell.
     *
     * <p>
     * This is intentionally a second-stage hit test. The view first identifies
     * the containing shell/region, then only tests wires from that region. This
     * mirrors the way a real detector display can narrow picking from a large
     * global set to the small subset of objects inside the active detector
     * region.
     * </p>
     *
     * @param container the active container
     * @param pp mouse position in local/screen coordinates
     * @param shell the shell already identified under the mouse
     * @return the hit wire slice item, or {@code null}
     */
    private WireSliceItem wireHit(IContainer container, Point pp, Shell3D shell) {
        if (container == null || pp == null || shell == null) {
            return null;
        }

        for (WireSliceItem wsi : wireSliceItems) {
            Wire3D wire = wsi.getWire();

            if (wire.getChamberIndex() != shell.getRegionIndex()) {
                continue;
            }

            if (wsi.getItem().contains(container, pp)) {
                return wsi;
            }
        }

        return null;
    }
}
