package edu.cnu.mdi.graphics.toolbar;

import java.util.List;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.ui.StyleEditorDialog;
import edu.cnu.mdi.item.AItem;

public class EditStyleButton extends ToolActionButton {

    public EditStyleButton(ToolContext ctx) {
        super(ctx, "images/colorwheel.png", "Edit style of selected items");
	}

	@Override
	protected void perform(ToolContext ctx) {
		var c = ctx.container();
		
		List<AItem> selected = c.getSelectedItems(); // if you don’t have this, use
															// getAnnotationList().getSelectedItems() + other lists

		if (selected == null || selected.isEmpty()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}

		// seed from first selected item
		IStyled seed = selected.get(0).getStyleSafe();
		Styled edited = StyleEditorDialog.edit(c.getComponent(), seed, false);
		if (edited == null)
			return;

		for (AItem item : selected) {
			item.setStyle(edited.copy()); // avoid shared mutable style objects
			item.setDirty(true);
		}

		c.refresh();
	}
}
