package edu.cnu.mdi.splot.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.splot.plot.PlotParameters;
import edu.cnu.mdi.swing.WindowPlacement;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Demonstrates placing a PlotPanel directly on a Swing dialog.
 */
@SuppressWarnings("serial")
public class PlotDialogExample extends JDialog {

	/**
	 * Create the plot dialog.
	 *
	 * @throws PlotDataException if the plot data cannot be created
	 */
	public PlotDialogExample() throws PlotDataException {
		super((Frame) null, "Plot Dialog Example", false);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		PlotPanel panel = createPlotPanel();
		panel.setPreferredSize(new Dimension(700, 600));

		add(panel, BorderLayout.CENTER);

		pack();
		WindowPlacement.centerComponent(this);
	}

	/**
	 * Create the plot shown in Listing 6-4.
	 *
	 * @return the completed plot panel
	 * @throws PlotDataException if the plot data cannot be created
	 */
	private PlotPanel createPlotPanel() throws PlotDataException {

		PlotData plotData = new PlotData(
				PlotDataType.XYXY,
				new String[] { "Measured signal" },
				null);

		Curve curve = (Curve) plotData.getCurve(0);
		curve.add(0.0, 0.2);
		curve.add(1.0, 1.1);
		curve.add(2.0, 1.8);
		curve.add(3.0, 3.2);
		curve.add(4.0, 4.1);

		curve.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
		curve.getStyle().setFillColor(Color.darkGray);
		curve.getStyle().setBorderColor(Color.black);

		PlotCanvas canvas = new PlotCanvas(
				plotData,
				"Sample XY Plot",
				"time (s)",
				"signal (V)");

		PlotParameters params = canvas.getParameters();
		params.includeYZero(true);
		params.setNumDecimalX(2);
		params.setNumDecimalY(2);

		return new PlotPanel(canvas);
	}

	/**
	 * Run the example as a standalone application.
	 *
	 * @param args ignored command-line arguments
	 */
	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			initializeUI();

			try {
				PlotDialogExample dialog = new PlotDialogExample();
				dialog.setVisible(true);
			}
			catch (PlotDataException e) {
				edu.cnu.mdi.log.Log.getInstance().exception(e);

				JOptionPane.showMessageDialog(
						null,
						"Could not create the sample plot:\n" + e.getMessage(),
						"Plot Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Initialize the standalone application's look and feel.
	 */
	private static void initializeUI() {
		FlatIntelliJLaf.setup();

		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);

		Fonts.refresh();
	}
}
