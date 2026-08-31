package edu.cnu.mdi.workspace;

/**
 * Observer of retained-result workspace changes.
 *
 * <p>Callbacks run synchronously on the thread that changed the workspace. UI
 * consumers should mutate a workspace on Swing's EDT or marshal their listener
 * work there.</p>
 *
 * @param <R> result type
 */
public interface ResultWorkspaceListener<R> {

    /** @param entry newly retained entry */
    default void resultRetained(RetainedResult<R> entry) { }

    /**
     * Called for explicit removal and capacity eviction.
     * @param entry removed entry
     */
    default void resultRemoved(RetainedResult<R> entry) { }

    /** Called after all entries have been removed. */
    default void workspaceCleared() { }
}
