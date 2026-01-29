package edu.cnu.mdi.ui.colors;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ColorMapSelectorPanel extends JPanel {

    /** Supported color map names */
    public enum ColorMapType {
        VIRIDIS("Viridis"),
        MAGMA("Magma"),
        INFERNO("Inferno"),
        PLASMA("Plasma"),
        TURBO("Turbo");

        private final String label;

        ColorMapType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    
    public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_combo.setEnabled(enabled);
		_scaleBar.setEnabled(enabled);
	}

    private final JComboBox<ColorMapType> _combo;
    private final ColorScaleBar _scaleBar;

    public ColorMapSelectorPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));

  
        _combo = new JComboBox<>(ColorMapType.values());
        _combo.setSelectedItem(ColorMapType.VIRIDIS); // default
        add(_combo);

        _scaleBar = new ColorScaleBar(ScientificColorMaps.VIRIDIS);
        _scaleBar.setPreferredSize(new Dimension(100, 20));
        add(_scaleBar);

        // Initialize to default
    //    applyColorMap(ColorMapType.VIRIDIS);

        // --- Wiring ---
        _combo.addActionListener(e -> {
            ColorMapType type = (ColorMapType) _combo.getSelectedItem();
            if (type != null) {
                applyColorMap(type);
            }
        });
    }

    /**
     * Apply the selected color map to the scale bar.
     */
    private void applyColorMap(ColorMapType type) {
        // Ensure EDT safety
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> applyColorMap(type));
            return;
        }

        switch (type) {
        case VIRIDIS:
            _scaleBar.setScale(ScientificColorMaps.VIRIDIS);
            break;
        case MAGMA:
            _scaleBar.setScale(ScientificColorMaps.MAGMA);
            break;
        case INFERNO:
            _scaleBar.setScale(ScientificColorMaps.INFERNO);
            break;
        case PLASMA:
            _scaleBar.setScale(ScientificColorMaps.PLASMA);
            break;
        case TURBO:
            _scaleBar.setScale(ScientificColorMaps.TURBO);
            break;
        }

        _scaleBar.repaint();
    }

    /**
     * Optional convenience accessor if other code wants the selection.
     */
    public ColorMapType getSelectedColorMap() {
        return (ColorMapType) _combo.getSelectedItem();
    }
}
