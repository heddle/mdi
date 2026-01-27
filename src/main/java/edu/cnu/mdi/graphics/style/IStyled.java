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
	 * Get the color used for fits.
	 *
	 * @return the fit line color.
	 */

    Color getLineColor();

	/**
	 * Get the color used for auxiliary lines.
	 *
	 * @return the auxiliary line color.
	 */

    Color getAuxLineColor();

	/**
	 * Set the color used for the fit drawing.
	 *
	 * @param fitColor the fit color.
	 */
    void setLineColor(Color fitColor);

	/**
	 * Set the color used for auxiliary lines.
	 *
	 * @param auxColor the auxiliary line color.
	 */
    void setAuxLineColor(Color auxColor);

	/**
	 * Get the style used for drawing fits.
	 *
	 * @return the line style for fits.
	 */
    LineStyle getLineStyle();

	/**
	 * Get the style used for drawing fits.
	 *
	 * @return the line style for fits.
	 */
    LineStyle getAuxLineStyle();

	/**
	 * Set the style used for drawing fits.
	 *
	 * @param lineStyle the fit line style.
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
	 * Get the line width for drawing fits.
	 *
	 * @return the fit line width in pixels.
	 */
    float getLineWidth();

	/**
	 * Get the line width for drawing auxiliary lines.
	 *
	 * @return the auxiliary line width in pixels.
	 */
    float getAuxLineWidth();

	/**
	 * Set the line width for drawing fit lines.
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
	 * Get the stroke used for drawing fits.
	 *
	 * @return the stroke used for drawing fits.
	 */
	default BasicStroke getStroke() {
		return GraphicsUtils.getStroke(getLineWidth(), getLineStyle());
	}
}
