package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseContainer;

/**
 * Construction, naming, property helpers and layout persistence of
 * {@link BaseView}. Views are built with {@code visible(false)} so the
 * deferred {@code setVisible(true)} the constructor would otherwise queue
 * cannot race with the assertions.
 */
class BaseViewTest {

	private static <T> T onEdt(Supplier<T> body) {
		Object[] result = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = body.get());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) throw re;
			if (cause instanceof Error err) throw err;
			throw new AssertionError(cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while running Swing test", e);
		}
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	private static void onEdt(Runnable body) {
		onEdt(() -> {
			body.run();
			return null;
		});
	}

	private static BaseView viewWithContainer(String title) {
		return onEdt(() -> new BaseView(new ViewPropertiesBuilder()
				.title(title)
				.visible(false)
				.width(400)
				.height(300)
				.worldSystem(new Rectangle2D.Double(0, 0, 10, 5))
				.build()));
	}

	private static BaseView viewWithoutContainer(String title) {
		return onEdt(() -> new BaseView(new ViewPropertiesBuilder()
				.title(title)
				.visible(false)
				.useContainer(false)
				.width(320)
				.height(240)
				.build()));
	}

	// ------------------------------------------------------- key sanitising

	@Test
	void sanitizeForKeyReplacesUnsafeCharactersWithUnderscores() {
		assertEquals("Sample_2D_Map_View", BaseView.sanitizeForKey("Sample 2D Map View"));
		assertEquals("a_b_c_d", BaseView.sanitizeForKey("a.b/c:d"));
		assertEquals("keep-this_and9", BaseView.sanitizeForKey("keep-this_and9"));
	}

	@Test
	void sanitizeForKeyTrimsOuterWhitespaceButNotInnerWhitespace() {
		assertEquals("Two_Words", BaseView.sanitizeForKey("  Two Words \t"));
	}

	@Test
	void sanitizeForKeyFallsBackToViewForNullOrBlank() {
		assertEquals("view", BaseView.sanitizeForKey(null));
		assertEquals("view", BaseView.sanitizeForKey(""));
		assertEquals("view", BaseView.sanitizeForKey("   "));
	}

	// ---------------------------------------------------------- construction

	@Test
	void titleDrivesTheNameAndThePersistenceKey() {
		BaseView view = viewWithContainer("My Test View");

		assertEquals("My Test View", view.getTitle());
		assertEquals("My Test View", view.getName());
		assertEquals("My_Test_View", view.getPropertyName());
	}

	@Test
	void aMissingTitleGetsTheDefaultTitle() {
		BaseView view = onEdt(() -> new BaseView(new ViewPropertiesBuilder()
				.visible(false).width(300).height(200).useContainer(false).build()));

		assertEquals("A View", view.getTitle());
		assertEquals("A_View", view.getPropertyName());
	}

	@Test
	void aWorldSystemYieldsADefaultBaseContainerWithStandardLayers() {
		BaseView view = viewWithContainer("Container View");

		assertInstanceOf(BaseContainer.class, view.getIContainer());
		assertNotNull(view.getDefaultLayer());
		assertNotNull(view.getAnnotationLayer());
		assertNotNull(view.getConnectionLayer());
		assertSame(view.getIContainer().getDefaultLayer(), view.getDefaultLayer());
		assertEquals(new Rectangle2D.Double(0, 0, 10, 5), view.getIContainer().getWorldSystem());
	}

	@Test
	void aContainerlessViewHasNoContainerAndNoLayers() {
		BaseView view = viewWithoutContainer("Plain View");

		assertNull(view.getIContainer());
		assertNull(view.getDefaultLayer());
		assertNull(view.getAnnotationLayer());
		assertNull(view.getConnectionLayer());
	}

	@Test
	void explicitSizeIsHonouredForAContainerlessView() {
		BaseView view = viewWithoutContainer("Sized View");

		assertEquals(320, view.getWidth());
		assertEquals(240, view.getHeight());
	}

	@Test
	void aContainerFactoryIsPreferredOverTheDefaultContainer() {
		BaseContainer custom = onEdt(() -> new BaseContainer(new Rectangle2D.Double(0, 0, 4, 4)));
		BaseView view = onEdt(() -> new BaseView(new ViewPropertiesBuilder()
				.title("Factory View")
				.visible(false)
				.width(300)
				.height(300)
				.worldSystem(new Rectangle2D.Double(0, 0, 4, 4))
				.containerFactory(world -> custom)
				.build()));

		assertSame(custom, view.getIContainer());
		assertSame(view, custom.getView());
	}

	@Test
	void theOptionsConstructorMatchesThePropertiesConstructor() {
		ViewOptions options = new ViewPropertiesBuilder()
				.title("Options View").visible(false).width(300).height(200).useContainer(false)
				.buildOptions();

		BaseView view = onEdt(() -> new BaseView(options));

		assertEquals("Options View", view.getTitle());
		assertEquals("Options_View", view.getPropertyName());
		assertNull(view.getIContainer());
	}

	@Test
	void nullOptionsAreRejected() {
		org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
				() -> new BaseView((ViewOptions) null));
	}

	// ---------------------------------------------------- property helpers

	@Test
	void booleanAndIntPropertiesRoundTripAndDefaultWhenAbsent() {
		BaseView view = viewWithoutContainer("Props View");

		assertFalse(view.checkBooleanProperty("flag"));
		assertEquals(-1, view.getIntProperty("count"));

		view.setBooleanProperty("flag", true);
		view.setIntProperty("count", 42);

		assertTrue(view.checkBooleanProperty("flag"));
		assertEquals(42, view.getIntProperty("count"));
		assertEquals("true", view.getProperties().get("flag"));
		assertEquals("42", view.getProperties().get("count"));
	}

	@Test
	void anUnparseableIntPropertyReadsAsMinusOne() {
		BaseView view = viewWithoutContainer("Bad Int View");
		view.getProperties().put("count", "not a number");

		assertEquals(-1, view.getIntProperty("count"));
	}

	// ----------------------------------------------------------- file filter

	@Test
	void fileFilterIsStoredAndReplaceable() {
		BaseView view = viewWithoutContainer("Drop View");
		assertNull(view.getFileFilter());

		Predicate<File> shapefiles = f -> f.getName().endsWith(".shp");
		view.setFileFilter(shapefiles);
		assertSame(shapefiles, view.getFileFilter());
		assertTrue(view.getFileFilter().test(new File("a.shp")));
		assertFalse(view.getFileFilter().test(new File("a.txt")));

		view.setFileFilter(null);
		assertNull(view.getFileFilter());
	}

	// ----------------------------------------------------------- persistence

	@Test
	void capturedStateUsesDottedKeysUnderThePropertyName() {
		BaseView view = viewWithContainer("Capture View");
		onEdt(() -> view.setBounds(11, 22, 333, 244));

		Properties captured = view.getConfigurationProperties();

		assertEquals("11", captured.getProperty("Capture_View.x"));
		assertEquals("22", captured.getProperty("Capture_View.y"));
		assertEquals("333", captured.getProperty("Capture_View.w"));
		assertEquals("244", captured.getProperty("Capture_View.h"));
		assertEquals("false", captured.getProperty("Capture_View.visible"));
		assertEquals("false", captured.getProperty("Capture_View.maximized"));
		assertNotNull(captured.getProperty("Capture_View.ontop"));
	}

	@Test
	void capturedStateIncludesTheWorldWindowOnlyWhenThereIsAContainer() {
		BaseView withContainer = viewWithContainer("World View");
		BaseView without = viewWithoutContainer("No World View");

		Properties a = withContainer.getConfigurationProperties();
		Properties b = without.getConfigurationProperties();

		for (String edge : new String[] { "xmin", "ymin", "xmax", "ymax" }) {
			assertNotNull(a.getProperty("World_View." + edge), edge);
			assertNull(b.getProperty("No_World_View." + edge), edge);
		}
	}

	@Test
	void savedBoundsRestoreExactly() {
		BaseView original = viewWithoutContainer("Round Trip");
		onEdt(() -> original.setBounds(15, 25, 310, 210));
		Properties saved = original.getConfigurationProperties();

		BaseView restored = viewWithoutContainer("Round Trip");
		onEdt(() -> restored.setFromProperties(saved));

		assertEquals(new Rectangle(15, 25, 310, 210), restored.getBounds());
	}

	@Test
	void legacyUndottedKeysAreStillRead() {
		BaseView view = viewWithoutContainer("Legacy View");
		Properties legacy = new Properties();
		legacy.put("Legacy_Viewx", "5");
		legacy.put("Legacy_Viewy", "6");
		legacy.put("Legacy_Vieww", "250");
		legacy.put("Legacy_Viewh", "150");

		onEdt(() -> view.setFromProperties(legacy));

		assertEquals(new Rectangle(5, 6, 250, 150), view.getBounds());
	}

	@Test
	void legacyWidthAndHeightSpellingsAreAccepted() {
		BaseView view = viewWithoutContainer("Wide View");
		Properties saved = new Properties();
		saved.put("Wide_View.x", "1");
		saved.put("Wide_View.y", "2");
		saved.put("Wide_View.width", "260");
		saved.put("Wide_View.height", "160");

		onEdt(() -> view.setFromProperties(saved));

		assertEquals(new Rectangle(1, 2, 260, 160), view.getBounds());
	}

	@Test
	void incompleteOrInvalidBoundsLeaveTheViewWhereItIs() {
		BaseView view = viewWithoutContainer("Stay Put");
		Rectangle before = view.getBounds();

		Properties missingHeight = new Properties();
		missingHeight.put("Stay_Put.x", "1");
		missingHeight.put("Stay_Put.y", "2");
		missingHeight.put("Stay_Put.w", "300");
		onEdt(() -> view.setFromProperties(missingHeight));
		assertEquals(before, view.getBounds(), "height missing");

		Properties zeroWidth = new Properties();
		zeroWidth.put("Stay_Put.x", "1");
		zeroWidth.put("Stay_Put.y", "2");
		zeroWidth.put("Stay_Put.w", "0");
		zeroWidth.put("Stay_Put.h", "100");
		onEdt(() -> view.setFromProperties(zeroWidth));
		assertEquals(before, view.getBounds(), "non-positive width");

		Properties garbage = new Properties();
		garbage.put("Stay_Put.x", "left");
		garbage.put("Stay_Put.y", "2");
		garbage.put("Stay_Put.w", "300");
		garbage.put("Stay_Put.h", "100");
		onEdt(() -> view.setFromProperties(garbage));
		assertEquals(before, view.getBounds(), "non-numeric x");
	}

	@Test
	void nullAndEmptyPropertiesAreIgnored() {
		BaseView view = viewWithoutContainer("Ignore View");
		Rectangle before = view.getBounds();

		onEdt(() -> {
			view.setFromProperties(null);
			view.setFromProperties(new Properties());
		});

		assertEquals(before, view.getBounds());
	}

	@Test
	void propertiesForOtherViewsAreNotApplied() {
		BaseView view = viewWithoutContainer("Mine");
		Rectangle before = view.getBounds();
		Properties other = new Properties();
		other.put("Theirs.x", "1");
		other.put("Theirs.y", "2");
		other.put("Theirs.w", "500");
		other.put("Theirs.h", "500");

		onEdt(() -> view.setFromProperties(other));

		assertEquals(before, view.getBounds());
	}
}
