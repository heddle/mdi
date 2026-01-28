package edu.cnu.mdi.splot.example;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.splot.plot.MultiplotPanel;
import edu.cnu.mdi.ui.fonts.Fonts;

public class MultiplotPanelDemo extends JFrame {
	
	protected final  MultiplotPanel multiplotPanel;

	public MultiplotPanelDemo() {
		setTitle("Multiplot Panel Demo");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		UIInit();
		
		multiplotPanel = new MultiplotPanel(true);
		add(multiplotPanel);
		setSize(800, 600);
	}
	
	
    //initialize the FlatLaf UI
	private void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MultiplotPanelDemo demo = new MultiplotPanelDemo();
				createGallery(demo.multiplotPanel);
				demo.setVisible(true);
			}
		});
	}
	
	private static void createGallery(MultiplotPanel mpp) {
		Gaussian gaussian = new Gaussian(true);
		mpp.addPlot("Gaussian", gaussian.getPlotPanel());
		
		ErfTest erfTest = new ErfTest(true);
		mpp.addPlot("Erf", erfTest.getPlotPanel());
		
		CubicLogLog cubicLogLog = new CubicLogLog(true);
		mpp.addPlot("Log-Log", cubicLogLog.getPlotPanel());
		
		TwoHisto twoHisto = new TwoHisto(true);
		mpp.addPlot("Histograms", twoHisto.getPlotPanel());
		
	}
}
