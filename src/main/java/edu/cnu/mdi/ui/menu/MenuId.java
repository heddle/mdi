package edu.cnu.mdi.ui.menu;

import java.util.Objects;

/**
 * Stable, non-display identifier for an application menu.
 *
 * <p>Unlike {@link javax.swing.JMenu#getText()}, an ID may remain constant when
 * a label is renamed or localized. Reverse-domain or similarly scoped values
 * such as {@code "app.file"} and {@code "diagnostics.plot"} are recommended.</p>
 *
 * @param value stable identifier text
 */
public record MenuId(String value) {
    /** Validates the identifier. */
    public MenuId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("menu ID must not be blank");
    }
}
