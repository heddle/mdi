package edu.cnu.mdi.transfer;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

/**
 * Implemented by any component that can receive files via drag-and-drop.
 *
 * <p>This interface is the <em>application-level</em> contract for file drops.
 * It is deliberately separate from the Swing plumbing ({@link FileDropHandler})
 * so that the view layer never has to reference Swing transfer internals
 * directly.</p>
 *
 * <h2>Intended usage</h2>
 * <p>Concrete views do <strong>not</strong> implement this interface manually.
 * Instead they extend {@link edu.cnu.mdi.view.BaseView}, which already
 * implements it, and they activate drop support through a single call:</p>
 * <pre>
 * // In a BaseView subclass constructor, after super(...):
 * enableFileDrop(f -> f.getName().endsWith(".csv"));
 *
 * {@literal @}Override
 * public void filesDropped(List{@literal <}File{@literal >} files) {
 *     openCsvFile(files.get(0));
 * }
 * </pre>
 *
 * <h2>Filter semantics</h2>
 * <p>The filter is applied by {@link FileDropHandler} before
 * {@link #filesDropped} is called.  Only files that pass the predicate are
 * forwarded.  Passing {@code null} as the filter accepts every dropped file.</p>
 *
 * <h2>Threading</h2>
 * <p>{@link #filesDropped} is called on the Swing Event Dispatch Thread.</p>
 */
public interface IFileDropHandler {

    /**
     * Set the file filter used to screen dropped files.
     *
     * <p>The {@link FileDropHandler} evaluates this predicate against each
     * candidate file and forwards only those that return {@code true}. A
     * {@code null} filter accepts every file without screening.</p>
     *
     * @param filter the acceptance predicate, or {@code null} to accept all files
     */
    void setFileFilter(Predicate<File> filter);

    /**
     * Returns the current file filter.
     *
     * @return the acceptance predicate, or {@code null} if no filter is set
     */
    Predicate<File> getFileFilter();

    /**
     * Called on the EDT when one or more accepted files have been dropped onto
     * this component.
     *
     * <p>The list contains only files that passed the current filter (or all
     * dropped files if no filter is set). The list is never {@code null} and
     * is never empty.</p>
     *
     * @param files the accepted dropped files; never {@code null}, never empty
     */
    void filesDropped(List<File> files);
}