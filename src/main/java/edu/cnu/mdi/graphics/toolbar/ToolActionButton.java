package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import edu.cnu.mdi.graphics.ImageManager;

/**
 * A non-toggle toolbar button that performs an immediate action (one-shot).
 * <p>
 * This is the replacement for legacy {@code ToolBarButton} that stored an
 * {@code IContainer}. In the new toolbar framework, actions should be
 * container-agnostic and operate via {@link ToolContext}.
 * </p>
 *
 * <p>
 * Unlike {@link ToolToggleButton}, this class does not change the active tool.
 * It just runs {@link #perform(ToolContext)} when clicked.
 * </p>
 *
 * @author heddle
 */
@SuppressWarnings("serial")
public abstract class ToolActionButton extends JButton {

    /** Preferred size for toolbar buttons. */
    private static final Dimension PREFERRED_SIZE = new Dimension(24, 24);

    private final ToolContext ctx;

    /**
     * Create a new action button.
     *
     * @param ctx          tool context (must not be null).
     * @param imageFile    image resource path for the icon.
     * @param toolTip      tooltip text.
     */
    public ToolActionButton(ToolContext ctx, String imageFile, String toolTip) {
        this.ctx = java.util.Objects.requireNonNull(ctx, "ctx");
        setFocusPainted(false);
        setBorderPainted(false);
        setToolTipText(toolTip);

        ImageIcon icon = ImageManager.getInstance().loadImageIcon(imageFile);
        setIcon(icon);

        addActionListener(e -> perform(this.ctx));
    }

    /**
     * Perform the button's action.
     *
     * @param ctx tool context (never null).
     */
    protected abstract void perform(ToolContext ctx);

    /**
     * Cursor to show while hovering this button (normally default).
     *
     * @return the cursor.
     */
    public Cursor canvasCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    @Override
    public Dimension getPreferredSize() {
        return PREFERRED_SIZE;
    }

    @Override
    public Dimension getMinimumSize() {
        return PREFERRED_SIZE;
    }

    @Override
    public Dimension getMaximumSize() {
        return PREFERRED_SIZE;
    }
}
