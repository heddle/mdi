package edu.cnu.mdi.item;

import java.awt.Font;

import edu.cnu.mdi.graphics.style.IStyled;

/**
 * Marks an item as having editable text content, font, and style — the minimal
 * contract required by {@link edu.cnu.mdi.dialog.TextEditDialog}.
 *
 * <h2>Purpose</h2>
 * <p>
 * {@code TextEditDialog} was originally written to accept a concrete
 * {@link TextItem}. Introducing this interface breaks that dependency so the
 * dialog can operate on any text-bearing item — in particular both
 * {@link TextItem} (world-space anchor) and
 * {@link edu.cnu.mdi.mapping.item.MapTextItem} (geographic anchor) — without
 * knowing about either class.
 * </p>
 *
 * <h2>Implementing classes</h2>
 * <p>
 * Any item that can be edited by {@code TextEditDialog} should implement this
 * interface. Typically that means declaring {@code implements ITextEditable} on
 * the class; the six methods are usually already present.
 * </p>
 *
 * <h2>Method contract</h2>
 * <ul>
 *   <li>{@link #getText()} / {@link #setText(String)} — the full text content
 *       as a single string with embedded newlines for multi-line text.</li>
 *   <li>{@link #getFont()} / {@link #setFont(Font)} — the display font. Passing
 *       {@code null} to {@code setFont} should restore a sensible default.</li>
 *   <li>{@link #getStyle()} / {@link #setStyle(IStyled)} — the fill, line, and
 *       text colors (and any other style attributes) for this item.</li>
 * </ul>
 */
public interface ITextEditable {

    /**
     * Returns the current text content.
     *
     * <p>Multi-line text is represented with embedded {@code '\n'} characters.
     * Implementations must never return {@code null}; an empty string is the
     * correct representation of no content.</p>
     *
     * @return the text; never {@code null}
     */
    String getText();

    /**
     * Replaces the text content.
     *
     * @param text the new text; implementations should treat {@code null} as an
     *             empty string
     */
    void setText(String text);

    /**
     * Returns the current display font.
     *
     * @return the font; must not be {@code null} in a correctly initialised item
     */
    Font getFont();

    /**
     * Sets the display font.
     *
     * @param font the new font; passing {@code null} should restore the
     *             implementation's default font
     */
    void setFont(Font font);

    /**
     * Returns the current style (colors, line style, etc.).
     *
     * @return the style; may be {@code null} on a partially constructed or
     *         disposed item, but must not be {@code null} during normal use
     */
    IStyled getStyle();

    /**
     * Replaces the current style.
     *
     * @param style the new style; {@code null} is accepted but may cause
     *              rendering errors unless {@code getStyleSafe()} is used
     *              subsequently
     */
    void setStyle(IStyled style);
}