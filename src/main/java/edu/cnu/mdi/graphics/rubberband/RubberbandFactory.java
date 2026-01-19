package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.util.Objects;

public final class RubberbandFactory {

	private RubberbandFactory() {}

	public static ARubberband create(Component component,
	                                IRubberbanded rubberbanded,
	                                ARubberband.Policy policy) {

		Objects.requireNonNull(component, "component");
		Objects.requireNonNull(rubberbanded, "rubberbanded");
		Objects.requireNonNull(policy, "policy");

		switch (policy) {
		case RECTANGLE:
			return new RubberRect(component, rubberbanded);

		case OVAL:
			return new RubberOval(component, rubberbanded);

		case RECTANGLE_PRESERVE_ASPECT:
			return new RubberRectPreserveAspect(component, rubberbanded);

		case XONLY:
			return new RubberXOnly(component, rubberbanded);

		case YONLY:
			return new RubberYOnly(component, rubberbanded);

		case LINE:
			return new RubberLine(component, rubberbanded);

		case TWO_CLICK_LINE:
			return new RubberTwoClickLine(component, rubberbanded);

		case POLYGON:
			return new RubberPolygon(component, rubberbanded);

		case POLYLINE:
			return new RubberPolyline(component, rubberbanded);

		case RADARC:
			return new RubberRadArc(component, rubberbanded);

		case NONE:
		default:
			return null;
		}
	}
}
