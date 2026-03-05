package edu.cnu.mdi.log;

import java.awt.Color;
import java.awt.Dimension;
import java.util.EnumMap;

import javax.swing.text.SimpleAttributeSet;

import edu.cnu.mdi.component.TextPaneScrollPane;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * Combines all log messages into one text pane. Uses different colors to
 * highlight different types of log messages.
 *
 * @author heddle
 */

@SuppressWarnings("serial")
public class LogPane extends TextPaneScrollPane {

	// the font sizes for different log levels
	private static int CONFIGFONTSIZE = 12;
	private static int WARNINGFONTSIZE = 11;
	private static int INFOFONTSIZE = 12;
	private static int ERRORFONTSIZE = 11;
	private static int EXCEPTIONFONTSIZE = 10;


	// the styles for different log levels
	private static EnumMap<Log.Level, SimpleAttributeSet> styles;

	// initialize the styles
	static {
		styles = new EnumMap<>(Log.Level.class);
		styles.put(Log.Level.INFO, createStyle(Color.black, "sansserif", INFOFONTSIZE, false, false));
		styles.put(Log.Level.CONFIG, createStyle(Color.blue, "sansserif", CONFIGFONTSIZE, false, false));
		styles.put(Log.Level.WARNING, createStyle(X11Colors.getX11Color("orange red"), "monospaced", WARNINGFONTSIZE, false, true));
		styles.put(Log.Level.ERROR, createStyle(Color.red, "sanserif", ERRORFONTSIZE, false, true));
		styles.put(Log.Level.ERROR, createStyle(Color.red, "monospaced", EXCEPTIONFONTSIZE, false, true));
	}

	/**
	 * Constructor.
	 */
	public LogPane() {
		setPreferredSize(new Dimension(800, 400));

		ILogListener ll = new ILogListener() {

			@Override
			public void config(String message) {
				append(Log.Level.CONFIG, message);
			}

			@Override
			public void info(String message) {
				append(Log.Level.INFO, message);
			}

			@Override
			public void error(String message) {
				append(Log.Level.ERROR, message);
			}

			@Override
			public void exception(String message) {
				append(Log.Level.EXCEPTION, message);
			}

			@Override
			public void warning(String message) {
				append(Log.Level.WARNING, message);
			}
		};

		Log.getInstance().addLogListener(ll);

	}

	/**
	 * Fix the message so it gets appended nicely.
	 *
	 * @param message the input message.
	 * @return the fixed message.
	 */
	private String fixMessage(String message) {
		if (message == null) {
			return "";
		}

		if (!(message.endsWith("\n"))) {
			return message + "\n";
		}
		return message;
	}

	/**
	 * Append the message with the appropriate style.
	 *
	 * @param grade   the grade of the messaged.
	 * @param message the message text.
	 */
	private void append(Log.Level level, String message) {
		boolean writeTime = (level == Log.Level.ERROR) || (level == Log.Level.EXCEPTION);
		append(fixMessage(message), styles.get(level), writeTime);
	}

}
