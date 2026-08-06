package edu.cnu.mdi.view;

import java.util.Objects;
import java.util.Properties;

/**
 * Immutable value object containing the initialization options for a
 * {@link BaseView}.
 *
 * <p>MDI historically accepts either alternating key/value arguments or a
 * mutable {@link Properties} instance. {@code ViewOptions} provides an
 * explicit constructor type while retaining complete compatibility with those
 * APIs. Create instances with {@link ViewPropertiesBuilder#buildOptions()}.
 * Both construction and conversion defensively copy the property map.</p>
 */
public final class ViewOptions {

    private final Properties properties;

    private ViewOptions(Properties properties) {
        this.properties = copy(properties);
    }

    /**
     * Creates options from existing MDI properties.
     *
     * @param properties properties to copy
     * @return immutable options snapshot
     * @throws NullPointerException if {@code properties} is null
     */
    public static ViewOptions from(Properties properties) {
        return new ViewOptions(Objects.requireNonNull(properties, "properties"));
    }

    /**
     * Returns a mutable defensive copy suitable for legacy MDI constructors.
     * Mutating the returned object does not alter these options.
     *
     * @return copied properties
     */
    public Properties toProperties() {
        return copy(properties);
    }

    private static Properties copy(Properties source) {
        Properties result = new Properties();
        result.putAll(source);
        return result;
    }
}
