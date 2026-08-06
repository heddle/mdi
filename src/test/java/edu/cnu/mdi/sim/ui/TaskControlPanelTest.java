package edu.cnu.mdi.sim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.CompletionStatus;
import edu.cnu.mdi.sim.task.TaskHandle;
import edu.cnu.mdi.sim.task.TaskListener;

class TaskControlPanelTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@Test
	void startButtonRunsTaskAndPanelReflectsCompletion() throws Exception {
		TaskHandle<String> handle = new TaskHandle<>(context -> {
			context.reportProgress(0.5, "Halfway");
			return "answer";
		});
		CountDownLatch completed = new CountDownLatch(1);
		handle.addListener(new TaskListener<>() {
			@Override public void onCompleted(TaskHandle<String> source,
					CompletionStatus status, Throwable error) { completed.countDown(); }
		});
		AtomicReference<TaskControlPanel<String>> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			TaskControlPanel<String> panel = new TaskControlPanel<>(handle);
			panelRef.set(panel);
			assertTrue(panel.getStartButton().isEnabled());
			panel.getStartButton().doClick();
		});

		assertTrue(completed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
		SwingUtilities.invokeAndWait(() -> {
			TaskControlPanel<String> panel = panelRef.get();
			assertEquals("Done", panel.getStatusLabel().getText());
			assertEquals(100, panel.getProgressBar().getValue());
			assertFalse(panel.getStartButton().isEnabled());
			assertFalse(panel.getCancelButton().isEnabled());
		});
	}

	@Test
	void unbindDoesNotCancelTask() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		TaskHandle<Void> handle = new TaskHandle<>(context -> {
			release.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			return null;
		});
		AtomicReference<TaskControlPanel<Void>> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			TaskControlPanel<Void> panel = new TaskControlPanel<>(handle);
			panelRef.set(panel);
			panel.getStartButton().doClick();
			panel.unbind();
			assertEquals(null, panel.getTaskHandle());
		});
		release.countDown();
		assertTrue(handle.awaitTermination(TIMEOUT.toMillis()));
		assertEquals(CompletionStatus.SUCCEEDED, awaitPublishedStatus(handle));
	}

	private static CompletionStatus awaitPublishedStatus(TaskHandle<?> handle) throws Exception {
		long deadline = System.nanoTime() + TIMEOUT.toNanos();
		while (handle.getCompletionStatus() == null && System.nanoTime() < deadline) {
			SwingUtilities.invokeAndWait(() -> { });
			Thread.sleep(1L);
		}
		return handle.getCompletionStatus();
	}
}
