package edu.cnu.mdi.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SimulationContextTest {

    @Test
    public void testElapsedTimeIsZeroBeforeEngineStarts() {
        SimulationContext context = new SimulationContext();
        assertEquals(0.0, context.getElapsedSeconds());
    }

    @Test
    public void testElapsedTimeAdvancesAfterStart() throws InterruptedException {
        SimulationContext context = new SimulationContext();
        context.markStarted();
        Thread.sleep(2L);
        assertTrue(context.getElapsedSeconds() > 0.0);
    }
}
