package edu.cnu.mdi.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

public class SimulationEngineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Test
    public void testImmediateRunRequestSurvivesInitialization() throws Exception {
        CountDownLatch initEntered = new CountDownLatch(1);
        CountDownLatch releaseInit = new CountDownLatch(1);
        CountDownLatch stepped = new CountDownLatch(1);

        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) throws Exception {
                initEntered.countDown();
                assertTrue(releaseInit.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }

            @Override
            public boolean step(SimulationContext ctx) {
                stepped.countDown();
                return false;
            }
        };

        SimulationEngine engine = new SimulationEngine(simulation, config(false));
        engine.start();
        assertTrue(initEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        engine.requestRun();
        releaseInit.countDown();

        try {
            assertTrue(stepped.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                    "Run request was lost while initialization completed");
            awaitState(engine, SimulationState.TERMINATED);
            assertEquals(1L, engine.getContext().getStepCount());
        } finally {
            engine.requestStop();
        }
    }

    @Test
    public void testCancelWhileReadyUsesCancellationLifecycle() throws Exception {
        AtomicInteger cancelCalls = new AtomicInteger();
        AtomicInteger shutdownCalls = new AtomicInteger();
        AtomicInteger doneCalls = new AtomicInteger();
        List<SimulationState> states = new CopyOnWriteArrayList<>();

        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) {}

            @Override
            public boolean step(SimulationContext ctx) {
                return true;
            }

            @Override
            public void cancel(SimulationContext ctx) {
                cancelCalls.incrementAndGet();
            }

            @Override
            public void shutdown(SimulationContext ctx) {
                shutdownCalls.incrementAndGet();
            }
        };

        SimulationEngine engine = new SimulationEngine(simulation, config(false));
        engine.addListener(new SimulationListener() {
            @Override
            public void onStateChange(SimulationContext ctx, SimulationState from,
                    SimulationState to, String reason) {
                states.add(to);
            }

            @Override
            public void onDone(SimulationContext ctx) {
                doneCalls.incrementAndGet();
            }
        });

        engine.start();
        awaitState(engine, SimulationState.READY);
        engine.requestCancel();
        awaitState(engine, SimulationState.TERMINATED);
        flushEdt();

        assertEquals(1, cancelCalls.get());
        assertEquals(1, shutdownCalls.get());
        assertEquals(0, doneCalls.get());
        assertEquals(1, states.stream()
                .filter(state -> state == SimulationState.TERMINATING).count());
    }

    @Test
    public void testLifecycleCallbacksRunOnEdt() throws Exception {
        AtomicBoolean allOnEdt = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);
        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) {}

            @Override
            public boolean step(SimulationContext ctx) {
                return false;
            }
        };
        SimulationEngine engine = new SimulationEngine(simulation, config(true));
        engine.addListener(new SimulationListener() {
            private void checkThread() {
                allOnEdt.compareAndSet(true, SwingUtilities.isEventDispatchThread());
            }

            @Override
            public void onStateChange(SimulationContext ctx, SimulationState from,
                    SimulationState to, String reason) {
                checkThread();
            }

            @Override
            public void onRun(SimulationContext ctx) {
                checkThread();
            }

            @Override
            public void onDone(SimulationContext ctx) {
                checkThread();
                done.countDown();
            }
        });

        engine.start();
        assertTrue(done.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(allOnEdt.get());
        assertFalse(SwingUtilities.isEventDispatchThread());
    }

    @Test
    public void testFailureStillRunsShutdownAndReportsFailureOnEdt() throws Exception {
        AtomicInteger shutdownCalls = new AtomicInteger();
        AtomicBoolean failureOnEdt = new AtomicBoolean(false);
        CountDownLatch failed = new CountDownLatch(1);
        IllegalStateException failure = new IllegalStateException("step failed");

        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) {}

            @Override
            public boolean step(SimulationContext ctx) {
                throw failure;
            }

            @Override
            public void shutdown(SimulationContext ctx) {
                shutdownCalls.incrementAndGet();
            }
        };
        SimulationEngine engine = new SimulationEngine(simulation, config(true));
        engine.addListener(new SimulationListener() {
            @Override
            public void onFail(SimulationContext ctx, Throwable error) {
                failureOnEdt.set(SwingUtilities.isEventDispatchThread() && error == failure);
                failed.countDown();
            }
        });

        engine.start();
        assertTrue(failed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(SimulationState.FAILED, engine.getState());
        assertEquals(1, shutdownCalls.get());
        assertTrue(failureOnEdt.get());
    }

    @Test
    public void testAddingSameListenerTwiceDoesNotDuplicateCallbacks() throws Exception {
        AtomicInteger doneCalls = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) {}

            @Override
            public boolean step(SimulationContext ctx) {
                return false;
            }
        };
        SimulationListener listener = new SimulationListener() {
            @Override
            public void onDone(SimulationContext ctx) {
                doneCalls.incrementAndGet();
                done.countDown();
            }
        };
        SimulationEngine engine = new SimulationEngine(simulation, config(true));
        engine.addListener(listener);
        engine.addListener(listener);

        engine.start();
        assertTrue(done.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        flushEdt();
        assertEquals(1, doneCalls.get());
    }

    @Test
    public void testCancellationRequestIsIdempotent() throws Exception {
        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch initEntered = new CountDownLatch(1);
        CountDownLatch releaseInit = new CountDownLatch(1);
        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) throws Exception {
                initEntered.countDown();
                assertTrue(releaseInit.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }

            @Override
            public boolean step(SimulationContext ctx) {
                return true;
            }
        };
        SimulationEngine engine = new SimulationEngine(simulation, config(false));
        engine.addListener(new SimulationListener() {
            @Override
            public void onCancelRequested(SimulationContext ctx) {
                notifications.incrementAndGet();
            }
        });

        engine.start();
        assertTrue(initEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        engine.requestCancel();
        engine.requestCancel();
        releaseInit.countDown();
        awaitState(engine, SimulationState.TERMINATED);
        flushEdt();
        assertEquals(1, notifications.get());
    }

    @Test
    public void testStopDuringInitializationNeverReportsRunning() throws Exception {
        CountDownLatch initEntered = new CountDownLatch(1);
        CountDownLatch releaseInit = new CountDownLatch(1);
        AtomicInteger runCalls = new AtomicInteger();
        Simulation simulation = new Simulation() {
            @Override
            public void init(SimulationContext ctx) throws Exception {
                initEntered.countDown();
                assertTrue(releaseInit.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }

            @Override
            public boolean step(SimulationContext ctx) {
                return true;
            }
        };
        SimulationEngine engine = new SimulationEngine(simulation, config(true));
        engine.addListener(new SimulationListener() {
            @Override
            public void onRun(SimulationContext ctx) {
                runCalls.incrementAndGet();
            }
        });

        engine.start();
        assertTrue(initEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        engine.requestStop();
        releaseInit.countDown();
        awaitState(engine, SimulationState.TERMINATED);
        flushEdt();
        assertEquals(0, runCalls.get());
        assertEquals(0L, engine.getContext().getStepCount());
    }

    private static SimulationEngineConfig config(boolean autoRun) {
        return new SimulationEngineConfig(0, 0, 0, autoRun);
    }

    private static void awaitState(SimulationEngine engine, SimulationState expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (engine.getState() != expected && System.nanoTime() < deadline) {
            Thread.sleep(2L);
        }
        assertEquals(expected, engine.getState());
    }

    private static void flushEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return;
        SwingUtilities.invokeAndWait(() -> {});
    }
}
