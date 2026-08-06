package edu.cnu.mdi.sim.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.CompletionStatus;
import edu.cnu.mdi.sim.SimulationEngineConfig;

class TaskHandleTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@Test
	void returnsTypedResultAndDeliversCallbacksOnEdt() throws Exception {
		TaskHandle<Integer> handle = BackgroundTasks.create(context -> {
			context.reportProgress(0.5, "halfway");
			context.postMessage("working");
			return 42;
		});
		CountDownLatch completed = new CountDownLatch(1);
		AtomicBoolean allOnEdt = new AtomicBoolean(true);
		AtomicInteger completedCalls = new AtomicInteger();
		handle.addListener(new TaskListener<>() {
			private void checkEdt() {
				allOnEdt.compareAndSet(true, SwingUtilities.isEventDispatchThread());
			}
			@Override public void onStarted(TaskHandle<Integer> source) { checkEdt(); }
			@Override public void onProgress(TaskHandle<Integer> source,
					edu.cnu.mdi.sim.ProgressInfo progress) { checkEdt(); }
			@Override public void onMessage(TaskHandle<Integer> source, String message) { checkEdt(); }
			@Override public void onSucceeded(TaskHandle<Integer> source, Integer result) {
				checkEdt();
				assertEquals(42, result);
			}
			@Override public void onCompleted(TaskHandle<Integer> source,
					CompletionStatus status, Throwable error) {
				checkEdt();
				completedCalls.incrementAndGet();
				completed.countDown();
			}
		});

		handle.start();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(CompletionStatus.SUCCEEDED, handle.getCompletionStatus());
		assertEquals(42, handle.getResult());
		assertNull(handle.getFailure());
		assertEquals(1, completedCalls.get());
		assertTrue(allOnEdt.get());
	}

	@Test
	void cooperativeCancellationIsNotReportedAsFailure() throws Exception {
		CountDownLatch entered = new CountDownLatch(1);
		TaskHandle<Void> handle = BackgroundTasks.create(context -> {
			entered.countDown();
			while (true) {
				context.throwIfCancellationRequested();
				Thread.sleep(1L);
			}
		});
		CountDownLatch completed = new CountDownLatch(1);
		handle.addListener(new TaskListener<>() {
			@Override public void onCompleted(TaskHandle<Void> source,
					CompletionStatus status, Throwable error) {
				completed.countDown();
			}
		});
		handle.start();
		assertTrue(entered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		handle.cancel();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(CompletionStatus.CANCELLED, handle.getCompletionStatus());
		assertNull(handle.getFailure());
	}

	@Test
	void cancellationBeforeStartPreventsTaskBody() throws Exception {
		AtomicBoolean executed = new AtomicBoolean();
		TaskHandle<Void> handle = new TaskHandle<>(context -> {
			executed.set(true);
			return null;
		});
		CountDownLatch completed = new CountDownLatch(1);
		handle.addListener(new TaskListener<>() {
			@Override public void onCompleted(TaskHandle<Void> source,
					CompletionStatus status, Throwable error) { completed.countDown(); }
		});
		handle.cancel();
		handle.start();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertFalse(executed.get());
		assertEquals(CompletionStatus.CANCELLED, handle.getCompletionStatus());
	}

	@Test
	void preservesTaskFailureIdentity() throws Exception {
		IllegalArgumentException failure = new IllegalArgumentException("bad input");
		TaskHandle<Void> handle = new TaskHandle<>(context -> { throw failure; });
		CountDownLatch completed = new CountDownLatch(1);
		handle.addListener(new TaskListener<>() {
			@Override public void onCompleted(TaskHandle<Void> source,
					CompletionStatus status, Throwable error) { completed.countDown(); }
		});
		handle.start();
		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		assertEquals(CompletionStatus.FAILED, handle.getCompletionStatus());
		assertSame(failure, handle.getFailure());
	}

	@Test
	void rejectsNonAutoRunConfiguration() {
		SimulationEngineConfig config = new SimulationEngineConfig(0, 0, 0, false);
		assertThrows(IllegalArgumentException.class,
				() -> new TaskHandle<>(context -> 1, config));
	}
}
