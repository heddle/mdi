package edu.cnu.mdi.graphics.connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.item.ConnectorItem;
import edu.cnu.mdi.item.ItemChangeListener;
import edu.cnu.mdi.item.ItemChangeType;
import edu.cnu.mdi.item.Layer;

/**
 * Manages {@link ConnectorItem} objects and enforces connection rules.
 * <p>
 * This manager listens to drawable list change events so it can:
 * </p>
 * <ul>
 * <li>cache connectors when they are added</li>
 * <li>remove connectors when an endpoint item is removed</li>
 * </ul>
 *
 * <p>
 * Duplicate connections are prevented: at most one connector may exist between
 * two endpoint items (in either direction).
 * </p>
 */
public class ConnectionManager implements ItemChangeListener {

	/** Singleton instance. */
	private static volatile ConnectionManager instance = null;

	/** All known connectors currently present in any observed list(s). */
	private final Set<ConnectorItem> connectors = new HashSet<>();

	// singleton constructor
	private ConnectionManager() {
		// private singleton constructor
	}

	/**
	 * Get the singleton instance.
	 *
	 * @return the connection manager
	 */
	public static ConnectionManager getInstance() {
		if (instance == null) {
			synchronized (ConnectionManager.class) {
				if (instance == null) {
					instance = new ConnectionManager();
				}
			}
		}
		return instance;
	}

	/**
	 * Determine whether two items can be connected.
	 * <p>
	 * Rules:
	 * <ul>
	 * <li>neither item is null</li>
	 * <li>items are not the same instance</li>
	 * <li>both items are connectable</li>
	 * <li>no existing connector already connects them (either direction)</li>
	 * </ul>
	 *
	 * @param item1 first endpoint
	 * @param item2 second endpoint
	 * @return true if a new connection is allowed
	 */
	public boolean canConnect(AItem item1, AItem item2) {
		if (item1 == null || item2 == null || (item1 == item2) || (item1.getContainer() != item2.getContainer())) {
			return false;
		}
		if (!item1.isConnectable() || !item2.isConnectable()) {
			return false;
		}

		return !hasConnection(item1, item2);
	}

	/**
	 * Create and register a new {@link ConnectorItem} if allowed.
	 * <p>
	 * Note: the created item will also be discovered via ADDED events if the
	 * layer/list notifies listeners; we still add to our cache immediately so
	 * callers can rely on the returned item.
	 * </p>
	 *
	 * @param layer layer to place the connector on (recommended: a connection
	 *              layer)
	 * @param item1 first endpoint
	 * @param item2 second endpoint
	 * @return the created connector, or null if connection is not allowed
	 */
	public ConnectorItem connect(Layer layer, AItem item1, AItem item2) {

		Objects.requireNonNull(layer, "layer");
		if (!canConnect(item1, item2)) {
			return null;
		}

		ConnectorItem ci = new ConnectorItem(layer, item1, item2);

		// dealing with at least two layers here!
		layer.addItemChangeListener(instance);
		item1.getLayer().addItemChangeListener(instance);
		item2.getLayer().addItemChangeListener(instance);
		connectors.add(ci);
		return ci;
	}

	/**
	 * Check whether a connection already exists between the two items. (Direction
	 * does not matter.)
	 */
	public boolean hasConnection(AItem item1, AItem item2) {
		if (item1 == null || item2 == null) {
			return false;
		}

		for (ConnectorItem c : connectors) {
			if (connects(c, item1, item2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return an unmodifiable snapshot of known connectors.
	 */
	public Set<ConnectorItem> getConnectors() {
		return Collections.unmodifiableSet(new HashSet<>(connectors));
	}

	/**
	 * Drawable list change callback.
	 */
	@Override
	public void itemChanged(Layer layer, AItem item, ItemChangeType type) {

		if (type == null) {
			return;
		}

		switch (type) {
		case ADDED:
			if (item instanceof ConnectorItem) {
				connectors.add((ConnectorItem) item);
			}
			break;

		case DELETED:
			// If a connector was removed, drop it from our cache.
			if (item instanceof ConnectorItem) {
				connectors.remove(item);
				break;
			}

			// If an endpoint item was removed, remove all connectors that touch it.
			if (item instanceof AItem) {
				removeConnectionsForEndpoint(item);
			}
			break;

		default:
			// ignore other changes
			break;
		}
	}

	// ------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------

	private void removeConnectionsForEndpoint(AItem endpoint) {
		if (endpoint == null) {
			return;
		}

		for (Iterator<ConnectorItem> it = connectors.iterator(); it.hasNext();) {
			ConnectorItem c = it.next();

			AItem a = safeStart(c);
			AItem b = safeEnd(c);

			if (a == endpoint || b == endpoint) {
				it.remove();

				// Remove the connector item from its layer, if it still has one.
				Layer layer = c.getLayer();
				if (layer != null) {
					layer.remove(c);
				}
			}
		}
	}

	/**
	 * Get all items directly connected to the given item.
	 *
	 * <p>
	 * The returned set contains the opposite endpoint of every
	 * {@link ConnectorItem} that touches the given item.
	 * </p>
	 *
	 * <p>
	 * The returned set is a snapshot and is not backed by the manager.
	 * </p>
	 *
	 * @param item the endpoint item
	 * @return an unmodifiable set of connected items (empty if none)
	 */
	public Set<AItem> getConnectedItems(AItem item) {
		if (item == null) {
			return Collections.emptySet();
		}

		Set<AItem> results = new HashSet<>();

		for (ConnectorItem c : connectors) {
			AItem a = safeStart(c);
			AItem b = safeEnd(c);

			if (a == item && b != null) {
				results.add(b);
			} else if (b == item && a != null) {
				results.add(a);
			}
		}

		return Collections.unmodifiableSet(results);
	}

	// Check whether connector c connects the two given items (either direction).
	private static boolean connects(ConnectorItem c, AItem i1, AItem i2) {
		AItem a = safeStart(c);
		AItem b = safeEnd(c);
		if (a == null || b == null) {
			return false;
		}

		return (a == i1 && b == i2) || (a == i2 && b == i1);
	}

	/**
	 * These accessors assume you add public endpoint getters to ConnectorItem. If
	 * you haven't yet, add: public AItem getStartItem() { return startItem; }
	 * public AItem getEndItem() { return endItem; }
	 */
	private static AItem safeStart(ConnectorItem c) {
		try {
			return c.getStartItem();
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private static AItem safeEnd(ConnectorItem c) {
		try {
			return c.getEndItem();
		} catch (RuntimeException ex) {
			return null;
		}
	}
}
