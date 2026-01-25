package edu.cnu.mdi.splot.edit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.LineStyle;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.fit.FitResult;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.HistoCurve;
import edu.cnu.mdi.splot.pdata.HistoData;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.style.StyleEditorPanel;
import edu.cnu.mdi.ui.colors.IColorChangeListener;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Environment;

/**
 * Used to edit parameters for curves on a plot
 *
 * @author heddle
 *
 */
@SuppressWarnings("serial")
public class CurveEditorPanel extends JPanel implements ActionListener, PropertyChangeListener {

	// the underlying plot canvas
	protected PlotCanvas _plotCanvas;

	// list font
	protected static Font _listFont = Fonts.plainFontDelta(0);
	protected static Font _textFont = Fonts.plainFontDelta(-2);

	// curve table
	private CurveTable _curveTable;

	// style panel
	protected StyleEditorPanel _stylePanel;

	// fit editor
	protected FitEditorPanel _fitPanel;

	// text area for fit info
	protected JEditorPane _textArea;

	/**
	 * A panel for editing data sets
	 *
	 * @param plotCanvas the plot being edited
	 */
	public CurveEditorPanel(PlotCanvas plotCanvas) {
		// note components already created by super constructor
		_plotCanvas = plotCanvas;
		_plotCanvas.addPropertyChangeListener(this);
		setBorder(new CommonBorder());
		addContent();
	}

	@Override
	public void setEnabled(boolean enabled) {
		_fitPanel.setEnabled(enabled);
		_stylePanel.setEnabled(enabled);
	}

	// new curve has been selected
	private void curveChanged(ACurve curve) {
		// a new curve was selected, which might be null
		// set all editors accordingly

		setEnabled(curve != null);
		if (curve != null) {
			// _showCurve.setSelected(curve.isVisible());
			_stylePanel.setStyle(curve.getStyle());
			_fitPanel.setFit(curve);
			_fitPanel.fitSpecific(curve.getCurveDrawingMethod());

			syncSelectorsFromCurve(curve);
		}

		_fitPanel.reconfigure(curve);
		validate();
		_fitPanel.repaint();
		setTextArea();
	}

	/**
	 * Add the content to the panel
	 */
	protected void addContent() {
		setLayout(new BorderLayout());

		JPanel sp = new JPanel();
		sp.setLayout(new BoxLayout(sp, BoxLayout.Y_AXIS));
		sp.setAlignmentX(Component.LEFT_ALIGNMENT);
		addList(sp);
		addStyle(sp);
		addFit(sp);

		add(sp, BorderLayout.NORTH);
		addTextArea();
	}

