package edu.cnu.mdi.sim.ga;

import java.util.Objects;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationEngine;

/** Optional output channel for genetic-algorithm status and UI hints. */
public interface GAFeedback {
	/** Publish a status message. */
	void message(String message);
	/** Publish progress. */
	void progress(ProgressInfo progress);
	/** Request that a presentation refresh itself. */
	void refresh();

	/** @return a feedback channel that discards all output */
	static GAFeedback none() {
		return NoFeedback.INSTANCE;
	}

	/** Adapt the unchanged parent simulation engine to this output channel. */
	static GAFeedback forEngine(SimulationEngine engine) {
		Objects.requireNonNull(engine, "engine");
		return new GAFeedback() {
			@Override public void message(String message) { engine.postMessage(message); }
			@Override public void progress(ProgressInfo progress) { engine.postProgress(progress); }
			@Override public void refresh() { engine.requestRefresh(); }
		};
	}

	enum NoFeedback implements GAFeedback {
		INSTANCE;
		@Override public void message(String message) { }
		@Override public void progress(ProgressInfo progress) { }
		@Override public void refresh() { }
	}
}
