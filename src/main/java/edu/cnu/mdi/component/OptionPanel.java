package edu.cnu.mdi.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * A panel that arranges a set of boolean options (JCheckBoxes) in a
 * multi-column layout suitable for use in MDI views.
 * <p>
 * The options are specified via an array of arrays of strings:
 *
 * <pre>
 * { { "Option 1", "true", "false" }, { "Option 2", "true", "true" } }
 * </pre>
 *
 * where each inner array contains:
 * <ul>
 * <li>label: the checkbox label (index 0)</li>
 * <li>enabled: "true" or "false" (index 1)</li>
 * <li>selected:"true" or "false" (index 2)</li>
 * </ul>
 * <p>
 * The panel arranges the checkboxes into a specified number of columns using
 * one of three layout modes: {@link #AS_ORDERED}, {@link #ALPHABETICAL}, or
 * {@link #MINSIZE}.
 * <p>
 * Columns are sized only as wide as necessary to fit the longest label in that
 * column. Horizontal gaps between columns are controlled by {@link #HGAP}, and
 * vertical spacing is based on the font height plus {@link #VGAP}. The "active"
 * checkbox icons are vertically aligned within each column.
 */
@SuppressWarnings("serial")
public class OptionPanel extends JPanel {

	/** Layout option: use the options in the order provided. */
	public static final int AS_ORDERED = 0;

	/** Layout option: sort options alphabetically by label. */
	public static final int ALPHABETICAL = 1;

	/**
	 * Layout option: minimize overall width by grouping widest labels into the same
	 * column. Internally, options are sorted by descending label width, then
	 * grouped into columns.
	 */
	public static final int MINSIZE = 2;

	/** Default horizontal gap between columns in pixels. */
	public static final int HGAP = 4;

	/** Default additional vertical gap between rows in pixels. */
	public static final int VGAP = 2;

	/**
	 * Listener interface for {@link OptionPanel} selection changes.
	 * <p>
	 * This is a convenience wrapper around {@link ItemListener} that provides the
	 * label and new selection state of the changed option.
	 */
	public interface OptionPanelListener {

		/**
		 * Called when a checkbox in the {@link OptionPanel} changes state.
		 *
		 * @param source   the {@code OptionPanel} where the change occurred
		 * @param label    the label associated with the checkbox
		 * @param selected the new selected state of the checkbox
		 */
		void optionStateChanged(OptionPanel source, String label, boolean selected);
	}

	/**
	 * Internal representation of an option in the panel.
	 */
	private static class OptionEntry {
		final String label;
		final JCheckBox checkBox;
		int columnIndex;
		int rowIndex;
		int labelPixelWidth;

		OptionEntry(String label, JCheckBox checkBox) {
			this.label = label;
			this.checkBox = checkBox;
		}
	}

	// configuration
	private final int _columnCount;
	private final int _layoutOption;
	private final Font _font;
	private final Color _foreground;
	private final Color _background;
	private final OptionPanelListener _listener;

	// data
	private final List<OptionEntry> _entries = new ArrayList<>();
	private final Map<String, OptionEntry> _entryByLabel = new LinkedHashMap<>();

	// computed layout data
	private Dimension _preferredSize;
	private int[] _columnWidths;
	private int _rowHeight;
	private int _maxRows;

	/**
	 * Creates an {@code OptionPanel}.
	 *
	 * @param listener     listener notified when any checkbox changes state; may be
	 *                     {@code null}
	 * @param columnCount  desired number of columns (must be &gt;= 1)
	 * @param font         font used for the checkbox labels (must not be
	 *                     {@code null})
	 * @param foreground   foreground color for the labels (may be {@code null} for
	 *                     default)
	 * @param background   background color for the panel and checkboxes (may be
	 *                     {@code null} for default)
	 * @param layoutOption layout option, one of {@link #AS_ORDERED},
	 *                     {@link #ALPHABETICAL}, or {@link #MINSIZE}
	 * @param optionData   array of option specifications: {@code {label, enabled,
	 *                     selected}} for each option
	 *
	 * @throws IllegalArgumentException if {@code columnCount < 1}, {@code font} is
	 *                                  {@code null}, or {@code optionData} is
	 *                                  invalid
	 */
	public OptionPanel(OptionPanelListener listener, int columnCount, Font font, Color foreground, Color background,
			int layoutOption, String[][] optionData) {

		if (columnCount < 1) {
			throw new IllegalArgumentException("columnCount must be >= 1");
		}
		if (font == null) {
			throw new IllegalArgumentException("font must not be null");
		}
		if (optionData == null) {
			throw new IllegalArgumentException("optionData must not be null");
		}

		_listener = listener;
		_columnCount = columnCount;
		_layoutOption = layoutOption;
		_font = font;
		_foreground = foreground;
		_background = background;

		setLayout(null); // we will perform absolute layout
		if (_background != null) {
			setBackground(_background);
			setOpaque(true);
		}

		buildEntries(optionData);
		layoutEntries();
	}

	/**
	 * Builds the {@link JCheckBox} components and internal {@link OptionEntry}
	 * structures from the raw option data.
	 *
	 * @param optionData the raw option specification array
	 */
	private void buildEntries(String[][] optionData) {

		// Build entries in initial order
		for (String[] row : optionData) {
			if (row == null || row.length < 3) {
				throw new IllegalArgumentException("Each option must have label, enabled, and selected entries.");
			}

			final String label = row[0];
			final boolean enabled = Boolean.parseBoolean(row[1]);
			final boolean selected = Boolean.parseBoolean(row[2]);

			JCheckBox checkBox = new JCheckBox(label, selected);
			checkBox.setFont(_font);

			if (_foreground != null) {
				checkBox.setForeground(_foreground);
			}
			if (_background != null) {
				checkBox.setBackground(_background);
			}

			checkBox.setEnabled(enabled);
			checkBox.setOpaque(_background != null);

			// Internal listener that delegates to the OptionPanelListener
			checkBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (_listener != null) {
						_listener.optionStateChanged(OptionPanel.this, label,
								(e.getStateChange() == ItemEvent.SELECTED));
					}
				}
			});

			OptionEntry entry = new OptionEntry(label, checkBox);
			_entries.add(entry);
			// assumes labels are unique; if not, later entries overwrite earlier
			_entryByLabel.put(label, entry);

			add(checkBox);
		}

		// Compute label widths
		FontMetrics fm = getFontMetrics(_font);
		for (OptionEntry entry : _entries) {
			entry.labelPixelWidth = fm.stringWidth(entry.label);
		}
	}

	/**
	 * Computes the layout (column and row indices, column widths, and preferred
	 * size) and sets component bounds accordingly.
	 */
	private void layoutEntries() {
		if (_entries.isEmpty()) {
			_preferredSize = new Dimension(0, 0);
			return;
		}

		// Make a working list of entries for possible reordering
		List<OptionEntry> working = new ArrayList<>(_entries);

		// Apply layout option ordering
		switch (_layoutOption) {
		case ALPHABETICAL:
			working.sort(Comparator.comparing(e -> e.label, String.CASE_INSENSITIVE_ORDER));
			break;
		case MINSIZE:
			// Sort by decreasing label width, so widest are grouped together
			working.sort(Comparator.comparingInt((OptionEntry e) -> e.labelPixelWidth).reversed());
			break;
		case AS_ORDERED:
		default:
			// keep original order
			break;
		}

		// Determine how many columns we will actually use (cannot exceed number of
		// options)
		int optionCount = working.size();
		int effectiveColumns = Math.min(_columnCount, optionCount);
		if (effectiveColumns < 1) {
			effectiveColumns = 1;
		}

		// Determine how many entries go into each column (column-major grouping)
		int basePerColumn = optionCount / effectiveColumns;
		int remainder = optionCount % effectiveColumns;

		int[] columnSizes = new int[effectiveColumns];
		for (int c = 0; c < effectiveColumns; c++) {
			columnSizes[c] = basePerColumn + ((c < remainder) ? 1 : 0);
		}

		// Assign entries to columns and rows
		int index = 0;
		_maxRows = 0;
		for (int c = 0; c < effectiveColumns; c++) {
			int rowsInColumn = columnSizes[c];
			for (int r = 0; r < rowsInColumn; r++) {
				OptionEntry entry = working.get(index++);
				entry.columnIndex = c;
				entry.rowIndex = r;
				_maxRows = Math.max(_maxRows, r + 1);
			}
		}

		// Compute per-column widths based on checkbox preferred sizes
		_columnWidths = new int[effectiveColumns];
		for (OptionEntry entry : working) {
			int col = entry.columnIndex;
			int prefWidth = entry.checkBox.getPreferredSize().width;
			_columnWidths[col] = Math.max(_columnWidths[col], prefWidth);
		}

		FontMetrics fm = getFontMetrics(_font);
		_rowHeight = fm.getHeight() + VGAP;

		// Compute preferred size
		Insets insets = getInsets();
		int totalWidth = insets.left + insets.right;
		if (effectiveColumns > 0) {
			totalWidth += Arrays.stream(_columnWidths).sum();
			totalWidth += HGAP * (effectiveColumns - 1);
		}

		int totalHeight = insets.top + insets.bottom;
		totalHeight += _maxRows * _rowHeight;

		_preferredSize = new Dimension(totalWidth, totalHeight);

		// Apply bounds to each checkbox to align active parts in columns
		int x = insets.left;
		for (int c = 0; c < effectiveColumns; c++) {
			final int colWidth = _columnWidths[c];
			for (OptionEntry entry : working) {
				if (entry.columnIndex == c) {
					int y = insets.top + entry.rowIndex * _rowHeight;
					entry.checkBox.setBounds(x, y, colWidth, _rowHeight);
				}
			}
			x += colWidth + HGAP;
		}

		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return (_preferredSize != null) ? _preferredSize : super.getPreferredSize();
	}

	// ----------------------------------------------------------------
	// Convenience API for interacting with options
	// ----------------------------------------------------------------

	/**
	 * Returns whether the option with the given label is currently selected.
	 *
	 * @param label the label of the option
	 * @return {@code true} if selected, {@code false} if not selected or not found
	 */
	public boolean isOptionSelected(String label) {
		OptionEntry entry = _entryByLabel.get(label);
		return (entry != null) && entry.checkBox.isSelected();
	}

	/**
	 * Sets the selected state of the option with the given label.
	 *
	 * @param label    the label of the option
	 * @param selected the new selected state
	 */
	public void setOptionSelected(String label, boolean selected) {
		OptionEntry entry = _entryByLabel.get(label);
		if (entry != null) {
			entry.checkBox.setSelected(selected);
		}
	}

	/**
	 * Enables or disables the option with the given label.
	 *
	 * @param label   the label of the option
	 * @param enabled {@code true} to enable, {@code false} to disable
	 */
	public void setOptionEnabled(String label, boolean enabled) {
		OptionEntry entry = _entryByLabel.get(label);
		if (entry != null) {
			entry.checkBox.setEnabled(enabled);
		}
	}

	/**
	 * Returns a defensive copy of the labels of all currently selected options.
	 *
	 * @return an array of selected option labels; never {@code null}
	 */
	public String[] getSelectedOptionLabels() {
		List<String> selected = new ArrayList<>();
		for (OptionEntry entry : _entries) {
			if (entry.checkBox.isSelected()) {
				selected.add(entry.label);
			}
		}
		return selected.toArray(new String[0]);
	}

	/**
	 * Returns the underlying {@link JCheckBox} for the given label, or {@code null}
	 * if not found. This can be useful for advanced customization.
	 *
	 * @param label the label of the option
	 * @return the {@code JCheckBox}, or {@code null} if not found
	 */
	public JCheckBox getCheckBox(String label) {
		OptionEntry entry = _entryByLabel.get(label);
		return (entry != null) ? entry.checkBox : null;
	}
}
