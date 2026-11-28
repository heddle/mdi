package edu.cnu.mdi.view;

import edu.cnu.mdi.log.LogPane;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * This is a predefined view used to display all the log messages.
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class LogView extends BaseView {

	public LogView() {
		this(600, 600, false);
	}

	public LogView(int width, int height, boolean visible) {
		super(PropertyUtils.TITLE, "Log", 
				PropertyUtils.WIDTH, width,
				PropertyUtils.HEIGHT, height, 
				PropertyUtils.VISIBLE, visible,
				PropertyUtils.USECONTAINER, false);
		LogPane log = new LogPane();
		add(log);
		addViewPopupTrigger(log.getViewport().getView());
		addViewPopupTrigger(log.getViewport());
	}

}