	// add the curve list
	protected void addList(JPanel addPanel) {

		JPanel nPanel = new JPanel();
		nPanel.setLayout(new BorderLayout(0, 4));

		Collection<ACurve> curves = _plotCanvas.getPlotData().getCurves();
		final DefaultListModel<ACurve> model = new DefaultListModel<>();
		for (ACurve curve : curves) {
			model.addElement(curve);
		}

		_curveTable = new CurveTable(_plotCanvas);
		ListSelectionListener lsl = new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					ACurve curve = _curveTable.getSelectedCurve();
					// might be null!
					curveChanged(curve);
				}
			}

		};

		_curveTable.getSelectionModel().addListSelectionListener(lsl);
		JScrollPane scrollPane = _curveTable.getScrollPane();
		scrollPane.setBorder(new CommonBorder("Curves"));

		nPanel.add(scrollPane, BorderLayout.CENTER);
		addPanel.add(nPanel);
	}

	/**
	 * Select the first curve
	 */
	public void selectFirstCurve() {
		if (_curveTable != null) {
			try {
				_curveTable.getSelectionModel().setSelectionInterval(0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// add the style editor
	private void addStyle(JPanel addPanel) {
		_stylePanel = new StyleEditorPanel(_plotCanvas.getPlotData().getType());

		if (_stylePanel.getSymbolSelector() != null) {
			_stylePanel.getSymbolSelector().addActionListener(this);
		}

		if (_stylePanel.getBorderSelector() != null) {
			_stylePanel.getBorderSelector().addActionListener(this);
		}

		if (_stylePanel.getSymbolSizeSelector() != null) {
			_stylePanel.getSymbolSizeSelector().addPropertyChangeListener(this);
		}
		if (_stylePanel.getLineWidthSelector() != null) {
			_stylePanel.getLineWidthSelector().addPropertyChangeListener(this);
		}

		IColorChangeListener iccl = new IColorChangeListener() {

			@Override
			public void colorChanged(Component component, Color color) {
				ACurve curve = _curveTable.getSelectedCurve();
				if (curve != null) {

					if (component == _stylePanel.getSymbolColor()) {
						curve.getStyle().setFillColor(_stylePanel.getSymbolColor().getColor());
					} else if (component == _stylePanel.getBorderColor()) {
						curve.getStyle().setBorderColor(_stylePanel.getBorderColor().getColor());
					} else if (component == _stylePanel.getFitLineColor()) {
						curve.getStyle().setLineColor(_stylePanel.getFitLineColor().getColor());
					}
					_plotCanvas.repaint();
				}

			}

		};

		if (_stylePanel.getSymbolColor() != null) {
			_stylePanel.getSymbolColor().setColorListener(iccl);
		}
		if (_stylePanel.getBorderColor() != null) {
			_stylePanel.getBorderColor().setColorListener(iccl);
		}
		if (_stylePanel.getFitLineColor() != null) {
			_stylePanel.getFitLineColor().setColorListener(iccl);
		}

		_stylePanel.setEnabled(false);
		addPanel.add(_stylePanel);
	}

	// add the fit editor
	private void addFit(JPanel addPanel) {
		_fitPanel = new FitEditorPanel();
		_fitPanel.setEnabled(false);
		_fitPanel.getPolynomialOrderSelector().addPropertyChangeListener(this);
		_fitPanel.getNumGaussianSelector().addPropertyChangeListener(this);
		_fitPanel.getNumRMSCheckBox().addPropertyChangeListener(this);
		_fitPanel.getStatErrorCheckBox().addPropertyChangeListener(this);

		_fitPanel.getFitSelector().addActionListener(this);

		addPanel.add(_fitPanel);
	}

	// add th text area
	private void addTextArea() {

		_textArea = new JEditorPane();
		_textArea.setEditable(false);
		_textArea.setContentType("text/html");

		if (Environment.getInstance().isLinux()) {
			_textArea.setText("<body style=\"font-size:10px;color:blue\">CNU sPlot</body>");
		} else {
			_textArea.setText("<body style=\"font-size:11px;color:blue\">CNU sPlot</body>");
		}
		JScrollPane scrollPane = new JScrollPane(_textArea);
		scrollPane.setBorder(new CommonBorder("Fit Parameters"));

		Dimension d = scrollPane.getPreferredSize();
		d.height = 350;
		scrollPane.setPreferredSize(d);
		add(scrollPane, BorderLayout.CENTER);
	}

	// put the text in the text area
	private void setTextArea() {
		_textArea.setText("");
		ACurve curve = _curveTable.getSelectedCurve();
		if (curve != null) {
			FitResult fr = curve.fitResult();
			if (fr != null) {
				_textArea.setText(fr.htmlSummary());
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		ACurve curve = _curveTable.getSelectedCurve();
		if (curve == null) {
			return;
		}

		Object source = e.getSource();
		if (source == _stylePanel.getSymbolSelector()) {
			@SuppressWarnings("unchecked")
			EnumComboBox<SymbolType> ecb = (EnumComboBox<SymbolType>) source;
			SymbolType stype = ecb.getSelectedEnum(); // may be null if extraChoiceLabel used

			if (curve.getStyle().getSymbolType() != stype) {
				curve.getStyle().setSymbolType(stype);
				_plotCanvas.repaint();
			}

		} else if (source == _stylePanel.getBorderSelector()) {
			@SuppressWarnings("unchecked")
			EnumComboBox<LineStyle> ecb = (EnumComboBox<LineStyle>) source;
			LineStyle lineStyle = ecb.getSelectedEnum();

			if (curve.getStyle().getLineStyle() != lineStyle) {
				curve.getStyle().setLineStyle(lineStyle);
				_plotCanvas.repaint();
			}

		} else if (source == _fitPanel.getFitSelector()) {
			@SuppressWarnings("unchecked")
			EnumComboBox<CurveDrawingMethod> ecb = (EnumComboBox<CurveDrawingMethod>) source;
			CurveDrawingMethod fitType = ecb.getSelectedEnum();

			if (curve.getCurveDrawingMethod() != fitType) {
				curve.setCurveDrawingMethod(fitType);
				_fitPanel.fitSpecific(curve.getCurveDrawingMethod());

				_fitPanel.reconfigure(curve);
				validate();
				_fitPanel.repaint();
				_plotCanvas.repaint();
			}
		}

	}

	/**
	 * The canvas or a widget has fired a property change. This is used as a simple
	 * notification mechanism.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if (PlotCanvas.DONEDRAWINGPROP.equals(evt.getPropertyName())) {
			setTextArea();
			return;
		}

		// all other props rely on having a non null curve
		ACurve curve = _curveTable.getSelectedCurve();
		if (curve == null) {
			return;
		}

		if (StyleEditorPanel.SYMBOLSIZEPROP.equals(evt.getPropertyName())) {
			int ssize = (Integer) evt.getNewValue();
			IStyled style = curve.getStyle();
			if (style.getSymbolSize() != ssize) {
				style.setSymbolSize(ssize);
				_plotCanvas.repaint();
			}
		}

		if (StyleEditorPanel.LINEWIDTHPROP.equals(evt.getPropertyName())) {
			int lwidth = (Integer) evt.getNewValue();
			float fwidth = (lwidth / 2.f);
			IStyled style = curve.getStyle();
			System.err.println("Setting line width to: " + fwidth);
			if (style.getLineWidth() != fwidth) {
				style.setLineWidth(fwidth);
				_plotCanvas.repaint();
			}
		}

		else if (FitEditorPanel.POLYNOMIALORDERPROP.equals(evt.getPropertyName())) {
			int porder = (Integer) evt.getNewValue();
			int fitOrder = curve.getFitOrder();
			if (fitOrder != porder) {
				curve.setFitOrder(porder);
				_plotCanvas.repaint();
			}
		}

		else if (FitEditorPanel.GAUSSIANNUMPROP.equals(evt.getPropertyName())) {
			int ngauss = (Integer) evt.getNewValue();
			int fitOrder = curve.getFitOrder();
			if (fitOrder != ngauss) {
				curve.setFitOrder(ngauss);
				_plotCanvas.repaint();
			}
		}

		else if (FitEditorPanel.USERMSPROP.equals(evt.getPropertyName())) {
			if (curve.isHistogram()) {
				HistoCurve hcurve = (HistoCurve) curve;
				boolean useRMS = (Boolean) evt.getNewValue();
				HistoData hd = hcurve.getHistoData();
				hd.setRmsInHistoLegend(useRMS);
				_plotCanvas.repaint();
			}
		}

		else if (FitEditorPanel.STATERRPROP.equals(evt.getPropertyName())) {
			if (curve.isHistogram()) {
				HistoCurve hcurve = (HistoCurve) curve;
				boolean statErr = (Boolean) evt.getNewValue();
				HistoData hd = hcurve.getHistoData();
				hd.setDrawStatisticalErrors(statErr);
				_plotCanvas.repaint();
			}

		}

	}

	private void syncSelectorsFromCurve(ACurve curve) {
		if (curve == null) {
			return;
		}

		// Symbol
		if (_stylePanel.getSymbolSelector() != null) {
			EnumComboBox<SymbolType> cb = _stylePanel.getSymbolSelector();
			cb.setSelectedItem(curve.getStyle().getSymbolType());
		}

		// Line style
		if (_stylePanel.getBorderSelector() != null) {
			EnumComboBox<LineStyle> cb = _stylePanel.getBorderSelector();
			cb.setSelectedItem(curve.getStyle().getLineStyle());
		}

		// Fit / drawing method
		if (_fitPanel.getFitSelector() != null) {
			EnumComboBox<CurveDrawingMethod> cb = _fitPanel.getFitSelector();
			cb.setSelectedItem(curve.getCurveDrawingMethod());
		}
	}

}
