package edu.cnu.mdi.workspace;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable named result retained by a {@link ResultWorkspace}.
 *
 * @param <R> result type
 * @param id stable identity assigned at retention time
 * @param name user-facing name
 * @param retainedAt timestamp at which the workspace accepted the result
 * @param result application result; never {@code null}
 * @param metadata immutable application-defined metadata
 */
public record RetainedResult<R>(UUID id, String name, Instant retainedAt, R result,
        Map<String, String> metadata) {

    /** Validates components and defensively copies metadata. */
    public RetainedResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(retainedAt, "retainedAt");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(metadata, "metadata");
        if (name.isBlank()) throw new IllegalArgumentException("result name must not be blank");
        metadata = Map.copyOf(metadata);
    }
}
