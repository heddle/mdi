package edu.cnu.mdi.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Utility for displaying {@link AbstractViewInfo} content in a lightweight,
 * non-modal, non-stealing info dialog.
 *
 * <h2>Deduplication</h2>
 * <p>Only one info dialog per <em>owner window</em> is kept open at a time.
 * Calling {@link #showInfoDialog} while a dialog is already open for the same
 * owner brings the existing dialog to the front and replaces its content with
 * the new {@link AbstractViewInfo}. This prevents multiple identical dialogs
 * accumulating when the user clicks an info button repeatedly, and allows the
 * same dialog shell to serve different views as the user navigates.</p>
 *
 * <h2>Ownership and memory management</h2>
 * <p>The owner-to-dialog mapping uses a {@link WeakHashMap} keyed on the
 * {@link Window} ancestor of the {@code parent} component. When an owner
 * window is garbage-collected its entry is removed automatically, so the map
 * never prevents window cleanup. A {@link WindowAdapter} on each dialog also
 * removes its entry when the dialog is explicitly closed, so that the next
 * call creates a fresh dialog rather than reusing a disposed one.</p>
 *
 * <h2>Caller contract</h2>
 * <ul>
 *   <li>Must be called on the Swing EDT.</li>
 *   <li>Callers that hold a reference to the returned {@link JDialog} and wish
 *       to close it programmatically should call {@link JDialog#dispose()} on
 *       it; the helper will detect the disposal and remove the entry from its
 *       tracking map so that a fresh dialog can be shown later.</li>
 * </ul>
 */
public class InfoDialogHelper {

    /**
     * Maps each top-level owner {@link Window} to the currently open info
     * dialog for that window.
     *
     * <p>Weak keys mean that a garbage-collected window automatically removes
     * its entry. The dialog values are strong references but a dialog is only
     * reachable as long as its owner window is alive anyway.</p>
     */
    private static final WeakHashMap<Window, JDialog> openDialogs =
            new WeakHashMap<>();

    /** Utility class: no instances. */
    private InfoDialogHelper() {
    }

    /**
     * Show an info dialog for the given {@link AbstractViewInfo}, reusing any
     * existing dialog that belongs to the same owner window.
     *
     * <p>If a dialog for this owner is already open and has not been disposed,
     * it is brought to the front and its HTML content is updated to
     * {@code info}. The scroll position is reset to the top. This allows the
     * same dialog to be reused when the user opens info for different views
     * without closing the panel in between.</p>
     *
     * <p>When no reusable dialog exists for this owner, a new one is created.
     * It is configured as:
     * </p>
     * <ul>
     *   <li>Modeless ({@link Dialog.ModalityType#MODELESS}) — does not block
     *       the parent window.</li>
     *   <li>Non-focus-stealing ({@code autoRequestFocus = false},
     *       {@code focusableWindowState = false}) — clicking the info button
     *       does not shift keyboard focus away from the main window.</li>
     *   <li>{@link java.awt.Window.Type#UTILITY} — suppresses the taskbar
     *       entry on platforms that honour this type.</li>
     * </ul>
     *
     * <p>Must be called on the Swing EDT.</p>
     *
     * @param parent the component relative to which the dialog is positioned;
     *               must not be {@code null}
     * @param info   the view-info object whose HTML to display; must not be
     *               {@code null}
     * @return the shown (possibly pre-existing) dialog; never {@code null}
     */
    public static JDialog showInfoDialog(Component parent, AbstractViewInfo info) {

        Window owner = SwingUtilities.getWindowAncestor(parent);

        // Reuse an existing dialog for this owner if one is still showing.
        JDialog existing = openDialogs.get(owner);
        if (existing != null && existing.isDisplayable()) {
            updateContent(existing, info);
            existing.toFront();
            return existing;
        }

        // Build a new dialog.
        JDialog dialog = new JDialog(owner, "Information",
                Dialog.ModalityType.MODELESS);

        dialog.setAlwaysOnTop(false);
        dialog.setAutoRequestFocus(false);
        dialog.setFocusableWindowState(false);
        dialog.setType(java.awt.Window.Type.UTILITY);
        dialog.setLayout(new BorderLayout());

        JEditorPane infoPane = buildEditorPane(info);
        JScrollPane scrollPane = new JScrollPane(infoPane);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(parent);

        // Remove the entry when the dialog is closed so that a subsequent
        // showInfoDialog call creates a fresh instance rather than reusing a
        // disposed dialog.
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                openDialogs.remove(owner, dialog);
            }
        });

        openDialogs.put(owner, dialog);
        dialog.setVisible(true);

        return dialog;
    }

    /**
     * Replace the HTML content of an existing info dialog without closing and
     * re-opening it.
     *
     * <p>Finds the {@link JEditorPane} that was added at construction time
     * and sets its content to the HTML generated by {@code info}. The caret
     * position is reset to zero so the user always sees the beginning of the
     * new content.</p>
     *
     * @param dialog the dialog whose content to update; must not be
     *               {@code null}
     * @param info   the new view info; must not be {@code null}
     */
    private static void updateContent(JDialog dialog, AbstractViewInfo info) {
        for (java.awt.Component c : dialog.getContentPane().getComponents()) {
            if (c instanceof JScrollPane sp) {
                java.awt.Component view = sp.getViewport().getView();
                if (view instanceof JEditorPane pane) {
                    pane.setText(info.getAsHTML());
                    pane.setCaretPosition(0);
                    return;
                }
            }
        }
    }

    /**
     * Construct and configure the {@link JEditorPane} that renders the info
     * HTML.
     *
     * <p>The pane is configured with:
     * </p>
     * <ul>
     *   <li>{@code text/html} content type.</li>
     *   <li>{@link JEditorPane#HONOR_DISPLAY_PROPERTIES} set to
     *       {@code true} so that the system {@code Label.font} propagates into
     *       the HTML renderer.</li>
     *   <li>An empty border providing 10 px of padding on all sides.</li>
     *   <li>Caret at position 0 so the scroll position starts at the top.</li>
     * </ul>
     *
     * @param info the view info whose HTML to load; must not be {@code null}
     * @return a configured, non-{@code null} editor pane
     */
    private static JEditorPane buildEditorPane(AbstractViewInfo info) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setText(info.getAsHTML());
        pane.setEditable(false);
        pane.setCaretPosition(0);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(UIManager.getFont("Label.font"));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return pane;
    }
}