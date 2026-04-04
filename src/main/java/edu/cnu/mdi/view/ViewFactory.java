package edu.cnu.mdi.view;

/**
 * Functional interface used to create a view instance.
 * <p>
 * This replaces the previous reflection-based convention that required each
 * lazily-created view class to expose a static
 * {@code construct(Object... keyVals)} method.
 * </p>
 * <p>
 * A {@link ViewFactory} may be implemented using:
 * </p>
 * <ul>
 * <li>a constructor reference, e.g. {@code MyView::new}</li>
 * <li>a lambda, e.g. {@code () -> new MyView(defaultKeyVals())}</li>
 * <li>an anonymous class where older syntax is preferred</li>
 * </ul>
 *
 * @param <T> the concrete {@link BaseView} subtype created by this factory
 */
@FunctionalInterface
public interface ViewFactory<T extends BaseView> {

	/**
	 * Create a new view instance.
	 *
	 * @return the newly created view, never {@code null}
	 */
	T create();
}