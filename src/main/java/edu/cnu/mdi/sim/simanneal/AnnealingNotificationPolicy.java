package edu.cnu.mdi.sim.simanneal;

/**
 * Delivery limits for accepted-move listener notifications.
 *
 * @param maximumQueued maximum notifications waiting for the EDT
 * @param maximumPerDrain maximum notifications delivered by one EDT task
 * @param acceptedMoveStride deliver every nth accepted-move notification
 */
public record AnnealingNotificationPolicy(
		int maximumQueued,
		int maximumPerDrain,
		int acceptedMoveStride) {

	public AnnealingNotificationPolicy {
		if (maximumQueued <= 0 || maximumPerDrain <= 0
				|| acceptedMoveStride <= 0) {
			throw new IllegalArgumentException("notification limits must be positive");
		}
	}

	/** @return policy that delivers every accepted move with bounded batching */
	public static AnnealingNotificationPolicy defaults() {
		return new AnnealingNotificationPolicy(10_000, 1_000, 1);
	}
}
