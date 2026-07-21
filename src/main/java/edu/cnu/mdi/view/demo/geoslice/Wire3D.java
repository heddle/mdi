package edu.cnu.mdi.view.demo.geoslice;

import edu.cnu.mdi.geometry.Line;

/**
 * One synthetic three-dimensional wire in the geometry slice demos.
 *
 * <p>
 * A {@code Wire3D} is represented by a finite 3D line segment. The 2D demo view
 * intersects that segment with the current constant-phi slicing plane and
 * renders the intersection point. The companion 3D demo view renders the line
 * segment directly so readers can see the source object being sliced.
 * </p>
 *
 * <p>
 * The class also carries simple detector-style metadata: region, layer, wire
 * number, fake-hit status, and stable fake ADC/TDC values. These values are
 * used only for feedback and visual emphasis in the demos.
 * </p>
 */
public final class Wire3D {

    /** Finite 3D line segment representing the wire. */
    private final Line line;

    /** Zero-based chamber/region index. */
    private final int chamberIndex;

    /** Zero-based layer index. */
    private final int layerIndex;

    /** Zero-based wire index within the layer. */
    private final int wireIndex;

    /** Unique wire id across the whole synthetic model. */
    private final int globalWireId;

    /** Whether this wire should be displayed as a fake hit. */
    private final boolean fakeHit;

    /** Stable fake ADC value for feedback. */
    private final int adc;

    /** Stable fake TDC value for feedback. */
    private final int tdc;

    /**
     * Create a synthetic wire.
     *
     * @param line finite 3D line segment
     * @param chamberIndex zero-based chamber/region index
     * @param layerIndex zero-based layer index
     * @param wireIndex zero-based wire index within the layer
     * @param globalWireId unique wire id across the model
     * @param fakeHit whether this wire is a fake hit
     * @param adc fake ADC value
     * @param tdc fake TDC value
     */
    public Wire3D(Line line, int chamberIndex, int layerIndex, int wireIndex,
            int globalWireId, boolean fakeHit, int adc, int tdc) {
        this.line = line;
        this.chamberIndex = chamberIndex;
        this.layerIndex = layerIndex;
        this.wireIndex = wireIndex;
        this.globalWireId = globalWireId;
        this.fakeHit = fakeHit;
        this.adc = adc;
        this.tdc = tdc;
    }

    /**
     * Return the 3D line segment representing this wire.
     *
     * @return wire line segment
     */
    public Line getLine() {
        return line;
    }

    /**
     * Return the zero-based chamber/region index.
     *
     * @return chamber index
     */
    public int getChamberIndex() {
        return chamberIndex;
    }

    /**
     * Return the zero-based layer index.
     *
     * @return layer index
     */
    public int getLayerIndex() {
        return layerIndex;
    }

    /**
     * Return the zero-based wire index within the layer.
     *
     * @return wire index
     */
    public int getWireIndex() {
        return wireIndex;
    }

    /**
     * Return the unique wire id across the synthetic model.
     *
     * @return global wire id
     */
    public int getGlobalWireId() {
        return globalWireId;
    }

    /**
     * Return whether this wire is a fake hit.
     *
     * @return {@code true} if fake hit
     */
    public boolean isFakeHit() {
        return fakeHit;
    }

    /**
     * Return the stable fake ADC value.
     *
     * @return ADC value
     */
    public int getAdc() {
        return adc;
    }

    /**
     * Return the stable fake TDC value.
     *
     * @return TDC value
     */
    public int getTdc() {
        return tdc;
    }

    /**
     * Return a compact feedback label.
     *
     * @return label such as {@code "region 2, layer 3, wire 14"}
     */
    @Override
    public String toString() {
        return String.format("region %d, layer %d, wire %d",
                chamberIndex + 1, layerIndex + 1, wireIndex + 1);
    }
}
