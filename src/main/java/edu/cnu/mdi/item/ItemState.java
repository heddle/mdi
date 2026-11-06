package edu.cnu.mdi.item;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Objects;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;

/**
 * An immutable snapshot of the editable state of an {@link AItem}: where it is
 * (focus, path or line), how it is oriented, how it is styled, and any
 * item-specific extras. Undo/redo stores one snapshot before and one after each
 * edit and puts the item back by handing a snapshot to
 * {@link AItem#restoreState(ItemState)}.
 *
 * <p>All mutable parts are copied on the way in and on the way out, so a
 * snapshot is unaffected by what happens to the item afterwards.</p>
 *
 * <p>The {@code extra} value carries state specific to an item class, such as
 * an ellipse's width and height. It must be immutable and have value-based
 * {@code equals}, for example a {@code record} of immutable parts, because
 * {@link #sameAs(ItemState)} uses it to tell whether an edit changed anything.</p>
 */
public final class ItemState {

	private final Point2D.Double focus;
	private final double azimuth;
	private final Path2D.Double path;
	private final Line2D.Double line;
	private final Styled style;
	private final Object extra;

	/**
	 * Create a snapshot. The geometry and style arguments are copied.
	 *
	 * @param focus   the item's focus, or {@code null}
	 * @param azimuth the item's azimuth in degrees
	 * @param path    the item's path, or {@code null}
	 * @param line    the item's line, or {@code null}
	 * @param style   the item's style, or {@code null} if it is not captured
	 * @param extra   item-specific immutable state with value equality, or
	 *                {@code null}
	 */
	public ItemState(Point2D.Double focus, double azimuth, Path2D.Double path, Line2D.Double line, IStyled style,
			Object extra) {
		this.focus = (focus == null) ? null : new Point2D.Double(focus.x, focus.y);
		this.azimuth = azimuth;
		this.path = (path == null) ? null : (Path2D.Double) path.clone();
		this.line = (line == null) ? null : (Line2D.Double) line.clone();
		this.style = (style == null) ? null : new Styled(style);
		this.extra = extra;
	}

	/** @return a copy of the focus, or {@code null} */
	public Point2D.Double focus() {
		return (focus == null) ? null : new Point2D.Double(focus.x, focus.y);
	}

	/** @return the azimuth in degrees */
	public double azimuth() {
		return azimuth;
	}

	/** @return a copy of the path, or {@code null} */
	public Path2D.Double path() {
		return (path == null) ? null : (Path2D.Double) path.clone();
	}

	/** @return a copy of the line, or {@code null} */
	public Line2D.Double line() {
		return (line == null) ? null : (Line2D.Double) line.clone();
	}

	/** @return a copy of the captured style, or {@code null} if none was captured */
	public Styled style() {
		return (style == null) ? null : new Styled(style);
	}

	/** @return the item-specific extra state, or {@code null} */
	public Object extra() {
		return extra;
	}

	/**
	 * Tests whether two snapshots describe exactly the same state. Used to avoid
	 * recording an edit that changed nothing, such as a drag that was cancelled
	 * or rejected.
	 *
	 * @param other the snapshot to compare with; may be {@code null}
	 * @return {@code true} if every captured part is identical
	 */
	public boolean sameAs(ItemState other) {
		if (other == null) {
			return false;
		}
		return same(focus, other.focus) && Double.compare(azimuth, other.azimuth) == 0
				&& samePath(path, other.path) && same(line, other.line) && sameStyle(style, other.style)
				&& Objects.equals(extra, other.extra);
	}

	/** Tests whether a live style is identical to the style captured in this snapshot. */
	boolean hasStyle(IStyled live) {
		return sameStyle(style, live);
	}

	private static boolean same(Point2D.Double a, Point2D.Double b) {
		if (a == null || b == null) {
			return a == b;
		}
		return Double.compare(a.x, b.x) == 0 && Double.compare(a.y, b.y) == 0;
	}

	private static boolean same(Line2D.Double a, Line2D.Double b) {
		if (a == null || b == null) {
			return a == b;
		}
		return Double.compare(a.x1, b.x1) == 0 && Double.compare(a.y1, b.y1) == 0
				&& Double.compare(a.x2, b.x2) == 0 && Double.compare(a.y2, b.y2) == 0;
	}

	private static boolean samePath(Path2D.Double a, Path2D.Double b) {
		if (a == null || b == null) {
			return a == b;
		}
		if (a.getWindingRule() != b.getWindingRule()) {
			return false;
		}
		PathIterator ia = a.getPathIterator(null);
		PathIterator ib = b.getPathIterator(null);
		double[] ca = new double[6];
		double[] cb = new double[6];
		while (!ia.isDone() && !ib.isDone()) {
			int ta = ia.currentSegment(ca);
			int tb = ib.currentSegment(cb);
			if (ta != tb) {
				return false;
			}
			int n = switch (ta) {
			case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 2;
			case PathIterator.SEG_QUADTO -> 4;
			case PathIterator.SEG_CUBICTO -> 6;
			default -> 0;
			};
			for (int i = 0; i < n; i++) {
				if (Double.compare(ca[i], cb[i]) != 0) {
					return false;
				}
			}
			ia.next();
			ib.next();
		}
		return ia.isDone() && ib.isDone();
	}

	private static boolean sameStyle(IStyled a, IStyled b) {
		if (a == null || b == null) {
			return a == b;
		}
		return Objects.equals(a.getFillColor(), b.getFillColor())
				&& Objects.equals(a.getBorderColor(), b.getBorderColor())
				&& Objects.equals(a.getTextColor(), b.getTextColor())
				&& Objects.equals(a.getLineColor(), b.getLineColor())
				&& Objects.equals(a.getAuxLineColor(), b.getAuxLineColor())
				&& a.getLineStyle() == b.getLineStyle() && a.getAuxLineStyle() == b.getAuxLineStyle()
				&& a.getSymbolType() == b.getSymbolType()
				&& Float.compare(a.getLineWidth(), b.getLineWidth()) == 0
				&& Float.compare(a.getAuxLineWidth(), b.getAuxLineWidth()) == 0
				&& a.getSymbolSize() == b.getSymbolSize();
	}
}
