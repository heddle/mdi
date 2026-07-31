package edu.cnu.mdi.splot.pdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Named list of {@code Double} values with cached min/max tracking.
 *
 * <p>
 * Min/max are updated incrementally on {@link #add(Double)} for speed. If
 * values are removed or replaced, min/max are recomputed as needed to remain
 * correct.
 * </p>
 */
@SuppressWarnings("serial")
public class DataList extends ArrayList<Double> {

	private double min = Double.POSITIVE_INFINITY;
	private double max = Double.NEGATIVE_INFINITY;

	/**
	 * Create a data list with the given name.
	 *
	 */
	public DataList() {
		super();
	}

	@Override
	public boolean add(Double value) {
		if (value != null) {
			if (value < min) {
				min = value;
			}
			if (value > max) {
				max = value;
			}
		}
		return super.add(value);
	}

	@Override
	public void add(int index, Double element) {
		// Let ArrayList validate the index before changing either data or caches.
		super.add(index, element);
		includeInRange(element);
	}

	@Override
	public boolean addAll(Collection<? extends Double> c) {
		Objects.requireNonNull(c, "collection");
		boolean changed = super.addAll(c);
		if (changed) {
			for (Double value : c) {
				includeInRange(value);
			}
		}
		return changed;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Double> c) {
		Objects.requireNonNull(c, "collection");
		boolean changed = super.addAll(index, c);
		if (changed) {
			for (Double value : c) {
				includeInRange(value);
			}
		}
		return changed;
	}

	@Override
	public Double remove(int index) {
		Double removed = super.remove(index);
		if (removed != null && (removed == min || removed == max)) {
			recomputeMinMax();
		}
		return removed;
	}

	@Override
	public boolean remove(Object o) {
		boolean changed = super.remove(o);
		if (changed) {
			// conservative: if we removed something, min/max may have changed
			// (especially if duplicates exist, recompute is still safe)
			recomputeMinMax();
		}
		return changed;
	}

	@Override
	public void clear() {
		super.clear();
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
	}

	@Override
	public Double set(int index, Double element) {
		Double prev = super.set(index, element);
		// prev might have been min/max; element might become min/max
		recomputeMinMax();
		return prev;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = super.removeAll(c);
		if (changed) {
			recomputeMinMax();
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = super.retainAll(c);
		if (changed) {
			recomputeMinMax();
		}
		return changed;
	}

	@Override
	public boolean removeIf(Predicate<? super Double> filter) {
		boolean changed = super.removeIf(filter);
		if (changed) {
			recomputeMinMax();
		}
		return changed;
	}

	@Override
	public void replaceAll(UnaryOperator<Double> operator) {
		super.replaceAll(operator);
		recomputeMinMax();
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
		recomputeMinMax();
	}

	/**
	 * Get the minimum value in the column.
	 *
	 * @return the minimum value, or +infinity if empty
	 */
	public double getMin() {
		return min;
	}

	/**
	 * Get the maximum value in the column.
	 *
	 * @return the maximum value, or -infinity if empty
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Get the values as a primitive array.
	 *
	 * @return values as a new primitive array
	 */
	public double[] values() {
		int n = size();
		double[] array = new double[n];
		for (int i = 0; i < n; i++) {
			Double d = get(i);
			array[i] = (d == null) ? Double.NaN : d.doubleValue();
		}
		return array;
	}

	/** Recompute min/max by scanning current contents. */
	protected void recomputeMinMax() {
		double newMin = Double.POSITIVE_INFINITY;
		double newMax = Double.NEGATIVE_INFINITY;

		for (Double d : this) {
			if (d == null) {
				continue;
			}
			if (d < newMin) {
				newMin = d;
			}
			if (d > newMax) {
				newMax = d;
			}
		}

		min = newMin;
		max = newMax;
	}

	private void includeInRange(Double value) {
		if (value == null) {
			return;
		}
		if (value < min) {
			min = value;
		}
		if (value > max) {
			max = value;
		}
	}
}
