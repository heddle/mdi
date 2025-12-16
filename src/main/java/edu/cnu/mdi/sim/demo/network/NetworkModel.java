package edu.cnu.mdi.sim.demo.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World-coordinate network model for the decluttering demo.
 * <p>
 * All coordinates are normalized to the unit square:
 * </p>
 * <ul>
 *   <li>{@code x in [0,1]}</li>
 *   <li>{@code y in [0,1]}</li>
 * </ul>
 * <p>
 * Servers are stored first, followed by clients. Every client connects to exactly
 * one server via an {@link Edge}.
 * </p>
 */
public final class NetworkModel {

    /** Node category. */
    public enum NodeType { SERVER, CLIENT }

    /**
     * A node in the unit-square world.
     * <p>
     * The simulation updates position and velocity; the view updates
     * {@link #worldRadius} each draw based on icon pixel sizes and current viewport.
     * </p>
     */
    public static final class Node {

        /** Unique node id within the model. */
        public final int id;

        /** Node type (server or client). */
        public final NodeType type;

        /** World position in the unit square. */
        public double x, y;

        /** World velocity in world-units per step. */
        public double vx, vy;

        /**
         * Approximate icon radius in world units.
         * <p>
         * This is set by the view each render pass. The simulation uses it for
         * overlap-aware repulsion and clamping so that icons remain on-screen.
         * </p>
         */
        public double worldRadius;

        Node(int id, NodeType type, double x, double y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Directed edge from a client node to a server node.
     * <p>
     * Indices refer to positions in {@link #nodes}.
     * </p>
     */
    public static final class Edge {

        /** Index of the client node in {@link #nodes}. */
        public final int clientIndex;

        /** Index of the server node in {@link #nodes}. */
        public final int serverIndex;

        Edge(int clientIndex, int serverIndex) {
            this.clientIndex = clientIndex;
            this.serverIndex = serverIndex;
        }
    }

    /** Nodes (servers first, then clients). */
    public final List<Node> nodes = new ArrayList<>();

    /** Each client has exactly one edge to a server. */
    public final List<Edge> edges = new ArrayList<>();

    /** Number of servers. */
    public final int serverCount;

    /** Number of clients. */
    public final int clientCount;

    private NetworkModel(int serverCount, int clientCount) {
        this.serverCount = serverCount;
        this.clientCount = clientCount;
    }

    /**
     * Create a random world-based network.
     *
     * @param serverCount number of servers (must be >= 1)
     * @param clientCount number of clients (must be >= 0)
     * @param rng random generator (non-null)
     * @return a randomized model in world coordinates
     */
    public static NetworkModel random(int serverCount, int clientCount, Random rng) {
        if (serverCount < 1) {
            throw new IllegalArgumentException("serverCount must be >= 1");
        }
        if (clientCount < 0) {
            throw new IllegalArgumentException("clientCount must be >= 0");
        }
        if (rng == null) {
            throw new IllegalArgumentException("rng must not be null");
        }

        NetworkModel m = new NetworkModel(serverCount, clientCount);

        // Servers
        for (int i = 0; i < serverCount; i++) {
            m.nodes.add(new Node(i, NodeType.SERVER, rng.nextDouble(), rng.nextDouble()));
        }

        // Clients + edges
        for (int i = 0; i < clientCount; i++) {
            int id = serverCount + i;
            int clientIndex = serverCount + i;

            m.nodes.add(new Node(id, NodeType.CLIENT, rng.nextDouble(), rng.nextDouble()));

            int serverIndex = rng.nextInt(serverCount);
            m.edges.add(new Edge(clientIndex, serverIndex));
        }

        return m;
    }
}
