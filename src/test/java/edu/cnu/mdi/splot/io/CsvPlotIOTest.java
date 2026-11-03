package edu.cnu.mdi.splot.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.cnu.mdi.splot.io.CsvPlotIO.Dataset;
import edu.cnu.mdi.splot.io.CsvPlotIO.Series;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;

class CsvPlotIOTest {

	@TempDir
	Path tempDir;

	private static Dataset read(String text) throws IOException {
		return CsvPlotIO.read(new StringReader(text));
	}

	private static String write(Dataset dataset) throws IOException {
		StringWriter out = new StringWriter();
		CsvPlotIO.write(out, dataset);
		return out.toString();
	}

	private static IOException rejects(String text) {
		return assertThrows(IOException.class, () -> read(text), text);
	}

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

	// ------------------------------------------------------------ long layout

	@Test
	void longLayoutWithErrorsAndLabelComments() throws IOException {
		Dataset d = read("""
				# title: Beam profile
				# xlabel: Position (mm)
				# ylabel: Counts
				curve,x,y,e
				run 1,0.0,3.5,0.2
				run 1,1.0,4.1,0.25
				run 2,0.5,2.9,0.3
				""");

		assertEquals("Beam profile", d.title());
		assertEquals("Position (mm)", d.xLabel());
		assertEquals("Counts", d.yLabel());
		assertTrue(d.hasErrors());
		assertEquals(2, d.series().size());
		Series one = d.series().get(0);
		assertEquals("run 1", one.name());
		assertArrayEquals(new double[] { 0.0, 1.0 }, one.x());
		assertArrayEquals(new double[] { 3.5, 4.1 }, one.y());
		assertArrayEquals(new double[] { 0.2, 0.25 }, one.e());
		assertEquals("run 2", d.series().get(1).name());
		assertArrayEquals(new double[] { 2.9 }, d.series().get(1).y());
	}

	@Test
	void longLayoutWithoutErrorsHasNoErrorArrays() throws IOException {
		Dataset d = read("curve,x,y\na,1,2\na,3,4\n");

		assertFalse(d.hasErrors());
		assertNull(d.series().get(0).e());
		assertNull(d.title());
		assertNull(d.xLabel());
		assertNull(d.yLabel());
	}

	@Test
	void interleavedRowsGroupIntoCurvesInOrderOfFirstAppearance() throws IOException {
		Dataset d = read("curve,x,y\nb,1,10\na,1,20\nb,2,11\na,2,21\nc,9,9\n");

		assertEquals(List.of("b", "a", "c"), d.series().stream().map(Series::name).toList());
		assertArrayEquals(new double[] { 10, 11 }, d.series().get(0).y());
		assertArrayEquals(new double[] { 20, 21 }, d.series().get(1).y());
	}

	@Test
	void longHeaderSynonymsAndCaseAreAccepted() throws IOException {
		Dataset d = read("Name,X,Y,Error\na,1,2,0.1\n");

		assertTrue(d.hasErrors());
		assertEquals("a", d.series().get(0).name());
		assertEquals("a", read("SERIES,x,y\na,1,2\n").series().get(0).name());
		assertFalse(read("series,x,y,sigma\na,1,2,0.1\n").series().isEmpty());
		assertTrue(read("series,x,y,yerr\na,1,2,0.1\n").hasErrors());
	}

	// ------------------------------------------------------------ wide layout

	@Test
	void wideLayoutUsesTheHeaderForNamesAndTheXLabel() throws IOException {
		Dataset d = read("time,alpha,beta\n0,1,10\n1,2,20\n2,3,30\n");

		assertEquals("time", d.xLabel());
		assertEquals(List.of("alpha", "beta"), d.series().stream().map(Series::name).toList());
		assertArrayEquals(new double[] { 0, 1, 2 }, d.series().get(0).x());
		assertArrayEquals(new double[] { 10, 20, 30 }, d.series().get(1).y());
		assertFalse(d.hasErrors());
	}

