package edu.cnu.mdi.mapping.layer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.NumberFormatter;

import edu.cnu.mdi.dialog.ButtonPanel;
import edu.cnu.mdi.swing.SwingSizingUtils;
import edu.cnu.mdi.ui.colors.ColorButton;
import edu.cnu.mdi.util.Bits;

/**
 * Generic capability-driven editor for common map-layer style properties.
 */
@SuppressWarnings("serial")
public class MapLayerStyleDialog extends JDialog {

    private final long styleBits;
    private final MapLayerStyle workingStyle;

    private ColorButton fillColorButton;
    private ColorButton boundaryColorButton;
    private ColorButton pointColorButton;
    private ColorButton labelColorButton;

    private JFormattedTextField lineWidthField;
    private JFormattedTextField labelFontSizeField;
    private JFormattedTextField pointSizeField;

    private JSlider opacitySlider;
    private JLabel opacityValueLabel;
    
    private JCheckBox adaptiveCheckBox;
    private JCheckBox drawLabelsCheckBox;
    private JCheckBox drawOutlineCheckBox;

    private JFormattedTextField latitudeStepField;
    private JFormattedTextField longitudeStepField;
    
    private JFormattedTextField minimumPopulationField;

    private JList<String> feedbackFieldsList;

    private boolean accepted;

    /**
     * Creates a style dialog.
     *
     * @param owner        owner window
     * @param title        dialog title
     * @param styleBits    capabilities to display
     * @param initialStyle initial style values
     */
    public MapLayerStyleDialog(
            Window owner,
            String title,
            long styleBits,
            MapLayerStyle initialStyle) {

        super(
                owner,
                title == null
                        ? "Map Layer Style"
                        : title,
                ModalityType.DOCUMENT_MODAL);

        this.styleBits = styleBits;
        this.workingStyle =
                new MapLayerStyle(initialStyle);

        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Displays the style editor.
     *
     * @param parent       parent component
     * @param title        dialog title
     * @param styleBits    capabilities to display
     * @param initialStyle initial values
     * @return edited style on OK, or {@code null} on Cancel
     */
    public static MapLayerStyle showDialog(
            Component parent,
            String title,
            long styleBits,
            MapLayerStyle initialStyle) {

        Window owner =
                (parent == null)
                        ? null
                        : SwingUtilities.getWindowAncestor(parent);

        MapLayerStyleDialog dialog =
                new MapLayerStyleDialog(
                        owner,
                        title,
                        styleBits,
                        initialStyle);

        dialog.setVisible(true);

        return dialog.accepted
                ? dialog.collectStyle()
                : null;
    }

    // builds the user interface
    private void buildUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Container content = getContentPane();
        content.setLayout(new BorderLayout(8, 8));

        JPanel editorPanel =
                new JPanel(new GridBagLayout());

        editorPanel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(
                        10, 10, 4, 10));

