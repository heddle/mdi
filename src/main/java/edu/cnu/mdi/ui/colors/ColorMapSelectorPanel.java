package edu.cnu.mdi.ui.colors;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ColorMapSelectorPanel extends JPanel {

    private final JComboBox<ScientificColorMap> _combo;
    private final ColorScaleBar _scaleBar;
    private ScientificColorMap _currentMap;

    /**
     * Create a panel for selecting a scientific color map.
     */
    public ColorMapSelectorPanel(ScientificColorMap initialMap) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));

        _currentMap = initialMap;

        _combo = new JComboBox<>(ScientificColorMap.values());
        _combo.setSelectedItem(initialMap);
        add(_combo);

        _scaleBar = new ColorScaleBar(initialMap);
        _scaleBar.setPreferredSize(new Dimension(100, 20));
        add(_scaleBar);

        _combo.addActionListener(e -> {
            ScientificColorMap map = (ScientificColorMap) _combo.getSelectedItem();
            if (map != null) {
            	_currentMap = map;
                applyColorMap(map);
            }
        });
    }

    /**
	 * Get the currently selected color map.
	 *
	 * @return the current ScientificColorMap
	 */
    public ScientificColorMap getCurrentMap() {
		return _currentMap;
	}


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _combo.setEnabled(enabled);
        _scaleBar.setEnabled(enabled);
    }

    // apply on EDT
    private void applyColorMap(ScientificColorMap map) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> applyColorMap(map));
            return;
        }
        _scaleBar.setColorMap(map);
    }

    public ScientificColorMap getSelectedColorMap() {
        return (ScientificColorMap) _combo.getSelectedItem();
    }
}
