package edu.cnu.mdi.graphics.toolbar.button;

import java.util.List;

import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.Styled;
import edu.cnu.mdi.graphics.style.ui.StyleEditorDialog;
import edu.cnu.mdi.graphics.toolbar.ToolContext;
import edu.cnu.mdi.item.AItem;

@SuppressWarnings("serial")
public class EditStyleButton extends ToolActionButton {

	public EditStyleButton(ToolContext ctx) {
		super(ctx, "images/svg/colorwheel.svg", "Edit style of selected items");
	}

	@Override
	protected void perform(ToolContext ctx) {
		var container = ctx.container();

		List<AItem> selected = container.getSelectedItems();

		if (selected == null || selected.isEmpty()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}

		// seed from first selected item
		IStyled seed = selected.get(0).getStyleSafe();
		Styled edited = StyleEditorDialog.edit(container.getComponent(), seed, false);
		if (edited == null) {
			return;
		}

		for (AItem item : selected) {
			item.setStyle(edited.copy()); // avoid shared mutable style objects
			item.setDirty(true);
		}

		container.refresh();
	}
}
