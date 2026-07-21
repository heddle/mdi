package edu.cnu.mdi.view.demo.geoslice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.cnu.mdi.geometry.Line;

/**
 * Synthetic three-dimensional model used by the geometry slice demo views.
 *
 * <p>
 * This class owns the demo model, not the rendered MDI or MDI-3D items. It
 * creates a small detector-like geometry consisting of three region shells and
 * many wire-like 3D line segments. A 2D view can slice this model and render
 * the intersections as ordinary MDI items; a 3D view can render the same source
 * model directly as lines, wireframes, and a slice plane.
 * </p>
 *
 * <p>
 * The model is intentionally simple and pedagogical. It is inspired by detector
 * displays, but it is not intended to represent any particular detector exactly.
 * </p>
 */
public final class GeometrySliceModel {

    /** Number of synthetic detector regions. */
    public static final int NUM_CHAMBERS = 3;

    /** Number of wire layers in each region. */
    public static final int NUM_LAYERS = 6;

    /** Number of wires in each layer. */
    public static final int WIRES_PER_LAYER = 36;

    /** Fixed random seed so the fake electronics values are reproducible. */
    private static final long RANDOM_SEED = 20260611L;

    /** Synthetic 3D wire collection. */
    private final List<Wire3D> wires = new ArrayList<>();

    /** Synthetic 3D shell collection. */
    private final List<Shell3D> shells = new ArrayList<>();

    /**
     * Create an empty model.
     *
     * <p>
     * Use {@link #createDefault()} for the normal demo model.
     * </p>
     */
    private GeometrySliceModel() {
    }

    /**
     * Create the standard synthetic model used by the demo views.
     *
     * @return populated default model
     */
    public static GeometrySliceModel createDefault() {
        GeometrySliceModel model = new GeometrySliceModel();
        model.build();
        return model;
    }

    /**
     * Return the synthetic wires.
     *
     * @return immutable view of the model wires
     */
    public List<Wire3D> getWires() {
        return Collections.unmodifiableList(wires);
    }

    /**
     * Return the synthetic region shells.
     *
     * @return immutable view of the model shells
     */
    public List<Shell3D> getShells() {
        return Collections.unmodifiableList(shells);
    }

    /**
     * Build the synthetic 3D model.
     *
     * <p>
     * The model has three chamber-like regions. Each region has six layers, and
     * each layer has a fixed number of wire-like line segments spanning the
     * sector in azimuth. Alternate layers are given a small z shift across the
     * azimuthal span so that the intersection pattern visibly changes as the phi
     * slider moves.
     * </p>
     */
    private void build() {
        wires.clear();
        shells.clear();

        Random random = new Random(RANDOM_SEED);
        int wireId = 0;

        for (int chamber = 0; chamber < NUM_CHAMBERS; chamber++) {
            double chamberR0 = 80.0 + chamber * 150.0;
            double chamberZ0 = 80.0 + chamber * 150.0;

            shells.add(Shell3D.forChamber(chamber, chamberR0, chamberZ0));

            for (int layer = 0; layer < NUM_LAYERS; layer++) {
                double layerOffset = layer * 7.0;

                for (int wire = 0; wire < WIRES_PER_LAYER; wire++) {
                    double f = wire / (double) (WIRES_PER_LAYER - 1);

                    double z = chamberZ0 + 260.0 * f;
                    double r = chamberR0 + 0.30 * (z - chamberZ0) + layerOffset;

                    double zShift = (layer % 2 == 0) ? -8.0 : 8.0;

                    edu.cnu.mdi.geometry.Point p1 =
                            SliceProjection.cylindrical(r, -30.0, z - zShift);

                    edu.cnu.mdi.geometry.Point p2 =
                            SliceProjection.cylindrical(r,  30.0, z + zShift);

                    boolean fakeHit = isFakeHit(chamber, layer, wire);
                    int adc = 1 + random.nextInt(99_999);
                    int tdc = 1 + random.nextInt(99_999);

                    wires.add(new Wire3D(
                            new Line(p1, p2),
                            chamber,
                            layer,
                            wire,
                            wireId++,
                            fakeHit,
                            adc,
                            tdc));
                }
            }
        }
    }

    /**
     * Return whether the given synthetic wire should be shown as a fake hit.
     *
     * <p>
     * The pattern is arbitrary. It creates a short diagonal run of red points in
     * region 2 so the feedback panel can demonstrate ADC/TDC-style readout.
     * </p>
     *
     * @param chamber zero-based region/chamber index
     * @param layer zero-based layer index
     * @param wire zero-based wire-in-layer index
     * @return {@code true} if this wire is a fake hit
     */
    private static boolean isFakeHit(int chamber, int layer, int wire) {
        if (chamber != 1) {
            return false;
        }
        if (layer < 1 || layer > 4) {
            return false;
        }
        return Math.abs(wire - (10 + 4 * layer)) <= 1;
    }
}
