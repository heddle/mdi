package edu.cnu.mdi.sim.demo.network;

public class Node {

	/** Node category. */
	public enum NodeType {
		SERVER, CLIENT, PRINTER
	}

	/** Unique node id within the model. */
	public final int id;

	/** Node type (server or client). */
	public final NodeType type;

	/** World position in the unit square. */
	public double x, y;

	/** World velocity in world-units per step. */
	public double vx, vy;

	/** Force on the node */
	public double fx, fy;

	/**
	 * Approximate icon radius in world units.
	 *
	 * <p>Set by the view after construction and again whenever the viewport zoom
	 * or size changes. The simulation reads this field on the simulation thread
	 * to compute overlap-aware repulsion and to clamp positions so icons remain
	 * fully visible inside the unit square.</p>
	 *
	 * <p>Declared {@code volatile} because the view writes it on the EDT while
	 * the simulation thread reads it concurrently. A stale value following a
	 * just-completed zoom is harmless — it affects at most one simulation step —
	 * but the {@code volatile} guarantee ensures the simulation thread will see
	 * the updated value on the immediately following read without requiring an
	 * explicit lock or memory barrier.</p>
	 */
	public volatile double worldRadius;

	/**
	 * Construct a new node.
	 * @param id unique node id
	 * @param type node type
	 * @param x initial x position
	 * @param y initial y position
	 */
	Node(int id, NodeType type, double x, double y) {
		this.id = id;
		this.type = type;
		this.x = x;
		this.y = y;
	}

}