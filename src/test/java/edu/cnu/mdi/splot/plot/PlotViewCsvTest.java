package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * CSV support in {@link PlotView}: what the drop filter accepts, that a dropped
 * CSV file becomes the view's plot, and that the File menu offers import and
 * export. The file-chooser and error-dialog paths are modal and not exercised.
 */
class PlotViewCsvTest {

	@TempDir
	Path tempDir;

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	private static PlotView newView() {
		return onEdt(() -> new PlotView(PropertyUtils.TITLE, "CSV Test Plot", PropertyUtils.VISIBLE, Boolean.FALSE));
	}

	private File write(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file.toFile();
	}

	@Test
	void theDropFilterAcceptsCsvAndPlotFilesButNothingElse() throws IOException {
		PlotView view = newView();
		var accepts = view.getFileFilter();

		assertTrue(accepts.test(write("a.csv", "x,y\n1,2\n")));
		assertTrue(accepts.test(write("b.CSV", "x,y\n1,2\n")));
		assertTrue(accepts.test(write("c.plot.json", "{}")));
		assertTrue(accepts.test(write("d.splot.json", "{}")));
		assertFalse(accepts.test(write("e.txt", "x")));
		assertFalse(accepts.test(write("f.json", "{}")));
		assertFalse(accepts.test(new File(tempDir.toFile(), "missing.csv")), "not an existing file");
		assertFalse(accepts.test(tempDir.toFile()), "directories are not files");
	}

	@Test
	void aDroppedCsvFileBecomesTheViewsPlot() throws IOException {
		PlotView view = newView();
		PlotCanvas before = view.getPlotCanvas();
		File csv = write("dropped run.csv", "# ylabel: Counts\ntime,alpha,beta\n0,1,2\n1,3,4\n2,5,6\n");

		onEdt(() -> {
			view.filesDropped(List.of(csv));
			return null;
		});

		PlotCanvas after = view.getPlotCanvas();
		assertNotSame(before, after);
		assertEquals(PlotDataType.XYXY, after.getPlotData().getType());
		assertEquals(2, after.getPlotData().getCurves().size());
		assertEquals("alpha", after.getPlotData().getCurves().get(0).name());
		assertEquals(3, after.getPlotData().getCurves().get(1).snapshot().length());
		assertEquals("dropped run", after.getParameters().getPlotTitle());
		assertEquals("time", after.getParameters().getXLabel());
		assertEquals("Counts", after.getParameters().getYLabel());
	}

	@Test
	void theFileMenuOffersCsvImportAndExport() {
		PlotView view = newView();

		JMenuBar bar = view.getJMenuBar();
		JMenu file = null;
		for (int i = 0; i < bar.getMenuCount(); i++) {
			if ("File".equals(bar.getMenu(i).getText())) {
				file = bar.getMenu(i);
			}
		}
		assertNotNull(file);
		StringBuilder items = new StringBuilder();
		for (int i = 0; i < file.getItemCount(); i++) {
			if (file.getItem(i) != null) {
				items.append(file.getItem(i).getText()).append('|');
			}
		}
		assertTrue(items.toString().contains("Import Data (CSV)"), items.toString());
		assertTrue(items.toString().contains("Export Data (CSV)"), items.toString());
		assertTrue(items.toString().contains("Open Plot"), "existing items are kept: " + items);
	}
}
