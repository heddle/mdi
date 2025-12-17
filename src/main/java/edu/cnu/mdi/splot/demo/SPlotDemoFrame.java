package edu.cnu.mdi.splot.demo;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SPlotDemoFrame extends JFrame {

    private final JPanel center = new JPanel(new BorderLayout());
    private final List<AExamplePlot> examples;

    public SPlotDemoFrame() {
        super("splot demo gallery");

        this.examples = loadExamples();

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());

        if (!examples.isEmpty()) {
            showExample(examples.get(0));
        }

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu demo = new JMenu("Examples");
        for (AExamplePlot ex : examples) {
            JMenuItem item = new JMenuItem(ex.getDisplayName());
            String tip = ex.getDescription();
            if (tip != null && !tip.isBlank()) item.setToolTipText(tip);
            item.addActionListener(e -> showExample(ex));
            demo.add(item);
        }

        bar.add(demo);
        return bar;
    }

    private void showExample(AExamplePlot ex) {
        center.removeAll();
        center.add(ex.buildComponent(), BorderLayout.CENTER);
        setTitle("splot demo: " + ex.getDisplayName());
        center.revalidate();
        center.repaint();
    }

    /**
     * Load examples.
     * <p>
     * Preferred: ServiceLoader (automatic discovery).
     * Fallback: hardwired list.
     * </p>
     */
    private static List<AExamplePlot> loadExamples() {
        List<AExamplePlot> list = new ArrayList<>();

        // 1) ServiceLoader-based discovery (recommended)
        ServiceLoader.load(AExamplePlot.class).forEach(list::add);

        // 2) Hardwired fallback if none discovered
        if (list.isEmpty()) {
            list.add(new GaussianFitExample());
            list.add(new TwoGaussianFitsExample());
            list.add(new PowerLawExample());
           // add more here later
        }

        list.sort(Comparator.comparing(AExamplePlot::getDisplayName));
        return list;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new SPlotDemoFrame().setVisible(true));
    }
}
