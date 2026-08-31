package edu.cnu.mdi.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SimulationCompletionTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@Test
	void reportsNaturalSuccessExactlyOnceOnEdt() throws Exception {
		assertCompletion(new Simulation() {
			@Override public void init(SimulationContext context) { }
			@Override public boolean step(SimulationContext context) { return false; }
		}, CompletionStatus.SUCCEEDED, null, null);
	}

	@Test
	void distinguishesRequestedStopFromNaturalSuccess() throws Exception {
		CountDownLatch initialized = new CountDownLatch(1);
		SimulationEngine engine = new SimulationEngine(new Simulation() {
			@Override public void init(SimulationContext context) { initialized.countDown(); }
			@Override public boolean step(SimulationContext context) { return true; }
		}, config(false));
		CountDownLatch completed = new CountDownLatch(1);
		CompletionCapture capture = new CompletionCapture(completed);
		engine.addListener(capture);
		engine.start();
		assertTrue(initialized.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		engine.requestStop();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(CompletionStatus.STOPPED, capture.status);
		assertEquals(1, capture.calls.get());
		assertTrue(capture.onEdt);
	}

	@Test
	void reportsCancellationExactlyOnce() throws Exception {
		CountDownLatch entered = new CountDownLatch(1);
		SimulationEngine engine = new SimulationEngine(new Simulation() {
			@Override public void init(SimulationContext context) { }
			@Override public boolean step(SimulationContext context) throws Exception {
				entered.countDown();
				while (!context.isCancelRequested()) {
					Thread.sleep(1L);
				}
				return false;
			}
		}, config(true));
		CountDownLatch completed = new CountDownLatch(1);
		CompletionCapture capture = new CompletionCapture(completed);
		engine.addListener(capture);
		engine.start();
		assertTrue(entered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		engine.requestCancel();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(CompletionStatus.CANCELLED, capture.status);
		assertNull(capture.error);
		assertEquals(1, capture.calls.get());
	}

	@Test
	void reportsOriginalFailure() throws Exception {
		IllegalStateException failure = new IllegalStateException("expected");
		assertCompletion(new Simulation() {
			@Override public void init(SimulationContext context) { }
			@Override public boolean step(SimulationContext context) { throw failure; }
		}, CompletionStatus.FAILED, failure, failure);
	}

	private static void assertCompletion(Simulation simulation, CompletionStatus expected,
			Throwable expectedError, Throwable identity) throws Exception {
		SimulationEngine engine = new SimulationEngine(simulation, config(true));
		CountDownLatch completed = new CountDownLatch(1);
		CompletionCapture capture = new CompletionCapture(completed);
		engine.addListener(capture);
		engine.start();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(expected, capture.status);
		assertEquals(1, capture.calls.get());
		assertTrue(capture.onEdt);
		if (identity == null) {
			assertEquals(expectedError, capture.error);
		} else {
			assertSame(identity, capture.error);
		}
	}

	private static SimulationEngineConfig config(boolean autoRun) {
		return new SimulationEngineConfig(0, 0, 0, autoRun);
	}

	private static final class CompletionCapture implements SimulationListener {
		private final CountDownLatch latch;
		private final AtomicInteger calls = new AtomicInteger();
		private volatile CompletionStatus status;
		private volatile Throwable error;
		private volatile boolean onEdt;

		private CompletionCapture(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onCompleted(SimulationContext context, CompletionStatus completionStatus,
				Throwable completionError) {
			status = completionStatus;
			error = completionError;
			onEdt = SwingUtilities.isEventDispatchThread();
			calls.incrementAndGet();
			latch.countDown();
		}
	}
}
