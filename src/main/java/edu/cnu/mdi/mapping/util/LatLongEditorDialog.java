package edu.cnu.mdi.mapping.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * A modal dialog that allows the user to view and edit a geographic coordinate
 * in either Decimal Degrees or UTM format.
 *
 * <p>Internally, the coordinate is stored as a {@link Point2D.Double} in radians,
 * following the mapping convention: {@code .x = longitude}, {@code .y = latitude}.
 *
 * <p>The two edit panels are kept in sync on every mode switch: before revealing
 * the incoming panel, the dialog first commits any edits from the outgoing panel
 * into {@code currentPoint}, then re-populates the incoming panel from that
 * updated value. This means the user's in-progress edits are never silently
 * discarded.
 *
 * <p>Usage:
 * <pre>{@code
 * LatLongEditorDialog dlg = new LatLongEditorDialog(ownerFrame, point);
 * dlg.setVisible(true);
 * Point2D.Double result = dlg.getResult(); // null if the user cancelled
 * }</pre>
 */
@SuppressWarnings("serial")
public class LatLongEditorDialog extends JDialog {

    /** Coordinate stored internally in radians: {@code .x = lon}, {@code .y = lat}. */
    private Point2D.Double currentPoint;

    /** Set to true only when the user explicitly clicks Save. */
    private boolean saved = false;

    // --- Decimal Degrees panel fields ---
    private JTextField txtLat;
    private JTextField txtLon;

    // --- UTM panel fields ---
    private JTextField txtEasting;
    private JTextField txtNorthing;
    private JTextField txtZone;
    private JTextField txtLetter;

    // --- Layout ---
    private JComboBox<String> comboMode;
    private JPanel cardPanel;