	@Test
	void blankCellsInTheWideLayoutMeanNoPointForThatCurve() throws IOException {
		Dataset d = read("x,a,b\n1,10,\n2,,200\n3,30,300\n");

		Series a = d.series().get(0);
		Series b = d.series().get(1);
		assertArrayEquals(new double[] { 1, 3 }, a.x());
		assertArrayEquals(new double[] { 10, 30 }, a.y());
		assertArrayEquals(new double[] { 2, 3 }, b.x());
		assertArrayEquals(new double[] { 200, 300 }, b.y());
	}

	@Test
	void shortWideRowsAreTreatedAsBlankTrailingCells() throws IOException {
		Dataset d = read("x,a,b\n1,10\n2,20,200\n");

		assertArrayEquals(new double[] { 1, 2 }, d.series().get(0).x());
		assertArrayEquals(new double[] { 2 }, d.series().get(1).x());
	}

	@Test
	void aNumericFirstRowMeansNoHeaderAndDefaultCurveNames() throws IOException {
		Dataset d = read("1,10,100\n2,20,200\n");

		assertEquals(List.of("Curve 1", "Curve 2"), d.series().stream().map(Series::name).toList());
		assertNull(d.xLabel());
		assertArrayEquals(new double[] { 1, 2 }, d.series().get(0).x());
		assertArrayEquals(new double[] { 100, 200 }, d.series().get(1).y());
	}

	@Test
	void aBlankWideHeaderNameGetsADefault() throws IOException {
		Dataset d = read("x,,b\n1,2,3\n");

		assertEquals("Curve 1", d.series().get(0).name());
		assertEquals("b", d.series().get(1).name());
	}

	// -------------------------------------------------- comments, quoting, EOL

	@Test
	void unknownCommentsAreIgnoredAndTheFirstLabelCommentWins() throws IOException {
		Dataset d = read("""
				# generated by something
				# TITLE:   First
				# title: Second
				# note: ignored
				curve,x,y
				a,1,2
				""");

		assertEquals("First", d.title());
	}

	@Test
	void quotedNamesMayContainCommasQuotesAndLineBreaks() throws IOException {
		Dataset d = read("curve,x,y\n\"a, \"\"big\"\"\nname\",1,2\n\"  padded  \",3,4\n");

		assertEquals("a, \"big\"\nname", d.series().get(0).name());
		assertEquals("  padded  ", d.series().get(1).name());
	}

	@Test
	void crlfBomBlankLinesAndMissingFinalNewlineAreTolerated() throws IOException {
		Dataset d = read("﻿curve,x,y\r\n\r\na,1,2\r\n   \r\na,3,4");

		assertArrayEquals(new double[] { 1, 3 }, d.series().get(0).x());
		assertArrayEquals(new double[] { 2, 4 }, d.series().get(0).y());
	}

	@Test
	void whitespaceAroundUnquotedCellsIsTrimmed() throws IOException {
		Dataset d = read("curve , x , y\n a name , 1.5 , -2e3 \n");

		assertEquals("a name", d.series().get(0).name());
		assertArrayEquals(new double[] { 1.5 }, d.series().get(0).x());
		assertArrayEquals(new double[] { -2000 }, d.series().get(0).y());
	}

	// ------------------------------------------------------------- bad input

	@Test
	void emptyAndHeaderOnlyFilesAreRejected() {
		rejects("");
		rejects("   \n\n");
		rejects("# title: only a comment\n");
		assertTrue(rejects("curve,x,y\n").getMessage().contains("no data"));
		assertTrue(rejects("x,a,b\n").getMessage().contains("no data"));
	}

	@Test
	void nonNumericValuesAreReportedWithTheirLineNumber() {
		IOException e = rejects("curve,x,y\na,1,2\na,3,oops\n");

		assertTrue(e.getMessage().startsWith("Line 3:"), e.getMessage());
		assertTrue(e.getMessage().contains("oops"), e.getMessage());
	}

