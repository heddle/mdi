package edu.cnu.mdi.graphics.style;

import java.awt.BasicStroke;
import java.awt.Color;

import edu.cnu.mdi.graphics.GraphicsUtils;

public interface IStyled {

	/**
	 * Get the color used for fill the interior area.
	 *
	 * @return the fill color.
	 */
    Color getFillColor();

	/**
	 * Set the color used for fill the interior area.
	 *
	 * @param fillColor the fill color.
	 */
    void setFillColor(Color fillColor);

	/**
	 * Get the color used for symbol borders.
	 *
	 * @return the symbol border color.
	 */
    Color getBorderColor();

	/**
	 * Set the color used for symbol borders.
	 *
	 * @param borderColor the border color.
	 */
    void setBorderColor(Color borderColor);

	/**
	 * Set the color used for text.
	 *
	 * @param textColor the text color.
	 */
    void setTextColor(Color textColor);

	/**
	 * Get the color used for text.
	 *
	 * @return the text color.
	 */
    Color getTextColor();

	/**
	 * Get the color used for the primary line (e.g. an item's outline, a
	 * curve's fit line, or a connector's stroke, depending on the
	 * implementer).
	 *
	 * @return the line color.
	 */

    Color getLineColor();

	/**
	 * Get the color used for auxiliary lines.
	 *
	 * @return the auxiliary line color.
	 */

    Color getAuxLineColor();

	/**
	 * Set the color used for the primary line (see {@link #getLineColor}).
	 *
	 * @param lineColor the line color.
	 */
    void setLineColor(Color lineColor);

	/**
	 * Set the color used for auxiliary lines.
	 *
	 * @param auxColor the auxiliary line color.
	 */
    void setAuxLineColor(Color auxColor);

	/**
	 * Get the style used for drawing the primary line (see
	 * {@link #getLineColor}).
	 *
	 * @return the primary line style.
	 */
    LineStyle getLineStyle();

	/**
	 * Get the style used for drawing auxiliary lines.
	 *
	 * @return the auxiliary line style.
	 */
    LineStyle getAuxLineStyle();

	/**
	 * Set the style used for drawing the primary line (see
	 * {@link #setLineColor}).
	 *
	 * @param lineStyle the primary line style.
	 */
    void setLineStyle(LineStyle lineStyle);

	/**
	 * Set the style used for drawing auxiliary lines.
	 *
	 * @param lineStyle the auxiliary line style.
	 */
    void setAuxLineStyle(LineStyle lineStyle);

	/**
	 * Get the symbol used for drawing points.
	 *
	 * @return the symbol used for drawing points.
	 */
    SymbolType getSymbolType();

	/**
	 * Set the symbol used for drawing points.
	 *
	 * @param symbolType the symbol used for drawing points.
	 */
    void setSymbolType(SymbolType symbolType);

	/**
	 * Get the line width for drawing the primary line.
	 *
	 * @return the primary line width in pixels.
	 */
    float getLineWidth();

	/**
	 * Get the line width for drawing auxiliary lines.
	 *
	 * @return the auxiliary line width in pixels.
	 */
    float getAuxLineWidth();

	/**
	 * Set the line width for drawing the primary line.
	 *
	 * @param lineWidth the line width in pixels.
	 */
    void setLineWidth(float lineWidth);

	/**
	 * Set the line width for drawing auxiliary lines.
	 *
	 * @param lineWidth the auxiliary line width in pixels.
	 */
    void setAuxLineWidth(float lineWidth);

	/**
	 * Get the symbol size (full width) in pixels.
	 *
	 * @return the symbol size (full width) in pixels.
	 */
    int getSymbolSize();

	/**
	 * Set symbol size (full width) in pixels.
	 *
	 * @param symbolSize symbol size (full width) in pixels.
	 */
    void setSymbolSize(int symbolSize);

	/**
	 * Get the stroke derived from the primary line width and style.
	 *
	 * @return the stroke used for drawing the primary line.
	 */
	default BasicStroke getStroke() {
		return GraphicsUtils.getStroke(getLineWidth(), getLineStyle());
	}
}
