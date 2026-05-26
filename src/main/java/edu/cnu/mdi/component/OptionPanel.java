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
 * A panel that arranges a set of boolean options ({@link JCheckBox}es) in a
 * multi-column layout suitable for use in MDI views.
 *
 * <h2>Option Data Format</h2>
 * <p>
 * Options are specified as an array of three-element {@code String} arrays:
 * </p>
 *
 * <pre>
 * { { "Option 1", "true",  "false" },
 *   { "Option 2", "true",  "true"  },
 *   { "Option 3", "false", "false" } }
 * </pre>
 *
 * <p>Each inner array must contain exactly three elements:</p>
 * <ol>
 *   <li><b>label</b>   – the text shown beside the checkbox (index 0)</li>
 *   <li><b>enabled</b> – {@code "true"} or {@code "false"} (index 1)</li>
 *   <li><b>selected</b>– {@code "true"} or {@code "false"} (index 2)</li>
 * </ol>
 *
 * <h2>Layout</h2>
 * <p>
 * Checkboxes are distributed across the requested number of columns in
 * column-major order (filled top-to-bottom, then left-to-right). Three
 * ordering modes are available via the {@code layoutOption} constructor
 * parameter:
 * </p>
 * <ul>
 *   <li>{@link #AS_ORDERED}   – options appear in the order supplied</li>
 *   <li>{@link #ALPHABETICAL} – options are sorted by label (case-insensitive)</li>
 *   <li>{@link #MINSIZE}      – options are sorted so the widest labels are
 *       spread across columns, minimising overall panel width</li>
 * </ul>
 * <p>
 * Each column is only as wide as its widest checkbox. Columns are separated by
 * {@link #HGAP} pixels, and rows are spaced by font-height plus {@link #VGAP}
 * pixels.
 * </p>
 *
 * <h2>Preferred-size and borders</h2>
 * <p>
 * Layout is performed once at construction time using a null layout (absolute
 * positioning). The <em>content</em> size (the area required by the checkboxes
 * themselves, <em>excluding</em> any border insets) is captured in
 * {@code _contentSize}. {@link #getPreferredSize()} then adds the panel's
 * <em>current</em> insets to that content size each time it is called, so the
 * result is always correct even when a border — such as a
 * {@link CommonBorder} — is applied <em>after</em> construction (which would
 * otherwise cause the border's title area to be clipped).
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * This class is not thread-safe. All interactions must occur on the
 * Swing Event Dispatch Thread (EDT).
 * </p>
 *
 * @see OptionPanelListener
 * @see CommonBorder
 */
@SuppressWarnings("serial")
public class OptionPanel extends JPanel {

	// ----------------------------------------------------------------
	// Layout option constants
	// ----------------------------------------------------------------

	/**
	 * Layout option: display options in the order they were supplied to the
	 * constructor, with no reordering.
	 */
	public static final int AS_ORDERED = 0;

	/**
	 * Layout option: sort options alphabetically by label (case-insensitive) before
	 * laying them out in columns.
	 */
	public static final int ALPHABETICAL = 1;

	/**
	 * Layout option: sort options by descending label pixel width before laying them
	 * out in columns. This tends to produce the narrowest overall panel width
	 * because the widest labels are distributed evenly across columns rather than
	 * all ending up in the same one.
	 */
	public static final int MINSIZE = 2;

	// ----------------------------------------------------------------
	// Spacing constants
	// ----------------------------------------------------------------

	/**
	 * Horizontal gap in pixels between adjacent columns of checkboxes.
	 * Callers who need a different gap should subclass or wrap the panel.
	 */
	public static final int HGAP = 10;

	/**
	 * Additional vertical gap in pixels added to the font height to determine the
	 * height of each row.
	 */
	public static final int VGAP = 6;

	// ----------------------------------------------------------------
	// Listener interface
	// ----------------------------------------------------------------

	/**
	 * Callback interface for changes to checkbox selection state within an
	 * {@link OptionPanel}.
	 *
	 * <p>This is a convenience wrapper around {@link ItemListener} that surfaces
	 * the human-readable label of the changed option rather than requiring callers
	 * to interrogate the raw {@link java.awt.event.ItemEvent ItemEvent}.</p>
	 */
	public interface OptionPanelListener {

		/**
		 * Invoked when a checkbox inside an {@link OptionPanel} changes state.
		 *
		 * @param source   the {@code OptionPanel} that contains the changed checkbox;
		 *                 never {@code null}
		 * @param label    the label text of the checkbox whose state changed;
		 *                 never {@code null}
		 * @param selected {@code true} if the checkbox is now selected,
		 *                 {@code false} if it is now deselected
		 */
		void optionStateChanged(OptionPanel source, String label, boolean selected);
	}

	// ----------------------------------------------------------------
	// Private inner class
	// ----------------------------------------------------------------

	/**
	 * Internal record that pairs a checkbox with its metadata and computed layout
	 * position. Instances are created once during {@link #buildEntries} and are
	 * mutated in-place during {@link #layoutEntries}.
	 */
	private static class OptionEntry {

		/** The label text used to identify this option. */
		final String label;

		/** The Swing component rendered for this option. */
		final JCheckBox checkBox;

		/** Zero-based column index assigned during layout. */
		int columnIndex;

		/** Zero-based row index within its column, assigned during layout. */
		int rowIndex;

		/**
		 * Width of {@link #label} in pixels at the panel's font, measured by
		 * {@link FontMetrics#stringWidth}. Used by the {@link OptionPanel#MINSIZE}
		 * layout option.
		 */
		int labelPixelWidth;

		/**
		 * Constructs an entry for the given label/checkbox pair.
		 *
		 * @param label    the option label; never {@code null}
		 * @param checkBox the checkbox component; never {@code null}
		 */
		OptionEntry(String label, JCheckBox checkBox) {
			this.label = label;
			this.checkBox = checkBox;
		}
	}

	// ----------------------------------------------------------------
	// Fields
	// ----------------------------------------------------------------

	// --- constructor-supplied configuration --------------------------

	/** Number of columns requested by the caller. */
	private final int _columnCount;

	/**
	 * One of {@link #AS_ORDERED}, {@link #ALPHABETICAL}, or {@link #MINSIZE}.
	 */
	private final int _layoutOption;

	/** Font applied to all checkbox labels. */
	private final Font _font;

	/** Foreground colour for labels, or {@code null} to use the default. */
	private final Color _foreground;

	/**
	 * Background colour for the panel and all checkboxes, or {@code null} to use
	 * the default.
	 */
	private final Color _background;

	/**
	 * Delegate notified when any checkbox changes state, or {@code null} if no
	 * notification is required.
	 */
	private final OptionPanelListener _listener;

	// --- data --------------------------------------------------------

	/**
	 * All entries in their original (constructor-supplied) order. Used by
	 * {@link #getSelectedOptionLabels()} to preserve insertion order.
	 */
	private final List<OptionEntry> _entries = new ArrayList<>();

	/**
	 * Fast lookup from label to entry. Labels are assumed to be unique; if
	 * duplicates are present, the later entry silently overwrites the earlier one.
	 */
	private final Map<String, OptionEntry> _entryByLabel = new LinkedHashMap<>();

	// --- computed layout data ----------------------------------------

	/**
	 * The size required to display all checkboxes, <em>excluding</em> any border
	 * insets. Set by {@link #layoutEntries()} and consumed by
	 * {@link #getPreferredSize()}.
	 *
	 * <p><b>Design note – why content size rather than full preferred size:</b><br>
	 * Layout is performed once at construction time. If a border is attached
	 * <em>after</em> construction (a common pattern when the border is set in a
	 * subclass constructor after {@code super()} returns), the insets reported by
	 * {@link #getInsets()} during layout are zero and therefore cannot be included
	 * in a size snapshot.  By storing only the border-inset-free content size here,
	 * {@link #getPreferredSize()} can add the <em>current</em> insets at call time,
	 * giving a correct result regardless of when the border is applied.</p>
	 */
	private Dimension _contentSize;

	/**
	 * Width of each column in pixels, indexed by zero-based column index. Set by
	 * {@link #layoutEntries()} and consumed by {@link #doLayout()}.
	 */
	private int[] _columnWidths;

	/** Height of a single row in pixels (font height + {@link #VGAP}). Set by
	 * {@link #layoutEntries()} and consumed by {@link #doLayout()}.
	 */
	private int _rowHeight;

	/**
	 * Maximum number of rows across all columns. Equal to the number of entries in
	 * the tallest column after distribution.
	 */
	private int _maxRows;

	/**
	 * Entries in the order they should be rendered (possibly reordered from
	 * {@link #_entries} depending on {@link #_layoutOption}). Set once by
	 * {@link #layoutEntries()} and used by {@link #doLayout()} to position
	 * checkboxes with the correct live insets.
	 */
	private List<OptionEntry> _orderedEntries;

	// ----------------------------------------------------------------
	// Constructor
	// ----------------------------------------------------------------

	/**
	 * Creates an {@code OptionPanel} with the specified configuration.
	 *
	 * <p>Layout is performed immediately during construction. If a border will be
	 * added after construction (e.g. in a subclass), {@link #getPreferredSize()}
	 * will still return the correct size because it adds the current insets
	 * dynamically rather than relying on a snapshot taken at construction time.</p>
	 *
	 * @param listener     callback notified when any checkbox changes state;
	 *                     {@code null} is permitted and means no notification
	 * @param columnCount  desired number of columns; must be &ge; 1
	 * @param font         font used for all checkbox labels; must not be
	 *                     {@code null}
	 * @param foreground   foreground (text) colour for labels; {@code null} uses
	 *                     the look-and-feel default
	 * @param background   background colour for the panel and all checkboxes;
	 *                     {@code null} uses the look-and-feel default and leaves
	 *                     the panel non-opaque
	 * @param layoutOption ordering mode; one of {@link #AS_ORDERED},
	 *                     {@link #ALPHABETICAL}, or {@link #MINSIZE}
	 * @param optionData   option specifications; each element must be a
	 *                     three-element array {@code {label, enabled, selected}}
	 *
	 * @throws IllegalArgumentException if {@code columnCount < 1}, {@code font} is
	 *                                  {@code null}, {@code optionData} is
	 *                                  {@code null}, or any inner array has fewer
	 *                                  than three elements
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

		setLayout(null); // absolute positioning; bounds are set in layoutEntries()
		if (_background != null) {
			setBackground(_background);
			setOpaque(true);
		}

		buildEntries(optionData);
		layoutEntries();
	}

	// ----------------------------------------------------------------
	// Private layout helpers
	// ----------------------------------------------------------------

	/**
	 * Creates {@link JCheckBox} components and populates {@link #_entries} and
	 * {@link #_entryByLabel} from the raw option data.
	 *
	 * <p>An internal {@link ItemListener} is attached to each checkbox. It
	 * delegates to {@link #_listener} (if non-null), passing the label and new
	 * selection state.</p>
	 *
	 * @param optionData the raw option specification array; validated by the caller
	 * @throws IllegalArgumentException if any inner array has fewer than three
	 *                                  elements
	 */
	private void buildEntries(String[][] optionData) {

		for (String[] row : optionData) {
			if (row == null || row.length < 3) {
				throw new IllegalArgumentException(
						"Each option entry must have three elements: label, enabled, selected.");
			}

			final String label    = row[0];
			final boolean enabled  = Boolean.parseBoolean(row[1]);
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

			// Delegate state changes to the panel listener, if any.
			checkBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (_listener != null) {
						_listener.optionStateChanged(
								OptionPanel.this,
								label,
								e.getStateChange() == ItemEvent.SELECTED);
					}
				}
			});

			OptionEntry entry = new OptionEntry(label, checkBox);
			_entries.add(entry);
			_entryByLabel.put(label, entry); // last entry wins on duplicate labels
			add(checkBox);
		}

		// Measure label widths once, after the component is added (so getFontMetrics
		// is backed by a Graphics context from the toolkit).
		FontMetrics fm = getFontMetrics(_font);
		for (OptionEntry entry : _entries) {
			entry.labelPixelWidth = fm.stringWidth(entry.label);
		}
	}

	/**
	 * Computes the column/row assignment for every entry and determines per-column
	 * widths and the overall content size. Stores results in {@link #_orderedEntries},
	 * {@link #_columnWidths}, {@link #_rowHeight}, and {@link #_contentSize} for
	 * later use by {@link #doLayout()}.
	 *
	 * <p><b>Important:</b> this method deliberately does <em>not</em> call
	 * {@link java.awt.Component#setBounds setBounds()} on the checkboxes and does
	 * <em>not</em> include border insets in {@link #_contentSize}. Actual
	 * positioning is deferred to {@link #doLayout()}, which is called by Swing
	 * with the real insets in place. This avoids the problem of a border applied
	 * after construction (e.g. in a subclass) being invisible to the layout.</p>
	 */
	private void layoutEntries() {
		if (_entries.isEmpty()) {
			_contentSize = new Dimension(0, 0);
			_orderedEntries = new ArrayList<>();
			return;
		}

		// Working list — may be reordered without affecting _entries.
		List<OptionEntry> working = new ArrayList<>(_entries);

		switch (_layoutOption) {
		case ALPHABETICAL:
			working.sort(Comparator.comparing(e -> e.label, String.CASE_INSENSITIVE_ORDER));
			break;
		case MINSIZE:
			// Widest labels first so they are spread across columns rather than stacked.
			working.sort(Comparator.comparingInt((OptionEntry e) -> e.labelPixelWidth).reversed());
			break;
		case AS_ORDERED:
		default:
			// No reordering required.
			break;
		}

		int optionCount = working.size();
		int effectiveColumns = Math.max(1, Math.min(_columnCount, optionCount));

		// Distribute entries into columns as evenly as possible (column-major order).
		int basePerColumn = optionCount / effectiveColumns;
		int remainder     = optionCount % effectiveColumns;

		int[] columnSizes = new int[effectiveColumns];
		for (int c = 0; c < effectiveColumns; c++) {
			columnSizes[c] = basePerColumn + (c < remainder ? 1 : 0);
		}

		// Assign column/row indices.
		int index = 0;
		_maxRows = 0;
		for (int c = 0; c < effectiveColumns; c++) {
			for (int r = 0; r < columnSizes[c]; r++) {
				OptionEntry entry = working.get(index++);
				entry.columnIndex = c;
				entry.rowIndex    = r;
				_maxRows = Math.max(_maxRows, r + 1);
			}
		}

		// Each column is as wide as its widest checkbox preferred size.
		_columnWidths = new int[effectiveColumns];
		for (OptionEntry entry : working) {
			int prefWidth = entry.checkBox.getPreferredSize().width;
			_columnWidths[entry.columnIndex] = Math.max(_columnWidths[entry.columnIndex], prefWidth);
		}

		FontMetrics fm = getFontMetrics(_font);
		_rowHeight = fm.getHeight() + VGAP;

		// Store content size without border insets; getPreferredSize() and
		// doLayout() both add live insets at call time.
		int contentWidth  = Arrays.stream(_columnWidths).sum() + HGAP * (effectiveColumns - 1);
		int contentHeight = _maxRows * _rowHeight;
		_contentSize = new Dimension(contentWidth, contentHeight);

		// Preserve the ordered list for doLayout().
		_orderedEntries = working;

		// doLayout() will be called by Swing when the component is realised;
		// trigger it now so the panel renders correctly if already visible.
		revalidate();
		repaint();
	}

	// ----------------------------------------------------------------
	// Overrides
	// ----------------------------------------------------------------

	/**
	 * Returns the preferred size of this panel, dynamically adding the panel's
	 * current border insets to the pre-computed content size.
	 *
	 * <p>Because insets are read at call time rather than snapshotted at
	 * construction time, this method returns a correct value even when a border
	 * (e.g. a {@link CommonBorder}) is applied <em>after</em> the constructor
	 * returns — a pattern used by subclasses such as {@code ChimeraOptionPanel}.</p>
	 *
	 * @return the preferred {@link Dimension}; never {@code null}
	 */
	@Override
	public Dimension getPreferredSize() {
		if (_contentSize == null) {
			return super.getPreferredSize();
		}
		Insets insets = getInsets();
		return new Dimension(
				_contentSize.width  + insets.left + insets.right,
				_contentSize.height + insets.top  + insets.bottom);
	}

	/**
	 * Positions each checkbox using the panel's current insets as the content
	 * origin.
	 *
	 * <p>Because this method is called by Swing every time the component is
	 * validated — including after a border is applied post-construction — the
	 * checkboxes are always placed below the titled border's title text and inside
	 * any other inset region, regardless of when {@link javax.swing.border.Border
	 * Border} was set.</p>
	 *
	 * <p>The column/row assignments and column widths computed by
	 * {@link #layoutEntries()} are reused here; only the pixel coordinates are
	 * recalculated.</p>
	 */
	@Override
	public void doLayout() {
		if (_orderedEntries == null || _columnWidths == null) {
			super.doLayout();
			return;
		}

		Insets insets = getInsets(); // live insets — correct even after setBorder()

		int x = insets.left;
		int lastCol = -1;
		for (OptionEntry entry : _orderedEntries) {
			int c = entry.columnIndex;
			// Accumulate x only when moving to the next column.
			if (c != lastCol) {
				if (lastCol >= 0) {
					x += _columnWidths[lastCol] + HGAP;
				}
				lastCol = c;
			}
			int y = insets.top + entry.rowIndex * _rowHeight;
			entry.checkBox.setBounds(x, y, _columnWidths[c], _rowHeight);
		}
	}

	// ----------------------------------------------------------------
	// Public API
	// ----------------------------------------------------------------

	/**
	 * Returns whether the option identified by {@code label} is currently selected.
	 *
	 * @param label the label of the option to query; must not be {@code null}
	 * @return {@code true} if the option exists and its checkbox is selected;
	 *         {@code false} otherwise (including when the label is not found)
	 */
	public boolean isOptionSelected(String label) {
		OptionEntry entry = _entryByLabel.get(label);
		return (entry != null) && entry.checkBox.isSelected();
	}

	/**
	 * Programmatically sets the selected state of the checkbox identified by
	 * {@code label}.
	 *
	 * <p>If the label is not found, this method does nothing. The call will
	 * fire any registered {@link OptionPanelListener} (via the checkbox's own
	 * {@link ItemListener}) if the state actually changes.</p>
	 *
	 * @param label    the label of the option to update; must not be {@code null}
	 * @param selected {@code true} to select, {@code false} to deselect
	 */
	public void setOptionSelected(String label, boolean selected) {
		OptionEntry entry = _entryByLabel.get(label);
		if (entry != null) {
			entry.checkBox.setSelected(selected);
		}
	}

	/**
	 * Enables or disables the checkbox identified by {@code label}.
	 *
	 * <p>A disabled checkbox is visible but cannot be interacted with by the user.
	 * If the label is not found, this method does nothing.</p>
	 *
	 * @param label   the label of the option to update; must not be {@code null}
	 * @param enabled {@code true} to enable, {@code false} to disable
	 */
	public void setOptionEnabled(String label, boolean enabled) {
		OptionEntry entry = _entryByLabel.get(label);
		if (entry != null) {
			entry.checkBox.setEnabled(enabled);
		}
	}

	/**
	 * Returns the labels of all currently <em>selected</em> options, in the order
	 * they were originally supplied to the constructor.
	 *
	 * @return a new array containing the labels of all selected options;
	 *         never {@code null}, but may be empty
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
	 * Returns the underlying {@link JCheckBox} for the option identified by
	 * {@code label}, or {@code null} if no such option exists.
	 *
	 * <p>This method is intended for advanced customisation (e.g. attaching
	 * additional listeners or tweaking renderer properties). Prefer the
	 * higher-level methods ({@link #isOptionSelected}, {@link #setOptionSelected},
	 * etc.) for normal use.</p>
	 *
	 * @param label the label of the option; must not be {@code null}
	 * @return the {@link JCheckBox}, or {@code null} if the label is not found
	 */
	public JCheckBox getCheckBox(String label) {
		OptionEntry entry = _entryByLabel.get(label);
		return (entry != null) ? entry.checkBox : null;
	}
}