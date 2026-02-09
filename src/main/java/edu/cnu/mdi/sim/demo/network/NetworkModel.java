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
 * <li>{@code x in [0,1]}</li>
 * <li>{@code y in [0,1]}</li>
 * </ul>
 * <p>
 * Servers are stored first, followed by clients. Every client connects to
 * exactly one server via an {@link Edge}.
 * </p>
 */
public final class NetworkModel {

	/**
	 * Directed edge from a node to another node.
	 * <p>
	 * Indices refer to positions in {@link #nodes}.
	 * </p>
	 */
	public static final class Edge {

		/** One node */
		public final Node node1;

		/** Other node */
		public final Node node2;

		Edge(Node node1, Node node2) {
			this.node1 = node1;
			this.node2 = node2;
		}
	}

	/** Nodes (servers, clients, printers). */
	public final List<Node> servers = new ArrayList<>();
	public final List<Node> clients = new ArrayList<>();
	public final List<Node> printers = new ArrayList<>();
	public final List<Node> nodes = new ArrayList<>(); //all nodes

	/** Each client has exactly one edge to a server. */
	public final List<Edge> edges = new ArrayList<>();

	/** Number of servers. */
	public final int serverCount;

	/** Number of clients. */
	public final int clientCount;
	
	/** Number of printers. */
	public final int printerCount;

	/**
	 * Private constructor to enforce use of the static factory method.
	 * @param serverCount number of servers
	 * @param clientCount number of clients
	 * @param printerCount number of printers
	 */
	private NetworkModel(int serverCount, int clientCount, int printerCount) {
		this.serverCount = serverCount;
		this.clientCount = clientCount;
		this.printerCount = printerCount;
	}

	/**
	 * Create a random world-based network.
	 *
	 * @param serverCount number of servers (must be >= 1)
	 * @param clientCount number of clients (must be >= 0)
	 * @param rng         random generator (non-null)
	 * @return a randomized model in world coordinates
	 */
	public static NetworkModel random(int serverCount, int clientCount, int printerCount, Random rng) {
		if (serverCount < 4) {
			throw new IllegalArgumentException("serverCount must be >= 4");
		}
		if (clientCount < 6) {
			throw new IllegalArgumentException("clientCount must be >= 6");
		}
		if (printerCount < 0) {
			throw new IllegalArgumentException("printerCount must be >= 0");
		}
		if (rng == null) {
			throw new IllegalArgumentException("rng must not be null");
		}

		NetworkModel model = new NetworkModel(serverCount, clientCount, printerCount);

		// Servers
		for (int i = 0; i < serverCount; i++) {
			model.servers.add(new Node(i, Node.NodeType.SERVER, rng.nextDouble(), rng.nextDouble()));
		}
		
		// Clients
		for (int i = 0; i < clientCount; i++) {
			model.clients.add(new Node(i, Node.NodeType.CLIENT, rng.nextDouble(), rng.nextDouble()));
		}
		
		// Printers
		for (int i = 0; i < printerCount; i++) {
			model.printers.add(new Node(i, Node.NodeType.PRINTER, rng.nextDouble(), rng.nextDouble()));
		}
		
		model.nodes.addAll(model.servers);
		model.nodes.addAll(model.clients);
		model.nodes.addAll(model.printers);

		// client-server connections
		for (Node client : model.clients) {
			//assign every client to a random server
			Node server = model.servers.get(rng.nextInt(serverCount));	
			model.edges.add(new Edge(client, server));
		}
		
		// printer connections
		for (Node printer : model.printers) {
			//assign every printer to some random (between 1 and 4) clients
			int numClients = 1 + rng.nextInt(4);
			// keep track for make sure assignments are unique
			List<Node> assignedClients = new ArrayList<>();
			for (int i = 0; i < numClients; i++) {
				Node client;
				do {
					client = model.clients.get(rng.nextInt(clientCount));
				} while (assignedClients.contains(client));
				assignedClients.add(client);
				model.edges.add(new Edge(client, printer));
					
			}
		}

		return model;
	}
}
