package edu.cnu.mdi.splot.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.pdata.Snapshot;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotParameters;

/**
 * Import and export of XY curve data as CSV, for exchange with tools that do
 * not know the {@code .plot.json} format (spreadsheets, pandas, gnuplot, R).
 *
 * <p>CSV carries <em>data only</em>: curve names, their points and optional
 * errors, plus the plot title and axis labels as optional header comments.
 * Styling, axis limits, fits and everything else belong to the
 * {@code .plot.json} format ({@link PlotIO}); importing a CSV therefore
 * produces a plot with default formatting. Only XY plots are supported;
 * histograms are not.</p>
 *
 * <h2>Export format</h2>
 * <p>The "long" layout, one point per row, which also handles curves with
 * different x values:</p>
 * <pre>
 * # title: Beam profile
 * # xlabel: Position (mm)
 * # ylabel: Counts
 * curve,x,y,e
 * run 1,0.0,3.5,0.2
 * run 1,1.0,4.1,0.2
 * run 2,0.0,2.9,0.3
 * </pre>
 * <p>The {@code e} column (the y error) appears only when the plot has errors.
 * Numbers are written with full precision, so a save followed by a load
 * reproduces the data exactly.</p>
 *
 * <h2>Accepted on import</h2>
 * <ul>
 *   <li><b>Long layout</b> as above. The header must be
 *       {@code curve,x,y} or {@code curve,x,y,e} ({@code name} or
 *       {@code series} may replace {@code curve}). Rows with the same curve
 *       name form one curve, in order of first appearance.</li>
 *   <li><b>Wide layout</b>: a header row {@code x,name1,name2,...} with one
 *       shared x column and one column per curve. A blank cell means "no
 *       point for this curve at this x".</li>
 *   <li><b>No header</b>: if the first row is numeric the file is read as the
 *       wide layout, with curves named {@code Curve 1}, {@code Curve 2}, ...</li>
 * </ul>
 * <p>Fields may be quoted per RFC 4180. Lines starting with {@code #} are
 * comments; {@code title:}, {@code xlabel:} and {@code ylabel:} comments set
 * the plot's labels and any other comment is ignored. Values must be finite
 * numbers; anything else is rejected with the offending line number rather
 * than guessed at.</p>
 */
public final class CsvPlotIO {

	/** File extension, including the dot. */
	public static final String EXTENSION = ".csv";

	/**
	 * The {@code edu.cnu.mdi.dialog.FileDialogs} directory-history purpose for
	 * CSV import/export dialogs, kept separate from plot-file dialogs.
	 */
	public static final String DIALOG_PURPOSE = "splot-csv";

	private static final Pattern META = Pattern.compile("^#\\s*(title|xlabel|ylabel)\\s*:\\s*(.*)$",
			Pattern.CASE_INSENSITIVE);

	private static final Set<String> CURVE_HEADERS = Set.of("curve", "name", "series");
	private static final Set<String> ERROR_HEADERS = Set.of("e", "error", "ey", "yerr", "sigma");

	private CsvPlotIO() {
	}

	// ------------------------------------------------------------------
	// Model
	// ------------------------------------------------------------------

	/**
	 * One curve's data.
	 *
	 * @param name curve name; non-blank
	 * @param x    x values
	 * @param y    y values, the same length as {@code x}
	 * @param e    y errors, the same length as {@code x}, or {@code null} for none
	 */
	public record Series(String name, double[] x, double[] y, double[] e) {
		/** Validates the record; the arrays are not copied. */
		public Series {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(x, "x");
			Objects.requireNonNull(y, "y");
			if (name.isBlank()) {
				throw new IllegalArgumentException("A curve name must not be blank.");
			}
			if (x.length != y.length || (e != null && e.length != x.length)) {
				throw new IllegalArgumentException("x, y and e must have the same length (curve '" + name + "').");
			}
		}
	}

