package edu.cnu.mdi.graphics.rubberband;

import java.awt.Component;
import java.awt.Point;

public class RubberTwoClickLine extends RubberLine {

	public RubberTwoClickLine(Component component, IRubberbanded rubberbanded) {
		super(component, rubberbanded);
	}

	@Override
	protected boolean approvePoint(Point p, boolean isFirstPoint) {
		return rubberbanded.approvePoint(p);
	}
}
