package edu.cnu.mdi.workspace;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered, optionally bounded collection of completed application results.
 *
 * <p>The workspace centralizes the common scientific-workbench pattern of
 * retaining completed calculations for later comparison, export, or reopening
 * diagnostic views. It stores application result objects without knowing their
 * scientific type or UI representation.</p>
 *
 * <h2>Retention and eviction</h2>
 * <p>Entries are ordered from oldest to newest. A positive capacity evicts the
 * oldest entry whenever a new result would exceed that capacity; zero means
 * unbounded. Eviction produces the same removal callback as explicit removal.</p>
 *
 * <h2>Threading</h2>
 * <p>State operations are synchronized and snapshots are immutable. Listeners
 * use copy-on-write registration and are called synchronously after each state
 * mutation. Listener failures are isolated from other listeners.</p>
 *
 * @param <R> retained result type
 */
public final class ResultWorkspace<R> {

    private final int capacity;
    private final Clock clock;
    private final List<RetainedResult<R>> entries = new ArrayList<>();
    private final CopyOnWriteArrayList<ResultWorkspaceListener<R>> listeners =
            new CopyOnWriteArrayList<>();

    /** Creates an unbounded workspace using the system UTC clock. */
    public ResultWorkspace() {
        this(0);
    }

    /**
     * Creates a workspace using the system UTC clock.
     * @param capacity maximum entries, or zero for unbounded
     * @throws IllegalArgumentException if capacity is negative
     */
    public ResultWorkspace(int capacity) {
        this(capacity, Clock.systemUTC());
    }

    ResultWorkspace(int capacity, Clock clock) {
        if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0");
        this.capacity = capacity;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Retains a result without metadata.
     * @param name user-facing name
     * @param result result object
     * @return immutable retained entry
     */
    public RetainedResult<R> retain(String name, R result) {
        return retain(name, result, Map.of());
    }

    /**
     * Retains a result and optional string metadata.
     *
     * @param name user-facing name
     * @param result result object
     * @param metadata application metadata, defensively copied
     * @return immutable retained entry
     */
    public RetainedResult<R> retain(String name, R result, Map<String, String> metadata) {
        RetainedResult<R> added = new RetainedResult<>(UUID.randomUUID(), name,
                Instant.now(clock), result, metadata);
        RetainedResult<R> evicted = null;
        synchronized (this) {
            entries.add(added);
            if (capacity > 0 && entries.size() > capacity) evicted = entries.remove(0);
        }
        if (evicted != null) notifyRemoved(evicted);
        notifyRetained(added);
        return added;
    }

    /** @return immutable oldest-to-newest snapshot */
    public synchronized List<RetainedResult<R>> entries() {
        return List.copyOf(entries);
    }

    /**
     * Finds an entry by stable ID.
     * @param id entry ID
     * @return matching entry, or empty
     */
    public synchronized Optional<RetainedResult<R>> find(UUID id) {
        Objects.requireNonNull(id, "id");
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    /**
     * Removes an entry by ID.
     * @param id entry ID
     * @return removed entry, or empty
     */
    public Optional<RetainedResult<R>> remove(UUID id) {
        Objects.requireNonNull(id, "id");
        RetainedResult<R> removed = null;
        synchronized (this) {
            for (int index = 0; index < entries.size(); index++) {
                if (entries.get(index).id().equals(id)) {
                    removed = entries.remove(index);
                    break;
                }
            }
        }
        if (removed != null) notifyRemoved(removed);
        return Optional.ofNullable(removed);
    }

    /** Removes all entries. A no-op workspace emits no callback. */
    public void clear() {
        List<RetainedResult<R>> removed;
        synchronized (this) {
            if (entries.isEmpty()) return;
            removed = List.copyOf(entries);
            entries.clear();
        }
        for (RetainedResult<R> entry : removed) notifyRemoved(entry);
        for (ResultWorkspaceListener<R> listener : listeners) {
            try { listener.workspaceCleared(); } catch (Throwable ignored) { }
        }
    }

    /** @return configured capacity, or zero when unbounded */
    public int capacity() {
        return capacity;
    }

    /** @return current number of retained entries */
    public synchronized int size() {
        return entries.size();
    }

    /** @param listener listener to add; null is ignored */
    public void addListener(ResultWorkspaceListener<R> listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    /** @param listener listener to remove */
    public void removeListener(ResultWorkspaceListener<R> listener) {
        listeners.remove(listener);
    }

    private void notifyRetained(RetainedResult<R> entry) {
        for (ResultWorkspaceListener<R> listener : listeners) {
            try { listener.resultRetained(entry); } catch (Throwable ignored) { }
        }
    }

    private void notifyRemoved(RetainedResult<R> entry) {
        for (ResultWorkspaceListener<R> listener : listeners) {
            try { listener.resultRemoved(entry); } catch (Throwable ignored) { }
        }
    }
}
