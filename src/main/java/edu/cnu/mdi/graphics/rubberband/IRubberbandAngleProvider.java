package edu.cnu.mdi.graphics.rubberband;

/**
 * Optional interface for rubberbands that can provide a meaningful angle
 * (e.g. RADARC sweep) that cannot be reconstructed from vertices alone.
 */
public interface IRubberbandAngleProvider {
    /**
     * @return signed, unwrapped sweep angle in degrees (may exceed 180 in magnitude)
     */
    double getRubberbandAngleDeg();
}
