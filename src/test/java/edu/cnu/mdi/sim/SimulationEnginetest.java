package edu.cnu.mdi.sim;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static void await(CountDownLatch latch, long timeoutMs, String what) {
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                fail("Timed out waiting for: " + what);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            fail("Interrupted waiting for: " + what);
        }
    }

    /** Ensures any already-posted EDT runnables have had a chance to execute. */
    private static void flushEdt() {
        try {
            SwingUtilities.invokeAndWait(() -> { /* no-op */ });
        } catch (Exception e) {
            // In headless mode this should still work; if it doesn't, tests will reveal it.
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------
    // Test doubles
    // ------------------------------------------------------------

    private static final class RecordingListener implements SimulationListener {

        final AtomicBoolean anyCallbackNotOnEdt = new AtomicBoolean(false);

        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final CountDownLatch runLatch = new CountDownLatch(1);
        final CountDownLatch pauseLatch = new CountDownLatch(1);
        final CountDownLatch resumeLatch = new CountDownLatch(1);
        final CountDownLatch cancelRequestedLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);
        final CountDownLatch failLatch = new CountDownLatch(1);

        volatile SimulationState lastFrom;
        volatile SimulationState lastTo;
        volatile Throwable failError;

        private void checkEdt() {
            if (!SwingUtilities.isEventDispatchThread()) {
                anyCallbackNotOnEdt.set(true);
            }
        }

        @Override
        public void onInit(SimulationContext ctx) {
            checkEdt();
            initLatch.countDown();
        }

        @Override
        public void onReady(SimulationContext ctx) {
            checkEdt();
            readyLatch.countDown();
        }

        @Override
        public void onRun(SimulationContext ctx) {
            checkEdt();
            runLatch.countDown();
        }

        @Override
        public void onPause(SimulationContext ctx) {
            checkEdt();
            pauseLatch.countDown();
        }

        @Override
        public void onResume(SimulationContext ctx) {
            checkEdt();
            resumeLatch.countDown();
        }

        @Override
        public void onCancelRequested(SimulationContext ctx) {
            checkEdt();
            cancelRequestedLatch.countDown();
        }

        @Override
        public void onDone(SimulationContext ctx) {
            checkEdt();
            doneLatch.countDown();
        }

        @Override
        public void onFail(SimulationContext ctx, Throwable error) {
            checkEdt();
            failError = error;
            failLatch.countDown();
        }

        @Override
        public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
            checkEdt();
            lastFrom = from;
            lastTo = to;
        }
    }

    private static final class CountingSimulation implements Simulation {
        final AtomicInteger initCalls = new AtomicInteger(0);
        final AtomicInteger stepCalls = new AtomicInteger(0);
        final AtomicInteger shutdownCalls = new AtomicInteger(0);
        final AtomicInteger cancelCalls = new AtomicInteger(0);

        private final int stepsToRun; // stop after this many step() calls
        private volatile boolean throwInStep = false;

        CountingSimulation(int stepsToRun) {
            this.stepsToRun = stepsToRun;
        }

        @Override
        public void init(SimulationContext ctx) throws Exception {
            initCalls.incrementAndGet();
        }

        @Override
        public boolean step(SimulationContext ctx) throws Exception {
            int n = stepCalls.incrementAndGet();
            if (throwInStep) {
                throw new RuntimeException("boom");
            }
            return n < stepsToRun;
        }

        @Override
        public void shutdown(SimulationContext ctx) throws Exception {
            shutdownCalls.incrementAndGet();
        }

        @Override
        public void cancel(SimulationContext ctx) throws Exception {
            cancelCalls.incrementAndGet();
        }
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    void initReachesReadyAndDoesNotAutoRunByDefault() {
        CountingSimulation sim = new CountingSimulation(3);
        // autoRun=false is the key for deterministic tests
        SimulationEngineConfig cfg = new SimulationEngineConfig(0, 0, 0, false);
        SimulationEngine engine = new SimulationEngine(sim, cfg);

        RecordingListener listener = new RecordingListener();
        engine.addListener(listener);

        assertEquals(SimulationState.NEW, engine.getState());

        engine.start();

        await(listener.initLatch, 1000, "onInit");
        await(listener.readyLatch, 1000, "onReady");
        flushEdt();

        assertEquals(1, sim.initCalls.get());
        assertEquals(SimulationState.READY, engine.getState());

        // Should not have run any steps yet because autoRun=false and we haven't requested run.
        assertEquals(0, sim.stepCalls.get());
        assertEquals(0L, engine.getContext().getStepCount());

        assertFalse(listener.anyCallbackNotOnEdt.get(), "Expected all listener callbacks on EDT");
    }

    @Test
    void runToCompletionIncrementsStepCountAndCallsShutdown() {
        int steps = 5;
        CountingSimulation sim = new CountingSimulation(steps);
        SimulationEngine engine = new SimulationEngine(sim, new SimulationEngineConfig(0, 0, 0, false));

        RecordingListener listener = new RecordingListener();
        engine.addListener(listener);

        engine.start();
        await(listener.readyLatch, 1000, "READY");

        engine.requestRun();
        await(listener.runLatch, 1000, "RUNNING");

        // Engine should terminate naturally once step() returns false.
        await(listener.doneLatch, 2000, "TERMINATED / done");
        flushEdt();

        assertEquals(SimulationState.TERMINATED, engine.getState());
        assertEquals(1, sim.initCalls.get());
        assertEquals(steps, sim.stepCalls.get());
        assertEquals(steps, engine.getContext().getStepCount(), "Engine increments stepCount once per step()");
        assertEquals(1, sim.shutdownCalls.get(), "shutdown should be called on normal termination");
        assertEquals(0, sim.cancelCalls.get(), "cancel should not be called on normal termination");
    }

    @Test
    void pauseStopsSteppingAndResumeContinues() {
        // Run many steps so we have time to pause in the middle.
        CountingSimulation sim = new CountingSimulation(10_000);
        SimulationEngine engine = new SimulationEngine(sim, new SimulationEngineConfig(0, 0, 0, false));

        RecordingListener listener = new RecordingListener();
        engine.addListener(listener);

        engine.start();
        await(listener.readyLatch, 1000, "READY");

        engine.requestRun();
        await(listener.runLatch, 1000, "RUNNING");

        // Wait until at least some steps have occurred.
        long start = System.nanoTime();
        while (engine.getContext().getStepCount() < 50 && (System.nanoTime() - start) < 1_000_000_000L) {
            Thread.yield();
        }
        long beforePause = engine.getContext().getStepCount();
        assertTrue(beforePause >= 1, "Expected at least one step before pausing");

        engine.requestPause();
        await(listener.pauseLatch, 1000, "PAUSED");
        flushEdt();

        long pausedCount1 = engine.getContext().getStepCount();
        try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        long pausedCount2 = engine.getContext().getStepCount();

        assertEquals(pausedCount1, pausedCount2, "Step count should not increase while paused");

        // Resume and confirm stepping continues
        engine.requestResume();

        // There is no explicit onResume() call in your engine right now (only onPause/onRun),
        // so we simply check that step count starts increasing again.
        start = System.nanoTime();
        while (engine.getContext().getStepCount() <= pausedCount2 && (System.nanoTime() - start) < 1_000_000_000L) {
            Thread.yield();
        }
        assertTrue(engine.getContext().getStepCount() > pausedCount2, "Step count should increase after resume");

        // Clean shutdown
        engine.requestStop();
        await(listener.doneLatch, 2000, "TERMINATED / done after stop");
        assertEquals(SimulationState.TERMINATED, engine.getState());
        assertEquals(1, sim.shutdownCalls.get());
    }

    @Test
    void cancelRequestsCancelHookAndTerminates() {
        CountingSimulation sim = new CountingSimulation(10_000);
        SimulationEngine engine = new SimulationEngine(sim, new SimulationEngineConfig(0, 0, 0, false));

        RecordingListener listener = new RecordingListener();
        engine.addListener(listener);

        engine.start();
        await(listener.readyLatch, 1000, "READY");

        engine.requestRun();
        await(listener.runLatch, 1000, "RUNNING");

        engine.requestCancel();
        await(listener.cancelRequestedLatch, 1000, "cancel requested callback");

        // Engine should call sim.cancel() and then shutdown() and terminate
        await(listener.doneLatch, 2000, "TERMINATED / done after cancel");
        flushEdt();

        assertTrue(engine.getContext().isCancelRequested(), "Context cancel flag should be set");
        assertEquals(SimulationState.TERMINATED, engine.getState());
        assertEquals(1, sim.cancelCalls.get(), "cancel hook should be called");
        assertEquals(1, sim.shutdownCalls.get(), "shutdown should still be called after cancel");
    }
}