	/**
	 * The contents of a CSV file.
	 *
	 * @param title  plot title, or {@code null} if not given
	 * @param xLabel x-axis label, or {@code null} if not given
	 * @param yLabel y-axis label, or {@code null} if not given
	 * @param series the curves; at least one, with unique names, and either all
	 *               or none with errors
	 */
	public record Dataset(String title, String xLabel, String yLabel, List<Series> series) {
		/** Validates and defensively copies the series list. */
		public Dataset {
			Objects.requireNonNull(series, "series");
			if (series.isEmpty()) {
				throw new IllegalArgumentException("A dataset needs at least one curve.");
			}
			series = List.copyOf(series);
			Set<String> names = new HashSet<>();
			boolean withErrors = series.get(0).e() != null;
			for (Series s : series) {
				if (!names.add(s.name())) {
					throw new IllegalArgumentException("Duplicate curve name '" + s.name() + "'.");
				}
				if ((s.e() != null) != withErrors) {
					throw new IllegalArgumentException("Either all curves or none may have errors.");
				}
			}
		}

		/** @return {@code true} if the curves carry y errors */
		public boolean hasErrors() {
			return series.get(0).e() != null;
		}
	}

	// ------------------------------------------------------------------
	// Files and plots
	// ------------------------------------------------------------------

	/**
	 * Tests the file name for a {@code .csv} extension (case-insensitive).
	 *
	 * @param f the file; may be {@code null}
	 * @return {@code true} if it names a CSV file
	 */
	public static boolean isCsvFile(File f) {
		return f != null && f.getName().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
	}

	/**
	 * Returns {@code f} unchanged if it already has a {@code .csv} extension,
	 * otherwise a file with {@code .csv} appended.
	 *
	 * @param f the file; may be {@code null}
	 * @return a file with a CSV extension, or {@code null} if {@code f} is
	 *         {@code null}
	 */
	public static File ensureCsvExtension(File f) {
		if (f == null || isCsvFile(f)) {
			return f;
		}
		return new File(f.getParentFile(), f.getName() + EXTENSION);
	}

