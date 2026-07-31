package edu.cnu.mdi.splot.pdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class DataListTest {

	@Test
	void indexedAddValidatesBeforeMutating() {
		DataList data = new DataList();

		assertThrows(IndexOutOfBoundsException.class, () -> data.add(2, 5.0));
		assertEquals(0, data.size());
		assertEquals(Double.POSITIVE_INFINITY, data.getMin());
		assertEquals(Double.NEGATIVE_INFINITY, data.getMax());
	}

	@Test
	void everyBulkMutationKeepsCachedBoundsCorrect() {
		DataList data = new DataList();
		data.addAll(List.of(3.0, 7.0));
		data.addAll(1, List.of(-2.0, 12.0));
		assertBounds(data, -2.0, 12.0);

		data.removeAll(List.of(-2.0, 12.0));
		assertBounds(data, 3.0, 7.0);

		data.addAll(List.of(-4.0, 20.0));
		data.retainAll(List.of(3.0, 7.0, -4.0));
		assertBounds(data, -4.0, 7.0);

		data.removeIf(value -> value < 0.0);
		assertBounds(data, 3.0, 7.0);

		data.replaceAll(value -> -value);
		assertBounds(data, -7.0, -3.0);

		data.subList(0, 1).clear();
		assertBounds(data, -7.0, -7.0);
	}

	private static void assertBounds(DataList data, double min, double max) {
		assertEquals(min, data.getMin());
		assertEquals(max, data.getMax());
	}
}
