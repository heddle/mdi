package edu.cnu.mdi.view;

import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;

/**
 * Factory interface for creating an {@link IContainer} with a given initial
 * world coordinate system.
 *
 * <h2>Purpose</h2>
 * <p>
 * This interface replaces the previous reflection-based convention that
 * required container classes to expose a {@code (Rectangle2D.Double)}
 * constructor discoverable only at runtime via
 * {@link Class#getDeclaredConstructor}. Using a functional interface instead
 * provides several concrete benefits:
 * </p>
 * <ul>
 *   <li><strong>Compile-time safety</strong> — mismatched signatures are
 *       caught by {@code javac}, not at first use.</li>
 *   <li><strong>IDE navigation</strong> — a constructor reference or lambda
 *       can be followed with "Go to definition"; a {@link Class} stored in a
 *       {@link java.util.Properties} map cannot.</li>
 *   <li><strong>Richer initialization</strong> — a lambda factory can call
 *       additional setup methods that a bare constructor cannot.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Register via {@link ViewPropertiesBuilder#containerFactory(ContainerFactory)}:
 * </p>
 * <pre>
 *   // Constructor reference — simplest case.
 *   ContainerFactory f = MyContainer::new;
 *
 *   // Lambda — when extra initialization is needed after construction.
 *   ContainerFactory f = ws -&gt; {
 *       MyContainer c = new MyContainer(ws);
 *       c.setGridEnabled(true);
 *       c.setSnapInterval(10);
 *       return c;
 *   };
 *
 *   Properties props = new ViewPropertiesBuilder()
 *       .title("My View")
 *       .worldSystem(world)
 *       .containerFactory(f)
 *       .build();
 * </pre>
 *
 * <h2>Fallback behaviour</h2>
 * <p>
 * When no {@code ContainerFactory} is registered, the framework falls back
 * to constructing a default {@link edu.cnu.mdi.container.BaseContainer}. The
 * legacy {@link PropertyUtils#CONTAINERCLASS} key is still honoured for
 * backward compatibility, but new code should always prefer this interface.
 * </p>
 *
 * <h2>Contract</h2>
 * <p>
 * Implementations must not return {@code null}. If construction fails the
 * factory should throw a descriptive {@link RuntimeException} rather than
 * returning {@code null}, so the caller can surface a meaningful error.
 * </p>
 */
@FunctionalInterface
public interface ContainerFactory {

    /**
     * Create a new {@link IContainer} initialised to the given world coordinate
     * rectangle.
     *
     * @param worldSystem the initial world window; never {@code null}
     * @return a fully constructed, non-{@code null} container
     * @throws RuntimeException if the container cannot be created
     */
    IContainer create(Rectangle2D.Double worldSystem);
}