	/**
	 * Writes the plot's XY curves to a CSV file. Safe to call from any thread.
	 *
	 * @param canvas the plot to export
	 * @param file   the destination
	 * @throws IOException if the plot does not hold XY data or the write fails
	 */
	public static void save(PlotCanvas canvas, File file) throws IOException {
		Objects.requireNonNull(file, "file");
		Dataset dataset = fromCanvas(canvas);
		try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			write(bw, dataset);
		}
	}

	/**
	 * Reads a CSV file and builds a new plot from it. The plot title defaults
	 * to the file name without its extension, and the axis labels to the x
	 * column's header (or "X") and "Y". Construction happens on the event
	 * thread.
	 *
	 * @param file the CSV file
	 * @return a new, un-started canvas
	 * @throws IOException if the file cannot be read or is not valid CSV data
	 */
	public static PlotCanvas loadCanvas(File file) throws IOException {
		Objects.requireNonNull(file, "file");
		Dataset dataset;
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			dataset = read(reader);
		}
		String name = file.getName();
		if (isCsvFile(file)) {
			name = name.substring(0, name.length() - EXTENSION.length());
		}
		return toCanvas(dataset, name);
	}

	/**
	 * Extracts the XY curves and labels from a plot.
	 *
	 * @param canvas the plot
	 * @return the plot's data; curve names are made unique if necessary
	 * @throws IOException if the plot does not hold XY data
	 */
	public static Dataset fromCanvas(PlotCanvas canvas) throws IOException {
		Objects.requireNonNull(canvas, "canvas");
		PlotData data = canvas.getPlotData();
		PlotDataType type = data.getType();
		if (type != PlotDataType.XYXY && type != PlotDataType.XYEXYE && type != PlotDataType.STRIP) {
			throw new IOException("CSV export supports XY plots only; this plot holds " + type + " data.");
		}

		boolean withErrors = type == PlotDataType.XYEXYE;
		List<Series> series = new ArrayList<>();
		Set<String> used = new HashSet<>();
		int index = 0;
		for (ACurve curve : data.getCurves()) {
			index++;
			String name = (curve.name() == null || curve.name().isBlank()) ? "Curve " + index : curve.name();
			String unique = name;
			for (int n = 2; !used.add(unique); n++) {
				unique = name + " (" + n + ")";
			}
			Snapshot snap = curve.snapshot();
			series.add(new Series(unique, snap.x, snap.y, withErrors ? snap.e : null));
		}
		if (series.isEmpty()) {
			throw new IOException("The plot has no curves to export.");
		}

		PlotParameters params = canvas.getParameters();
		return new Dataset(params.getPlotTitle(), params.getXLabel(), params.getYLabel(), series);
	}

	/**
	 * Builds a plot from a dataset. Runs on the event thread.
	 *
	 * @param dataset      the data
	 * @param defaultTitle title used when the dataset has none
	 * @return a new canvas
	 * @throws IOException if the plot cannot be built
	 */
	public static PlotCanvas toCanvas(Dataset dataset, String defaultTitle) throws IOException {
		Objects.requireNonNull(dataset, "dataset");
		PlotCanvas[] out = new PlotCanvas[1];
		Exception[] err = new Exception[1];

		Runnable build = () -> {
			try {
				String[] names = dataset.series().stream().map(Series::name).toArray(String[]::new);
				PlotDataType type = dataset.hasErrors() ? PlotDataType.XYEXYE : PlotDataType.XYXY;
				PlotData data = new PlotData(type, names, null);
				for (int i = 0; i < names.length; i++) {
					Series s = dataset.series().get(i);
					Curve curve = (Curve) data.getCurves().get(i);
					if (s.e() != null) {
						curve.addAll(s.x(), s.y(), s.e());
					} else {
						curve.addAll(s.x(), s.y());
					}
				}
				String title = dataset.title() != null ? dataset.title() : defaultTitle;
				String xl = dataset.xLabel() != null ? dataset.xLabel() : "X";
				String yl = dataset.yLabel() != null ? dataset.yLabel() : "Y";
				out[0] = new PlotCanvas(data, title, xl, yl);
			} catch (PlotDataException | RuntimeException e) {
				err[0] = e;
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			build.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(build);
			} catch (Exception e) {
				throw new IOException("Failed to build the plot on the event thread.", e);
			}
		}
		if (err[0] != null) {
			throw new IOException("Failed to build the plot: " + err[0].getMessage(), err[0]);
		}
		return out[0];
	}

	// ------------------------------------------------------------------
	// Writing
	// ------------------------------------------------------------------

	/**
	 * Writes a dataset in the long layout.
	 *
	 * @param writer the destination; not closed
	 * @param dataset the data
	 * @throws IOException if writing fails
	 */
	public static void write(Writer writer, Dataset dataset) throws IOException {
		Objects.requireNonNull(writer, "writer");
		Objects.requireNonNull(dataset, "dataset");

		comment(writer, "title", dataset.title());
		comment(writer, "xlabel", dataset.xLabel());
		comment(writer, "ylabel", dataset.yLabel());

		boolean withErrors = dataset.hasErrors();
		writer.write(withErrors ? "curve,x,y,e\n" : "curve,x,y\n");
		for (Series s : dataset.series()) {
			String name = quote(s.name());
			for (int i = 0; i < s.x().length; i++) {
				writer.write(name);
				writer.write(',');
				writer.write(Double.toString(s.x()[i]));
				writer.write(',');
				writer.write(Double.toString(s.y()[i]));
				if (withErrors) {
					writer.write(',');
					writer.write(Double.toString(s.e()[i]));
				}
				writer.write('\n');
			}
		}
	}

	private static void comment(Writer writer, String key, String value) throws IOException {
		if (value != null && !value.isBlank()) {
			writer.write("# " + key + ": " + value.replaceAll("[\\r\\n]+", " ").trim() + "\n");
		}
	}

	/** Quotes a field if it contains a delimiter, quote, line break, edge space or leading '#'. */
	private static String quote(String field) {
		boolean needs = field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0
				|| field.indexOf('\r') >= 0 || field.startsWith("#") || !field.equals(field.trim());
		return needs ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
	}

	// ------------------------------------------------------------------
	// Reading
	// ------------------------------------------------------------------

	/** One parsed CSV record and the (1-based) line it started on. */
	private record Row(int line, List<String> cells) {
	}

	/**
	 * Parses CSV text in any of the accepted layouts.
	 *
	 * @param reader the source; not closed
	 * @return the data
	 * @throws IOException if the text is malformed or holds no data
	 */
	public static Dataset read(Reader reader) throws IOException {
		Objects.requireNonNull(reader, "reader");
		StringWriter sw = new StringWriter();
		reader.transferTo(sw);
		String text = sw.toString();
		if (text.startsWith("﻿")) {
			text = text.substring(1);
		}

		Map<String, String> meta = new LinkedHashMap<>();
		List<Row> rows = tokenize(text, meta);
		if (rows.isEmpty()) {
			throw new IOException("The CSV file contains no data.");
		}

		Row first = rows.get(0);
		boolean headerless = isNumericRow(first.cells());
		List<Series> series;
		String xHeader = null;

		if (!headerless && isLongHeader(first.cells())) {
			series = readLong(rows.subList(1, rows.size()), first.cells().size() == 4);
		} else {
			List<Row> body = headerless ? rows : rows.subList(1, rows.size());
			List<String> names = new ArrayList<>();
			int columns = first.cells().size();
			if (columns < 2) {
				throw new IOException("Line " + first.line()
						+ ": expected an x column and at least one y column, or a 'curve,x,y' header.");
			}
			for (int c = 1; c < columns; c++) {
				String name = headerless ? "" : first.cells().get(c);
				names.add(name.isEmpty() ? "Curve " + c : name);
			}
			if (!headerless && !first.cells().get(0).isEmpty()) {
				xHeader = first.cells().get(0);
			}
			series = readWide(body, names);
		}

		if (series.stream().allMatch(s -> s.x().length == 0)) {
			throw new IOException("The CSV file has a header but no data rows.");
		}
		return new Dataset(meta.get("title"), meta.containsKey("xlabel") ? meta.get("xlabel") : xHeader,
				meta.get("ylabel"), series);
	}

	/** {@code curve,x,y} or {@code curve,x,y,e}, with the accepted header synonyms. */
	private static boolean isLongHeader(List<String> cells) {
		int n = cells.size();
		if (n != 3 && n != 4) {
			return false;
		}
		if (!CURVE_HEADERS.contains(lower(cells.get(0))) || !lower(cells.get(1)).equals("x")
				|| !lower(cells.get(2)).equals("y")) {
			return false;
		}
		return n == 3 || ERROR_HEADERS.contains(lower(cells.get(3)));
	}

	private static String lower(String s) {
		return s.trim().toLowerCase(Locale.ROOT);
	}

	private static List<Series> readLong(List<Row> rows, boolean withErrors) throws IOException {
		int needed = withErrors ? 4 : 3;
		Map<String, DoubleBuffer[]> byName = new LinkedHashMap<>();
		for (Row row : rows) {
			List<String> c = row.cells();
			if (c.size() < needed) {
				throw new IOException("Line " + row.line() + ": expected " + needed + " columns but found " + c.size()
						+ ".");
			}
			String name = c.get(0);
			if (name.isEmpty()) {
				throw new IOException("Line " + row.line() + ": the curve name is blank.");
			}
			DoubleBuffer[] cols = byName.computeIfAbsent(name,
					k -> new DoubleBuffer[] { new DoubleBuffer(), new DoubleBuffer(), new DoubleBuffer() });
			cols[0].add(number(c.get(1), row.line(), "x"));
			cols[1].add(number(c.get(2), row.line(), "y"));
			if (withErrors) {
				cols[2].add(number(c.get(3), row.line(), "e"));
			}
		}
		List<Series> out = new ArrayList<>();
		for (Map.Entry<String, DoubleBuffer[]> entry : byName.entrySet()) {
			DoubleBuffer[] cols = entry.getValue();
			out.add(new Series(entry.getKey(), cols[0].toArray(), cols[1].toArray(),
					withErrors ? cols[2].toArray() : null));
		}
		return out;
	}

	private static List<Series> readWide(List<Row> rows, List<String> names) throws IOException {
		Set<String> seen = new HashSet<>();
		for (String name : names) {
			if (!seen.add(name)) {
				throw new IOException("Duplicate curve name '" + name + "' in the header.");
			}
		}
		int count = names.size();
		DoubleBuffer[] xs = new DoubleBuffer[count];
		DoubleBuffer[] ys = new DoubleBuffer[count];
		for (int j = 0; j < count; j++) {
			xs[j] = new DoubleBuffer();
			ys[j] = new DoubleBuffer();
		}
		for (Row row : rows) {
			List<String> c = row.cells();
			if (c.size() > count + 1) {
				throw new IOException("Line " + row.line() + ": " + c.size() + " values but the header has only "
						+ (count + 1) + " columns.");
			}
			if (c.get(0).isEmpty()) {
				throw new IOException("Line " + row.line() + ": the x value is missing.");
			}
			double x = number(c.get(0), row.line(), "x");
			for (int j = 0; j < count && j + 1 < c.size(); j++) {
				String cell = c.get(j + 1);
				if (cell.isEmpty()) {
					continue; // no point for this curve at this x
				}
				xs[j].add(x);
				ys[j].add(number(cell, row.line(), names.get(j)));
			}
		}
		List<Series> out = new ArrayList<>();
		for (int j = 0; j < count; j++) {
			out.add(new Series(names.get(j), xs[j].toArray(), ys[j].toArray(), null));
		}
		return out;
	}

	/** A row is numeric (and so headerless) when its first cell and every non-blank cell are numbers. */
	private static boolean isNumericRow(List<String> cells) {
		if (cells.size() < 2 || !isNumber(cells.get(0))) {
			return false;
		}
		for (String cell : cells) {
			if (!cell.isEmpty() && !isNumber(cell)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isNumber(String s) {
		try {
			return Double.isFinite(Double.parseDouble(s.trim()));
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static double number(String s, int line, String what) throws IOException {
		try {
			double d = Double.parseDouble(s.trim());
			if (Double.isFinite(d)) {
				return d;
			}
		} catch (NumberFormatException e) {
			// reported below
		}
		throw new IOException("Line " + line + ": '" + s + "' is not a finite number (" + what + ").");
	}

	// ------------------------------------------------------------------
	// Tokenizing (RFC 4180 with # comments)
	// ------------------------------------------------------------------

	/**
	 * Splits the text into records. Comment lines feed {@code meta} (first
	 * occurrence of each key wins) and blank records are dropped. Unquoted
	 * cells are trimmed; quoted cells are kept verbatim.
	 */
	private static List<Row> tokenize(String text, Map<String, String> meta) throws IOException {
		List<Row> rows = new ArrayList<>();
		int n = text.length();
		int i = 0;
		int line = 1;

		while (i < n) {
			char c = text.charAt(i);
			if (c == '\r' || c == '\n') { // blank line
				if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
					i++;
				}
				i++;
				line++;
				continue;
			}
			if (c == '#') { // comment line
				int end = i;
				while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
					end++;
				}
				Matcher m = META.matcher(text.substring(i, end).trim());
				if (m.matches()) {
					meta.putIfAbsent(m.group(1).toLowerCase(Locale.ROOT), m.group(2).trim());
				}
				i = end;
				continue;
			}

			int startLine = line;
			List<String> cells = new ArrayList<>();
			StringBuilder cell = new StringBuilder();
			boolean inQuotes = false;
			boolean wasQuoted = false;
			boolean closed = false;

			while (true) {
				if (i >= n) {
					if (inQuotes) {
						throw new IOException("Line " + startLine + ": a quoted field is never closed.");
					}
					cells.add(cellText(cell, wasQuoted));
					break;
				}
				c = text.charAt(i);
				if (inQuotes) {
					if (c == '"') {
						if (i + 1 < n && text.charAt(i + 1) == '"') {
							cell.append('"');
							i += 2;
						} else {
							inQuotes = false;
							closed = true;
							i++;
						}
					} else {
						if (c == '\n') {
							line++;
						}
						cell.append(c);
						i++;
					}
				} else if (c == ',') {
					cells.add(cellText(cell, wasQuoted));
					cell.setLength(0);
					wasQuoted = false;
					closed = false;
					i++;
				} else if (c == '\r' || c == '\n') {
					cells.add(cellText(cell, wasQuoted));
					if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
						i++;
					}
					i++;
					line++;
					break;
				} else if (closed) {
					throw new IOException("Line " + line + ": unexpected character after a closing quote.");
				} else if (c == '"' && cell.toString().isBlank()) {
					cell.setLength(0);
					inQuotes = true;
					wasQuoted = true;
					i++;
				} else {
					cell.append(c);
					i++;
				}
			}

			if (cells.stream().anyMatch(s -> !s.isEmpty())) {
				rows.add(new Row(startLine, cells));
			}
		}
		return rows;
	}

	private static String cellText(StringBuilder cell, boolean quoted) {
		return quoted ? cell.toString() : cell.toString().trim();
	}

	/** Minimal growable {@code double[]}, to avoid boxing large columns. */
	private static final class DoubleBuffer {
		private double[] values = new double[16];
		private int size;

		void add(double d) {
			if (size == values.length) {
				values = java.util.Arrays.copyOf(values, size * 2);
			}
			values[size++] = d;
		}

		double[] toArray() {
			return java.util.Arrays.copyOf(values, size);
		}
	}
}