    /** Card key for the Decimal Degrees panel. */
    private static final String CARD_DD  = "DD";
    /** Card key for the UTM panel. */
    private static final String CARD_UTM = "UTM";

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new coordinate editor dialog.
     *
     * @param owner        the parent frame (may be {@code null})
     * @param initialPoint the starting coordinate in radians
     *                     ({@code .x = longitude}, {@code .y = latitude});
     *                     a defensive copy is taken immediately
     */
    public LatLongEditorDialog(Frame owner, Point2D.Double initialPoint) {
        super(owner, "Coordinate Editor", true);
        // Defensive copy so we don't mutate the caller's object mid-edit.
        this.currentPoint = new Point2D.Double(initialPoint.x, initialPoint.y);
        initComponents();
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void initComponents() {
        setLayout(new BorderLayout(0, 4));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // --- Mode selector ---
        comboMode = new JComboBox<>(new String[]{"Decimal Degrees", "UTM"});
        comboMode.addActionListener(e -> switchView());
        add(comboMode, BorderLayout.NORTH);

        // --- Card panels ---
        cardPanel = new JPanel(new CardLayout());
        cardPanel.add(createLatLonPanel(), CARD_DD);
        cardPanel.add(createUTMPanel(),    CARD_UTM);
        add(cardPanel, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave   = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> {
            if (commitCurrentPanel()) {
                saved = true;
                dispose();
            }
            // If commitCurrentPanel() returned false, validation messages have
            // already been shown; keep the dialog open so the user can correct.
        });

        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

        // Populate fields before pack() so the dialog sizes to real content.
        populateDDFields();
        populateUTMFields();

        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private JPanel createLatLonPanel() {
        JPanel p = new JPanel(new GridLayout(2, 2, 4, 4));
        p.setBorder(new EmptyBorder(4, 0, 4, 0));

        p.add(new JLabel("Latitude (°):"));
        txtLat = new JTextField(12);
        p.add(txtLat);

        p.add(new JLabel("Longitude (°):"));
        txtLon = new JTextField(12);
        p.add(txtLon);

        return p;
    }

    private JPanel createUTMPanel() {
        JPanel p = new JPanel(new GridLayout(4, 2, 4, 4));
        p.setBorder(new EmptyBorder(4, 0, 4, 0));

        p.add(new JLabel("Easting (m):"));
        txtEasting = new JTextField(12);
        p.add(txtEasting);

        p.add(new JLabel("Northing (m):"));
        txtNorthing = new JTextField(12);
        p.add(txtNorthing);

        p.add(new JLabel("Zone:"));
        txtZone = new JTextField(4);
        p.add(txtZone);

        p.add(new JLabel("Band letter:"));
        txtLetter = new JTextField(2);
        p.add(txtLetter);

        return p;
    }

    // -------------------------------------------------------------------------
    // Panel switching
    // -------------------------------------------------------------------------

    /**
     * Handles a change in the mode combo box.
     *
     * <p>The strategy is: commit the <em>currently visible</em> panel's values
     * into {@code currentPoint} first, then re-populate the <em>incoming</em>
     * panel from the (now updated) {@code currentPoint}, and finally flip the card.
     * If the commit fails (bad input), we roll the combo back and keep the current
     * panel visible so the user can fix the problem.
     */
    private void switchView() {
        CardLayout cl = (CardLayout) cardPanel.getLayout();
        boolean switchingToUTM = comboMode.getSelectedIndex() == 1;

        if (switchingToUTM) {
            // We are leaving DD — commit DD fields first.
            if (!commitDDFields()) {
                // Rollback the combo silently (remove listener while changing).
                comboMode.removeActionListener(comboMode.getActionListeners()[0]);
                comboMode.setSelectedIndex(0);
                comboMode.addActionListener(e -> switchView());
                return;
            }
            populateUTMFields();
            cl.show(cardPanel, CARD_UTM);
        } else {
            // We are leaving UTM — commit UTM fields first.
            if (!commitUTMFields()) {
                comboMode.removeActionListener(comboMode.getActionListeners()[0]);
                comboMode.setSelectedIndex(1);
                comboMode.addActionListener(e -> switchView());
                return;
            }
            populateDDFields();
            cl.show(cardPanel, CARD_DD);
        }
    }

    // -------------------------------------------------------------------------
    // Field population (currentPoint → UI)
    // -------------------------------------------------------------------------

    /** Writes {@code currentPoint} into the Decimal Degrees text fields. */
    private void populateDDFields() {
        txtLat.setText(String.format("%.6f", Math.toDegrees(currentPoint.y)));
        txtLon.setText(String.format("%.6f", Math.toDegrees(currentPoint.x)));
    }

    /** Converts {@code currentPoint} to UTM and writes the result into the UTM fields. */
    private void populateUTMFields() {
        try {
            UTMCoordinate utm = GeoUtils.fromRadians(currentPoint);
            txtEasting.setText(String.format("%.2f",  utm.easting));
            txtNorthing.setText(String.format("%.2f", utm.northing));
            txtZone.setText(String.valueOf(utm.zone));
            txtLetter.setText(String.valueOf(utm.letter));
        } catch (IllegalArgumentException ex) {
            // Latitude is outside the UTM-defined range — leave fields blank
            // and show a non-blocking tooltip so the user understands why.
            txtEasting.setText("");
            txtNorthing.setText("");
            txtZone.setText("");
            txtLetter.setText("");
            txtEasting.setToolTipText("Latitude out of UTM range: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Field parsing / commit (UI → currentPoint)
    // -------------------------------------------------------------------------

    /**
     * Reads and validates the Decimal Degrees fields, updating {@code currentPoint}
     * on success.
     *
     * @return {@code true} if parsing succeeded; {@code false} if the input was
     *         invalid (an error dialog will have been shown to the user)
     */
    private boolean commitDDFields() {
        double lat, lon;
        try {
            lat = Double.parseDouble(txtLat.getText().trim());
            lon = Double.parseDouble(txtLon.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Please enter valid decimal numbers for latitude and longitude.");
            return false;
        }
        if (lat < -90.0 || lat > 90.0) {
            showError("Latitude must be between -90° and +90°.");
            return false;
        }
        if (lon < -180.0 || lon > 180.0) {
            showError("Longitude must be between -180° and +180°.");
            return false;
        }
        currentPoint = new Point2D.Double(Math.toRadians(lon), Math.toRadians(lat));
        return true;
    }

    /**
     * Reads and validates the UTM fields, converting back to radians and updating
     * {@code currentPoint} on success.
     *
     * @return {@code true} if parsing and conversion succeeded; {@code false} if
     *         the input was invalid (an error dialog will have been shown)
     */
    private boolean commitUTMFields() {
        double easting, northing;
        int zone;
        char letter;

        try {
            easting  = Double.parseDouble(txtEasting.getText().trim());
            northing = Double.parseDouble(txtNorthing.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Easting and Northing must be valid numbers.");
            return false;
        }

        try {
            zone = Integer.parseInt(txtZone.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Zone must be an integer between 1 and 60.");
            return false;
        }

        String letterText = txtLetter.getText().trim().toUpperCase();
        if (letterText.length() != 1) {
            showError("Band letter must be a single character (e.g. T).");
            return false;
        }
        letter = letterText.charAt(0);

        try {
            UTMCoordinate utm = new UTMCoordinate(easting, northing, zone, letter);
            currentPoint = GeoUtils.toRadians(utm);
        } catch (IllegalArgumentException ex) {
            showError("Invalid UTM coordinate: " + ex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Commits whichever panel is currently active.
     *
     * @return {@code true} if the commit succeeded
     */
    private boolean commitCurrentPanel() {
        return comboMode.getSelectedIndex() == 0 ? commitDDFields() : commitUTMFields();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the edited coordinate in radians ({@code .x = longitude},
     * {@code .y = latitude}), or {@code null} if the user cancelled without saving.
     *
     * <p>A defensive copy is returned so the caller cannot modify the dialog's
     * internal state after the fact.
     *
     * @return the result coordinate, or {@code null} on cancel
     */
    public Point2D.Double getResult() {
        if (!saved) return null;
        return new Point2D.Double(currentPoint.x, currentPoint.y);
    }
}