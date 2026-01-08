package edu.cnu.mdi.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.function.Function;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * A JComboBox backed directly by enum values, with optional extra choice (e.g.
 * "Any" or "None") and customizable labels.
 *
 * @param <E> the enum type
 */
@SuppressWarnings("serial")
public class EnumComboBox<E extends Enum<E>> extends JComboBox<E> {

	/** Label to use for the extra non-enum choice (e.g. "Any"). */
	private final String extraChoiceLabel;

	/** Function that maps an enum constant to the label to show. */
	private final Function<E, String> labelProvider;

	/**
	 * Create a combo box that lists all constants of the given enum type, using
	 * {@link Enum#name()} as the label.
	 *
	 * @param enumType the enum class
	 */
	public EnumComboBox(Class<E> enumType) {
		this(enumType, null, Enum::name);
	}

	/**
	 * Create a combo box that lists all constants of the given enum type, with an
	 * optional extra choice (e.g. "Any"), a default selection, and a custom label
	 * provider.
	 *
	 * @param enumType         the enum class
	 * @param defaultChoice    the default selected enum constant, or null to select
	 *                         the extra choice if present
	 * @param extraChoiceLabel label for a non-enum choice at the top, or null for
	 *                         none
	 * @param labelProvider    function mapping enum constants to display labels
	 */
	public EnumComboBox(Class<E> enumType, E defaultChoice, String extraChoiceLabel,
			Function<E, String> labelProvider) {
		this(enumType, extraChoiceLabel, labelProvider);
		if (defaultChoice != null) {
			setSelectedItem(defaultChoice);
		} else if (extraChoiceLabel != null) {
			setSelectedItem(null);
		}
	}

	/**
	 * Create a combo box that lists all constants of the given enum type, with an
	 * optional extra choice (e.g. "Any") and a custom label provider.
	 *
	 * @param enumType         the enum class
	 * @param extraChoiceLabel label for a non-enum choice at the top, or null for
	 *                         none
	 * @param labelProvider    function mapping enum constants to display labels
	 */
	public EnumComboBox(Class<E> enumType, String extraChoiceLabel, Function<E, String> labelProvider) {

		super(new DefaultComboBoxModel<>());
		this.extraChoiceLabel = extraChoiceLabel;
		this.labelProvider = (labelProvider != null) ? labelProvider : Enum::name;

		DefaultComboBoxModel<E> model = (DefaultComboBoxModel<E>) getModel();

		// optional "Any"/"None" entry represented by null
		if (extraChoiceLabel != null) {
			model.addElement(null);
		}

		for (E constant : enumType.getEnumConstants()) {
			model.addElement(constant);
		}

		// custom renderer to show labels
		setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if (value == null) {
					setText(extraChoiceLabel);
				} else {
					@SuppressWarnings("unchecked")
					E e = (E) value;
					setText(labelProvider.apply(e));
				}
				return this;
			}
		});

		// optional: size to content (similar to your version)
		sizeComboBox();
	}

	/**
	 * Get the selected enum value. Returns {@code null} if the extra choice was
	 * used and is selected.
	 *
	 * @return the selected enum value, or {@code null} for the extra choice
	 */
	public E getSelectedEnum() {
		@SuppressWarnings("unchecked")
		E value = (E) getSelectedItem();
		return value;
	}

	// size the combo box to just fit (similar logic to the original)
	private void sizeComboBox() {
		FontMetrics fm = getFontMetrics(getFont());
		int maxSW = 0;
		int count = getItemCount();

		for (int i = 0; i < count; i++) {
			Object o = getItemAt(i);
			if (o == null && extraChoiceLabel == null) {
				continue;
			}
			String s = (o == null) ? extraChoiceLabel : labelFor((E) o);
			if (s != null) {
				maxSW = Math.max(maxSW, fm.stringWidth(s));
			}
		}
		Dimension d = getPreferredSize();
		d.width = maxSW + 80; // padding
		setPreferredSize(d);
		setMinimumSize(d);
	}

	private String labelFor(E e) {
		return (e == null) ? extraChoiceLabel : labelProvider.apply(e);
	}
}
