package edu.cnu.mdi.view;

import edu.cnu.mdi.log.SimpleLogPane;
import edu.cnu.mdi.properties.PropertyUtils;

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
		super(PropertyUtils.TITLE, "Log", PropertyUtils.ICONIFIABLE, true, PropertyUtils.MAXIMIZABLE, true,
				PropertyUtils.CLOSABLE, true, PropertyUtils.RESIZABLE, true, PropertyUtils.WIDTH, width,
				PropertyUtils.PROPNAME, "LOGVIEW", PropertyUtils.HEIGHT, height, PropertyUtils.VISIBLE, visible);
		add(new SimpleLogPane());
	}

}
