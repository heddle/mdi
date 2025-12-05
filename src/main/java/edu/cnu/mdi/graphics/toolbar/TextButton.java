package edu.cnu.mdi.graphics.toolbar;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.dialog.LabelDialog;
import edu.cnu.mdi.graphics.GraphicsUtils;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.item.TextItem;


/**
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class TextButton extends ToolBarToggleButton {

	/**
	 * Create a button for creating annotated text items.
	 *
	 * @param container the owner container.
	 */
	public TextButton(IContainer container) {
		super(container, "images/text.gif", "Use to annotate");
	}

	/**
	 * The mouse was clicked.
	 *
	 * @param mouseEvent the causal event.
	 */
	@Override
	public void mouseClicked(MouseEvent mouseEvent) {

		LabelDialog labelDialog = new LabelDialog();
		GraphicsUtils.centerComponent(labelDialog);
		labelDialog.setVisible(true);

		String resultString = UnicodeSupport.specialCharReplace(labelDialog.getText());

		if ((resultString != null) && (resultString.length() > 0)) {
			Font font = labelDialog.getSelectedFont();
			if (font != null) {
				Point2D.Double wp = new Point2D.Double();
				container.localToWorld(mouseEvent.getPoint(), wp);
				TextItem item = new TextItem(container.getAnnotationList(), wp, font, resultString,
						labelDialog.getTextForeground(), labelDialog.getTextBackground(), null);
				if (item != null) {
					item.setDraggable(true);
					item.setRotatable(true);
					item.setResizable(true);
					item.setDeletable(true);
					item.setLocked(false);
					item.setRightClickable(true);
				}
				container.refresh();
			}
		}

		container.selectAllItems(false);
		container.getToolBar().resetDefaultSelection();
		container.refresh();
	}

}