        int row = 0;

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.FILL_COLOR)) {

            fillColorButton =
                    new ColorButton(
                            "Choose…",
                            "Fill Color",
                            workingStyle.getFillColor(),
                            true,
                            true);

            addRow(
                    editorPanel,
                    row++,
                    "Fill color:",
                    fillColorButton);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.BOUNDARY_COLOR)) {

            boundaryColorButton =
                    new ColorButton(
                            "Choose…",
                            "Boundary Color",
                            workingStyle.getBoundaryColor(),
                            true,
                            true);

            addRow(
                    editorPanel,
                    row++,
                    "Boundary color:",
                    boundaryColorButton);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.LINE_WIDTH)) {

            lineWidthField =
                    createDoubleField(
                            workingStyle.getLineWidth(),
                            0.1,
                            20.0);

            addRow(
                    editorPanel,
                    row++,
                    "Line width:",
                    lineWidthField);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.POINT_COLOR)) {

            pointColorButton =
                    new ColorButton(
                            "Choose…",
                            "Point Color",
                            workingStyle.getPointColor(),
                            true,
                            true);

            addRow(
                    editorPanel,
                    row++,
                    "Point color:",
                    pointColorButton);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.LABEL_COLOR)) {

            labelColorButton =
                    new ColorButton(
                            "Choose…",
                            "Label Color",
                            workingStyle.getLabelColor(),
                            true,
                            true);

            addRow(
                    editorPanel,
                    row++,
                    "Label color:",
                    labelColorButton);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.LABEL_FONT_SIZE)) {

            labelFontSizeField =
                    createDoubleField(
                            workingStyle.getLabelFontSize(),
                            6.0,
                            36.0);

            addRow(
                    editorPanel,
                    row++,
                    "Label size:",
                    labelFontSizeField);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.POINT_SIZE)) {

            pointSizeField =
                    createDoubleField(
                            workingStyle.getPointSize(),
                            0.1,
                            100.0);

            addRow(
                    editorPanel,
                    row++,
                    "Point size:",
                    pointSizeField);
        }
        
        if (Bits.check(
                styleBits,
                MapLayerStyleBits.MIN_POPULATION)) {

            minimumPopulationField =
                    createLongField(
                            workingStyle.getMinimumPopulation(),
                            0L,
                            Long.MAX_VALUE);

            addRow(
                    editorPanel,
                    row++,
                    "Minimum population:",
                    minimumPopulationField);
        }
        
        if (Bits.check(
                styleBits,
                MapLayerStyleBits.ADAPTIVE)) {

            adaptiveCheckBox =
                    new JCheckBox(
                            "Use zoom-adaptive spacing",
                            workingStyle.isAdaptive());

            addRow(
                    editorPanel,
                    row++,
                    "Spacing:",
                    adaptiveCheckBox);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.DRAW_LABELS)) {

            drawLabelsCheckBox =
                    new JCheckBox(
                            "Draw labels",
                            workingStyle.isDrawLabels());

            addRow(
                    editorPanel,
                    row++,
                    "Labels:",
                    drawLabelsCheckBox);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.DRAW_OUTLINE)) {

            drawOutlineCheckBox =
                    new JCheckBox(
                            "Draw map outline",
                            workingStyle.isDrawOutline());

            addRow(
                    editorPanel,
                    row++,
                    "Outline:",
                    drawOutlineCheckBox);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.LATITUDE_STEP)) {

            latitudeStepField =
                    createDoubleField(
                            workingStyle.getLatitudeStepDeg(),
                            1.0 / 3600.0,
                            180.0);

            addRow(
                    editorPanel,
                    row++,
                    "Latitude step (degrees):",
                    latitudeStepField);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.LONGITUDE_STEP)) {

            longitudeStepField =
                    createDoubleField(
                            workingStyle.getLongitudeStepDeg(),
                            1.0 / 3600.0,
                            360.0);

            addRow(
                    editorPanel,
                    row++,
                    "Longitude step (degrees):",
                    longitudeStepField);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.OPACITY)) {

            opacitySlider =
                    new JSlider(
                            0,
                            100,
                            (int) Math.round(
                                    100.0
                                            * workingStyle.getOpacity()));

            opacityValueLabel =
                    new JLabel();

            updateOpacityLabel();

            opacitySlider.addChangeListener(
                    e -> updateOpacityLabel());

            JPanel sliderPanel =
                    new JPanel(new BorderLayout(6, 0));

            sliderPanel.add(
                    opacitySlider,
                    BorderLayout.CENTER);

            sliderPanel.add(
                    opacityValueLabel,
                    BorderLayout.EAST);

            addRow(
                    editorPanel,
                    row++,
                    "Opacity:",
                    sliderPanel);
        }

        if (Bits.check(
                styleBits,
                MapLayerStyleBits.FEEDBACK_FIELDS)) {

            java.util.List<String> available =
                    workingStyle.getAvailableFeedbackFields();

            feedbackFieldsList = new JList<>(
                    available.toArray(String[]::new));
            feedbackFieldsList.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            feedbackFieldsList.setVisibleRowCount(
                    Math.min(6, Math.max(3, available.size())));

            java.util.List<String> selected =
                    workingStyle.getSelectedFeedbackFields();
            int[] selectedIndices =
                    java.util.stream.IntStream.range(0, available.size())
                            .filter(i -> selected.contains(available.get(i)))
                            .toArray();
            feedbackFieldsList.setSelectedIndices(selectedIndices);

            JScrollPane scrollPane = new JScrollPane(feedbackFieldsList);
            scrollPane.setPreferredSize(SwingSizingUtils.preferredSizeAtLeast(
                    scrollPane, 230, 92));
            scrollPane.setToolTipText(
                    "Select the DBF fields displayed after a successful hit test");

            addRow(
                    editorPanel,
                    row++,
                    "Feedback fields:",
                    scrollPane);
        }
        
        if (adaptiveCheckBox != null) {
            adaptiveCheckBox.addActionListener(
                    e -> updateSpacingControls());

            updateSpacingControls();
        }

        if (row == 0) {
            editorPanel.add(
                    new JLabel(
                            "This layer has no editable style properties."));
        }

        content.add(
                editorPanel,
                BorderLayout.CENTER);

        content.add(
                createButtonPanel(),
                BorderLayout.SOUTH);

        setMinimumSize(SwingSizingUtils.preferredSizeAtLeast(
                content, 360, getPreferredSize().height));
    }
    
    private void updateSpacingControls() {
        boolean enabled =
                adaptiveCheckBox == null
                        || !adaptiveCheckBox.isSelected();

        if (latitudeStepField != null) {
            latitudeStepField.setEnabled(enabled);
        }

        if (longitudeStepField != null) {
            longitudeStepField.setEnabled(enabled);
        }
    }

    private JPanel createButtonPanel() {
        ActionListener listener =
                new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                String command =
                        event.getActionCommand();

                if (ButtonPanel.OK_LABEL.equals(command)) {
                    if (commitFormattedFields()) {
                        accepted = true;
                        dispose();
                    }
                }
                else if (ButtonPanel.CANCEL_LABEL.equals(command)) {
                    accepted = false;
                    dispose();
                }
            }
        };

        return ButtonPanel.closeOutPanel(
                ButtonPanel.USE_OKCANCEL,
                listener,
                6);
    }

    /**
     * Commits every numeric editor before accepting the dialog.
     *
     * <p>The formatters allow temporarily invalid text while the user edits,
     * so validation belongs at this transaction boundary. On failure the
     * dialog remains open and the offending field is selected.</p>
     */
    private boolean commitFormattedFields() {
        JFormattedTextField[] fields = {
                lineWidthField,
                labelFontSizeField,
                pointSizeField,
                latitudeStepField,
                longitudeStepField,
                minimumPopulationField
        };

        for (JFormattedTextField field : fields) {
            if (field == null || !field.isEnabled()) {
                continue;
            }
            try {
                field.commitEdit();
            } catch (ParseException ex) {
                field.requestFocusInWindow();
                field.selectAll();
                JOptionPane.showMessageDialog(
                        this,
                        "Enter a valid numeric value within the permitted range.",
                        "Invalid value",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    private static void addRow(
            JPanel panel,
            int row,
            String label,
            Component editor) {

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridy = row;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;

        panel.add(
                new JLabel(label),
                gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(editor, gbc);
    }

    static JFormattedTextField createDoubleField(
            double value,
            double minimum,
            double maximum) {

        NumberFormatter formatter =
                new NumberFormatter(
                        new java.text.DecimalFormat("0.0#"));

        formatter.setValueClass(Double.class);
        formatter.setMinimum(minimum);
        formatter.setMaximum(maximum);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);

        JFormattedTextField field =
                new JFormattedTextField(formatter);

        field.setValue(value);
        field.setColumns(8);

        return field;
    }
    
    /**
     * Creates a formatted field for a non-decimal long value.
     *
     * @param value   initial value
     * @param minimum minimum permitted value
     * @param maximum maximum permitted value
     * @return configured formatted text field
     */
    static JFormattedTextField createLongField(
            long value,
            long minimum,
            long maximum) {

        java.text.DecimalFormat format =
                new java.text.DecimalFormat("#,##0");

        format.setParseIntegerOnly(true);

        NumberFormatter formatter =
                new NumberFormatter(format);

        formatter.setValueClass(Long.class);
        formatter.setMinimum(minimum);
        formatter.setMaximum(maximum);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);

        JFormattedTextField field =
                new JFormattedTextField(formatter);

        field.setValue(value);
        field.setColumns(12);

        return field;
    }

    private void updateOpacityLabel() {
        if (opacitySlider == null
                || opacityValueLabel == null) {
            return;
        }

        opacityValueLabel.setText(
                opacitySlider.getValue() + "%");
    }

    // Collects the current values from the editor controls into a new style object.
    private MapLayerStyle collectStyle() {
        MapLayerStyle result =
                new MapLayerStyle(workingStyle);

        if (fillColorButton != null) {
            result.setFillColor(
                    fillColorButton.getColor());
        }

        if (boundaryColorButton != null) {
            result.setBoundaryColor(
                    boundaryColorButton.getColor());
        }

        if (pointColorButton != null) {
            result.setPointColor(
                    pointColorButton.getColor());
        }

        if (labelColorButton != null) {
            result.setLabelColor(
                    labelColorButton.getColor());
        }

        if (lineWidthField != null) {
            Number value =
                    (Number) lineWidthField.getValue();

            if (value != null) {
                result.setLineWidth(
                        value.floatValue());
            }
        }

        if (pointSizeField != null) {
            Number value =
                    (Number) pointSizeField.getValue();

            if (value != null) {
                result.setPointSize(
                        value.doubleValue());
            }
        }

        if (labelFontSizeField != null) {
            Number value =
                    (Number) labelFontSizeField.getValue();

            if (value != null) {
                result.setLabelFontSize(
                        value.floatValue());
            }
        }
        
        if (minimumPopulationField != null) {
            Number value =
                    (Number) minimumPopulationField.getValue();

            if (value != null) {
                result.setMinimumPopulation(
                        value.longValue());
            }
        }
        
        if (adaptiveCheckBox != null) {
            result.setAdaptive(
                    adaptiveCheckBox.isSelected());
        }

        if (drawLabelsCheckBox != null) {
            result.setDrawLabels(
                    drawLabelsCheckBox.isSelected());
        }

        if (drawOutlineCheckBox != null) {
            result.setDrawOutline(
                    drawOutlineCheckBox.isSelected());
        }

        if (latitudeStepField != null) {
            Number value =
                    (Number) latitudeStepField.getValue();

            if (value != null) {
                result.setLatitudeStepDeg(
                        value.doubleValue());
            }
        }

        if (longitudeStepField != null) {
            Number value =
                    (Number) longitudeStepField.getValue();

            if (value != null) {
                result.setLongitudeStepDeg(
                        value.doubleValue());
            }
        }
        

        if (opacitySlider != null) {
            result.setOpacity(
                    opacitySlider.getValue() / 100.0);
        }

        if (feedbackFieldsList != null) {
            result.setSelectedFeedbackFields(
                    feedbackFieldsList.getSelectedValuesList());
        }

        return result;
    }
}
