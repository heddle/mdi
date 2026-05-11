package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.io.File;
import java.util.List;

import javax.swing.JPanel;

import edu.cnu.mdi.json.JsonSplitPane;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * A container-less view that displays a JSON file as both colorized raw text
 * and a collapsible tree, side by side in a split pane.
 *
 * <h2>Usage</h2>
 * <p>
 * Open a file programmatically via {@link #loadFile(File)}, or simply drag
 * a {@code .json} file onto the view — drop support is enabled in the
 * constructor via {@link BaseView#enableFileDrop(java.util.function.Predicate)}.
 * </p>
 *
 * <h2>Layout</h2>
 * <p>
 * The view is container-less (no {@link edu.cnu.mdi.container.IContainer}):
 * it follows the same pattern as {@link LogView} and adds its content
 * component — a {@link JsonSplitPane} — directly to the frame's
 * {@code BorderLayout.CENTER}.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * File I/O and JSON parsing are performed on a background thread inside
 * {@link JsonSplitPane#loadFile(File)}. The EDT is never blocked regardless
 * of file size.
 * </p>
 */
@SuppressWarnings("serial")
public class JsonView extends BaseView {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Title shown in the internal frame's title bar. */
    private static final String TITLE = "JSON Viewer";

    /** Default view width in pixels. */
    private static final int DEFAULT_WIDTH  = 900;

    /** Default view height in pixels. */
    private static final int DEFAULT_HEIGHT = 600;

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** The split-pane content component. */
    private final JsonSplitPane splitPane;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Create a {@code JsonView} with default dimensions, initially hidden.
     */
    public JsonView() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, true);
    }

    /**
     * Create a {@code JsonView} with explicit dimensions and visibility.
     *
     * <p>Construction follows the {@link LogView} pattern: properties are
     * passed to {@link BaseView}, then the content panel is added to
     * {@code BorderLayout.CENTER}. File drop support is activated last so
     * the view frame — the drop surface for container-less views — is
     * already in the Swing hierarchy.</p>
     *
     * @param width   initial frame width in pixels
     * @param height  initial frame height in pixels
     * @param visible {@code true} to make the view visible immediately
     */
    public JsonView(int width, int height, boolean visible) {
        super(PropertyUtils.TITLE,       TITLE,
              PropertyUtils.WIDTH,        width,
              PropertyUtils.HEIGHT,       height,
              PropertyUtils.VISIBLE,      visible);

        splitPane = new JsonSplitPane();
        
        ViewInfoButton info = getInfoButton();
        //add the button to the far right side of the north component
        // of the split pane 
        JPanel north = splitPane.getHeader();
        //add to the EAST position
        north.add(info, BorderLayout.EAST);
        
       
        add(splitPane);

        // Accept .json files dropped anywhere on the view frame.
        // For container-less views the frame-level fallback in enableFileDrop
        // is the only active drop surface, which is exactly what we want.
        enableFileDrop(f -> f.isFile()
                && f.getName().toLowerCase().endsWith(".json"));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Load and display the given JSON file.
     *
     * <p>Delegates to {@link JsonSplitPane#loadFile(File)}, which performs
     * I/O and parsing on a background thread and updates both panes on the
     * EDT when done.</p>
     *
     * @param file the JSON file to display; must not be {@code null}
     */
    public void loadFile(File file) {
        splitPane.loadFile(file);
    }

    // -------------------------------------------------------------------------
    // BaseView overrides
    // -------------------------------------------------------------------------

    /**
     * Handle a JSON file dropped onto this view.
     *
     * <p>Only the first file is used. The filter set in the constructor
     * ({@code .json} extension) has already been applied by
     * {@link edu.cnu.mdi.transfer.FileDropHandler}, so no second check is
     * needed here.</p>
     *
     * @param files the accepted dropped files; never {@code null}, never empty
     */
    @Override
    public void filesDropped(List<File> files) {
        loadFile(files.get(0));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link JsonViewInfo} describing the view's purpose,
     * supported file types, and keyboard shortcuts.</p>
     */
    @Override
    public AbstractViewInfo getViewInfo() {
        return new JsonViewInfo();
    }
}