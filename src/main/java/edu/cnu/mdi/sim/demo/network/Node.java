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
	 * <p>
	 * This is set by the view each render pass. The simulation uses it for
	 * overlap-aware repulsion and clamping so that icons remain on-screen.
	 * </p>
	 */
	public double worldRadius;

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