	@Test
	void lineNumbersCountCommentsAndBlankLines() {
		IOException e = rejects("# c\n\ncurve,x,y\na,1,2\na,bad,4\n");

		assertTrue(e.getMessage().startsWith("Line 5:"), e.getMessage());
	}

	@Test
	void nonFiniteNumbersAreRejected() {
		rejects("curve,x,y\na,1,NaN\n");
		rejects("curve,x,y\na,Infinity,2\n");
		rejects("curve,x,y\na,1,-Infinity\n");
		rejects("x,a\n1,NaN\n");
	}

	@Test
	void longRowsWithTooFewColumnsOrABlankNameAreRejected() {
		assertTrue(rejects("curve,x,y\na,1\n").getMessage().contains("expected 3 columns"));
		assertTrue(rejects("curve,x,y,e\na,1,2\n").getMessage().contains("expected 4 columns"));
		assertTrue(rejects("curve,x,y\n,1,2\n").getMessage().contains("curve name is blank"));
		rejects("curve,x,y,e\na,1,2,\n"); // errors column present but blank
	}

	@Test
	void wideProblemsAreRejected() {
		assertTrue(rejects("x,a,a\n1,2,3\n").getMessage().contains("Duplicate curve name"));
		assertTrue(rejects("x,a\n1,2,3\n").getMessage().contains("values but the header"));
		assertTrue(rejects("x,a\n,2\n").getMessage().contains("x value is missing"));
		rejects("x\n1\n"); // an x column and nothing to plot
		rejects("onlyone\nvalue\n");
	}

	@Test
	void quotingErrorsAreRejected() {
		assertTrue(rejects("curve,x,y\n\"never closed,1,2\n").getMessage().contains("never closed"));
		assertTrue(rejects("curve,x,y\n\"a\"b,1,2\n").getMessage().contains("after a closing quote"));
	}

	// --------------------------------------------------------------- writing

	@Test
	void writesTheLongLayoutExactly() throws IOException {
		Dataset d = new Dataset("T", "X (mm)", "Counts", List.of(
				new Series("run 1", new double[] { 0, 1.5 }, new double[] { 2, 3 }, new double[] { 0.1, 0.2 }),
				new Series("run 2", new double[] { 4 }, new double[] { 5 }, new double[] { 0.3 })));

		assertEquals("""
				# title: T
				# xlabel: X (mm)
				# ylabel: Counts
				curve,x,y,e
				run 1,0.0,2.0,0.1
				run 1,1.5,3.0,0.2
				run 2,4.0,5.0,0.3
				""", write(d));
	}

	@Test
	void noErrorColumnAndNoLabelCommentsWhenThereIsNothingToSay() throws IOException {
		Dataset d = new Dataset(null, " ", null,
				List.of(new Series("a", new double[] { 1 }, new double[] { 2 }, null)));

		assertEquals("curve,x,y\na,1.0,2.0\n", write(d));
	}

	@Test
	void multiLineLabelsAreFlattenedToOneCommentLine() throws IOException {
		Dataset d = new Dataset("two\nlines\r\nhere", null, null,
				List.of(new Series("a", new double[] { 1 }, new double[] { 2 }, null)));

		assertTrue(write(d).startsWith("# title: two lines here\n"));
	}

	@Test
	void datasetValidationRejectsInconsistentInput() {
		double[] one = { 1 };
		assertThrows(IllegalArgumentException.class, () -> new Dataset("t", "x", "y", List.of()));
		assertThrows(IllegalArgumentException.class, () -> new Dataset("t", "x", "y",
				List.of(new Series("a", one, one, null), new Series("a", one, one, null))));
		assertThrows(IllegalArgumentException.class, () -> new Dataset("t", "x", "y",
				List.of(new Series("a", one, one, one), new Series("b", one, one, null))));
		assertThrows(IllegalArgumentException.class, () -> new Series(" ", one, one, null));
		assertThrows(IllegalArgumentException.class, () -> new Series("a", one, new double[2], null));
		assertThrows(IllegalArgumentException.class, () -> new Series("a", one, one, new double[2]));
	}

