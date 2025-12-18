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
public abstract class ToolActionButton extends JButton implements ToolContextAware {

    /** Preferred size for toolbar buttons. */
    private static final Dimension PREFERRED_SIZE = new Dimension(24, 24);

    private ToolContext ctx;

    /**
     * Create a new action button.
     *
     * @param ctx          tool context (must not be null).
     * @param imageFile    image resource path for the icon.
     * @param toolTip      tooltip text.
     */
    public ToolActionButton(ToolContext ctx, String imageFile, String toolTip) {
        this(imageFile, toolTip);
        this.ctx = java.util.Objects.requireNonNull(ctx, "ctx");
    }
    
    protected ToolActionButton(String imageFile, String toolTip) {
        setFocusPainted(false);
        setBorderPainted(false);
        setToolTipText(toolTip);

        ImageIcon icon = ImageManager.getInstance().loadImageIcon(imageFile, 16, 16);
        setIcon(icon);

        // Works with injection: ctx will be set by BaseToolBar before the user can click.
        addActionListener(e -> {
            if (ctx != null) {
                perform(ctx);
            }
        });
    }
    
    @Override
    public final void setToolContext(ToolContext toolContext) {
        this.ctx = toolContext;
        onToolContextAvailable(); // hook for subclasses
    }

    protected void onToolContextAvailable() {
        // default no-op
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
