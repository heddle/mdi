package edu.cnu.mdi.graphics.toolbar;

import java.awt.Cursor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Central registry and selection manager for {@link ITool} instances.
 * <p>
 * The controller owns:
 * </p>
 * <ul>
 *   <li>a registry of tools, keyed by tool id</li>
 *   <li>the currently active tool</li>
 *   <li>an optional default tool used when a toggle is turned off</li>
 * </ul>
 * <p>
 * Selection triggers tool lifecycle callbacks ({@link ITool#onSelected(ToolContext)}
 * and {@link ITool#onDeselected(ToolContext)}), and updates the canvas cursor to
 * match the active tool.
 * </p>
 * <p>
 * This class deliberately contains no Swing button logic. UI widgets simply request
 * activation via {@link #select(String)}.
 * </p>
 *
 * @author heddle
 */
public class ToolController {

    private final ToolContext ctx;
    private final Map<String, ITool> tools = new LinkedHashMap<>();
    private ITool active;
    private ITool defaultTool;
    
    private final java.util.List<ToolSelectionListener> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();


    /**
     * Create a controller for the given context.
     *
     * @param ctx the tool context (never null).
     */
    public ToolController(ToolContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ToolContext must not be null");
    }

    /**
     * Register a tool with the controller.
     * <p>
     * If a tool with the same id already exists, it will be replaced.
     * </p>
     *
     * @param tool the tool to register (must not be null).
     */
    public void register(ITool tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        tools.put(tool.id(), tool);
    }

    /**
     * Look up a registered tool by id.
     *
     * @param id the tool id.
     * @return the tool instance, or null if not registered.
     */
    public ITool get(String id) {
        return tools.get(id);
    }

    /**
     * @return the currently active tool, or null if none has been selected.
     */
    public ITool getActive() {
        return active;
    }

    /**
     * Set the default tool to be used when toggles are turned off.
     *
     * @param id the id of a registered tool (ignored if not registered).
     */
    public void setDefaultTool(String id) {
        defaultTool = get(id);
    }

    /**
     * Get the configured default tool id.
     *
     * @return the default tool id, or null if no default tool is configured.
     */
    public String getDefaultToolId() {
        return (defaultTool == null) ? null : defaultTool.id();
	}

	public void addSelectionListener(ToolSelectionListener l) {
		if (l != null && !listeners.contains(l)) {
			listeners.add(l);
		}
	}

    private void notifySelectionChanged() {
    	if (active == null) return;
        for (ToolSelectionListener l : listeners) {
            l.activeToolChanged(active);
        }
    }

    /**
     * Select the tool with the given id.
     * <p>
     * If the tool is already active or not registered, this method does nothing.
     * </p>
     *
     * @param id the tool id.
     */
    public void select(String id) {
        ITool next = get(id);
        if (next == null || next == active) return;

        if (active != null) active.onDeselected(ctx);

        active = next;
        active.onSelected(ctx);

        applyCursor();
        notifySelectionChanged();
    }

    private void applyCursor() {
        if (ctx.canvas() == null) return;

        Cursor c = (active == null) ? Cursor.getDefaultCursor() : active.cursor(ctx);
        ctx.canvas().setCursor((c != null) ? c : Cursor.getDefaultCursor());
    }


    /**
     * Reset tool selection to the configured default tool, if any.
     * <p>
     * Typically called when a toggle button is deselected to avoid a "no tool active"
     * state.
     * </p>
     */
    public void resetToDefault() {
        if (defaultTool != null) {
            select(defaultTool.id());
        }
    }
    
    public void applyCursorNow() {
        applyCursor();
    }

}