	// ------------------------------------------------------------ round trips

	@Test
	void roundTripReproducesNamesLabelsAndEveryValueExactly() throws IOException {
		double[] x = { 0.1, 1.0 / 3.0, -0.0, 1e-300, 1.7976931348623157E308, 123456789.123456789 };
		double[] y = { -2.5, Math.PI, 0.0, 4.9e-324, -1e22, 7 };
		double[] e = { 0.01, 1e-9, 0, 3, 0.5, 1e10 };
		Dataset in = new Dataset("A, \"quoted\" title", "x label", "y label", List.of(
				new Series("plain", x, y, e),
				new Series("with, comma", new double[] { 1, 2 }, new double[] { 3, 4 }, new double[] { 5, 6 }),
				new Series("#hash", new double[] { 7 }, new double[] { 8 }, new double[] { 9 }),
				new Series("  spaced  ", new double[] { 7 }, new double[] { 8 }, new double[] { 9 }),
				new Series("multi\nline", new double[] { 7 }, new double[] { 8 }, new double[] { 9 })));

		Dataset out = read(write(in));

		assertEquals("A, \"quoted\" title", out.title());
		assertEquals("x label", out.xLabel());
		assertEquals("y label", out.yLabel());
		assertEquals(in.series().size(), out.series().size());
		for (int i = 0; i < in.series().size(); i++) {
			Series a = in.series().get(i);
			Series b = out.series().get(i);
			assertEquals(a.name(), b.name());
			assertArrayEquals(a.x(), b.x(), "x of " + a.name());
			assertArrayEquals(a.y(), b.y(), "y of " + a.name());
			assertArrayEquals(a.e(), b.e(), "e of " + a.name());
		}
		// assertArrayEquals on doubles distinguishes -0.0 from 0.0 and NaN payloads
		assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(out.series().get(0).x()[2]));
	}

	@Test
	void roundTripWithoutErrors() throws IOException {
		Dataset in = new Dataset(null, null, null, List.of(
				new Series("a", new double[] { 1, 2 }, new double[] { 3, 4 }, null),
				new Series("b", new double[] { 5 }, new double[] { 6 }, null)));

		Dataset out = read(write(in));

		assertFalse(out.hasErrors());
		assertArrayEquals(new double[] { 1, 2 }, out.series().get(0).x());
		assertArrayEquals(new double[] { 3, 4 }, out.series().get(0).y());
		assertArrayEquals(new double[] { 6 }, out.series().get(1).y());
	}

	// ------------------------------------------------------ files and canvases

	@Test
	void extensionHelpers() {
		assertTrue(CsvPlotIO.isCsvFile(new File("a.csv")));
		assertTrue(CsvPlotIO.isCsvFile(new File("a.CSV")));
		assertFalse(CsvPlotIO.isCsvFile(new File("a.csv.txt")));
		assertFalse(CsvPlotIO.isCsvFile(new File("a.plot.json")));
		assertFalse(CsvPlotIO.isCsvFile(null));

		assertEquals(new File("dir", "a.csv"), CsvPlotIO.ensureCsvExtension(new File("dir", "a")));
		assertEquals(new File("dir", "a.CSV"), CsvPlotIO.ensureCsvExtension(new File("dir", "a.CSV")));
		assertNull(CsvPlotIO.ensureCsvExtension(null));
	}

	@Test
	void importingAFileBuildsAPlotTitledAfterTheFile() throws IOException {
		Path file = tempDir.resolve("my run.csv");
		Files.writeString(file, "time,a,b\n0,1,2\n1,3,4\n", StandardCharsets.UTF_8);

		PlotCanvas canvas = CsvPlotIO.loadCanvas(file.toFile());

		assertEquals("my run", canvas.getParameters().getPlotTitle());
		assertEquals("time", canvas.getParameters().getXLabel());
		assertEquals("Y", canvas.getParameters().getYLabel());
		assertEquals(PlotDataType.XYXY, canvas.getPlotData().getType());
		assertEquals(2, canvas.getPlotData().getCurves().size());
		assertEquals(2, canvas.getPlotData().getCurves().get(0).snapshot().length());
	}

	@Test
	void labelCommentsOverrideTheFileNameAndHeaderDefaults() throws IOException {
		Path file = tempDir.resolve("data.csv");
		Files.writeString(file, "# title: Real title\n# xlabel: Seconds\n# ylabel: Volts\nx,a\n1,2\n");

		PlotCanvas canvas = CsvPlotIO.loadCanvas(file.toFile());

		assertEquals("Real title", canvas.getParameters().getPlotTitle());
		assertEquals("Seconds", canvas.getParameters().getXLabel());
		assertEquals("Volts", canvas.getParameters().getYLabel());
	}

	@Test
	void errorsMakeAnErrorBarPlot() throws IOException {
		Path file = tempDir.resolve("err.csv");
		Files.writeString(file, "curve,x,y,e\na,1,2,0.5\n");

		PlotCanvas canvas = CsvPlotIO.loadCanvas(file.toFile());

		assertEquals(PlotDataType.XYEXYE, canvas.getPlotData().getType());
		assertArrayEquals(new double[] { 0.5 }, canvas.getPlotData().getCurves().get(0).snapshot().e);
	}

	@Test
	void aPlotSurvivesExportAndReimport() throws IOException {
		Path source = tempDir.resolve("source.csv");
		Files.writeString(source, "# title: Round trip\n# xlabel: X\n# ylabel: Y\n"
				+ "curve,x,y,e\nfirst,0.1,0.2,0.01\nfirst,0.3,0.4,0.02\nsecond,5,6,0.5\n");
		PlotCanvas original = CsvPlotIO.loadCanvas(source.toFile());

		Path exported = tempDir.resolve("exported.csv");
		CsvPlotIO.save(original, exported.toFile());
		PlotCanvas again = CsvPlotIO.loadCanvas(exported.toFile());

		assertEquals("Round trip", again.getParameters().getPlotTitle());
		assertEquals(2, again.getPlotData().getCurves().size());
		for (int i = 0; i < 2; i++) {
			var a = original.getPlotData().getCurves().get(i);
			var b = again.getPlotData().getCurves().get(i);
			assertEquals(a.name(), b.name());
			assertArrayEquals(a.snapshot().x, b.snapshot().x);
			assertArrayEquals(a.snapshot().y, b.snapshot().y);
			assertArrayEquals(a.snapshot().e, b.snapshot().e);
		}
	}

	@Test
	void duplicateCurveNamesInAPlotAreMadeUniqueOnExport() throws IOException {
		PlotCanvas canvas = onEdt(() -> {
			try {
				PlotData data = new PlotData(PlotDataType.XYXY, new String[] { "same", "same" }, null);
				return new PlotCanvas(data, "t", "x", "y");
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		});
		Dataset d = CsvPlotIO.fromCanvas(canvas);

		assertEquals(List.of("same", "same (2)"), d.series().stream().map(Series::name).toList());
	}

	@Test
	void histogramPlotsCannotBeExported() {
		PlotCanvas canvas = onEdt(() -> {
			try {
				PlotData data = new PlotData(new HistoData("h", 0, 1, 10));
				return new PlotCanvas(data, "t", "x", "y");
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		});

		IOException e = assertThrows(IOException.class, () -> CsvPlotIO.fromCanvas(canvas));

		assertTrue(e.getMessage().contains("XY plots only"), e.getMessage());
	}

	@Test
	void importingAMalformedFileReportsAnIOException() throws IOException {
		Path file = tempDir.resolve("bad.csv");
		Files.writeString(file, "curve,x,y\na,1,not-a-number\n");

		assertThrows(IOException.class, () -> CsvPlotIO.loadCanvas(file.toFile()));
	}
}
