package edu.cnu.mdi.sim.simanneal;

import java.util.Objects;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationEngine;

/** Optional output channel for annealing status and UI hints. */
public interface AnnealingFeedback {
	void message(String message);
	void progress(ProgressInfo progress);
	void refresh();

	static AnnealingFeedback none() { return NoFeedback.INSTANCE; }

	/** Adapt the unchanged parent simulation engine to this output channel. */
	static AnnealingFeedback forEngine(SimulationEngine engine) {
		Objects.requireNonNull(engine, "engine");
		return new AnnealingFeedback() {
			@Override public void message(String message) { engine.postMessage(message); }
			@Override public void progress(ProgressInfo progress) { engine.postProgress(progress); }
			@Override public void refresh() { engine.requestRefresh(); }
		};
	}

	enum NoFeedback implements AnnealingFeedback {
		INSTANCE;
		@Override public void message(String message) { }
		@Override public void progress(ProgressInfo progress) { }
		@Override public void refresh() { }
	}
}
