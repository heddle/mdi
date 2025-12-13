package edu.cnu.mdi.graphics.toolbar;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import edu.cnu.mdi.dialog.LabelDialog;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.item.TextItem;

/**
 * Tool that creates an annotated {@link TextItem} on the container canvas.
 * <p>
 * Legacy behavior (from {@code TextButton}):
 * </p>
 * <ul>
 *   <li>On mouse click, shows a {@link LabelDialog} to collect text + font + colors.</li>
 *   <li>Applies {@link UnicodeSupport#specialCharReplace(String)} to the entered text.</li>
 *   <li>If non-empty, creates a {@link TextItem} at the clicked world coordinate
 *       in the container's annotation list.</li>
 *   <li>Configures the item as draggable/rotatable/resizable/deletable/right-clickable.</li>
 *   <li>Clears selection, resets to default tool, and refreshes.</li>
 * </ul>
 *
 * <p>
 * This tool intentionally does not depend on toolbar button classes; it uses
 * {@link ToolContext} to access the container and to reset to the default tool.
 * </p>
 *
 * @author heddle
 */
public class TextTool implements ITool {

    /** Tool id used for registration/selection. */
    public static final String ID = "text";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String toolTip() {
        return "Use to annotate";
    }

    /**
     * Show the label dialog and, if valid text/font are chosen, create a {@link TextItem}
     * at the clicked world point.
     */
    @Override
    public void mouseClicked(ToolContext ctx, MouseEvent mouseEvent) {
        if (ctx == null || mouseEvent == null || ctx.container() == null) {
            return;
        }

        LabelDialog labelDialog = new LabelDialog();
        GraphicsUtils.centerComponent(labelDialog);
        labelDialog.setVisible(true);

        String resultString = UnicodeSupport.specialCharReplace(labelDialog.getText());
        if (resultString == null || resultString.isEmpty()) {
            // Even if user cancels/empties, we still fall through to legacy "cleanup".
            cleanup(ctx);
            return;
        }

        Font font = labelDialog.getSelectedFont();
        if (font == null) {
            cleanup(ctx);
            return;
        }

        Point2D.Double wp = new Point2D.Double();
        ctx.container().localToWorld(mouseEvent.getPoint(), wp);

        TextItem item = new TextItem(
                ctx.container().getAnnotationList(),
                wp,
                font,
                resultString,
                labelDialog.getTextForeground(),
                labelDialog.getTextBackground(),
                null
        );

        if (item != null) {
            item.setDraggable(true);
            item.setRotatable(true);
            item.setResizable(true);
            item.setDeletable(true);
            item.setLocked(false);
            item.setRightClickable(true);
        }

        cleanup(ctx);
    }

    /**
     * Common post-action behavior: clear selection, return to default tool, refresh.
     */
    private void cleanup(ToolContext ctx) {
        ctx.container().selectAllItems(false);
        ctx.resetToDefaultTool();
        ctx.container().refresh();
    }
}
