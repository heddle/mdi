package edu.cnu.mdi.sim.demo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;
import edu.cnu.mdi.sim.SimulationEngineConfig;

class NetworkDeclutterSimulationTest {

	@Test
	void randomModelHasExpectedMembershipAndConnections() {
		NetworkModel model = NetworkModel.random(4, 12, 3, new Random(17L));

		assertEquals(4, model.servers.size());
		assertEquals(12, model.clients.size());
		assertEquals(3, model.printers.size());
		assertEquals(19, model.nodes.size());
		assertTrue(model.edges.size() >= 15);

		for (Node client : model.clients) {
			long serverEdges = model.edges.stream()
					.filter(edge -> edge.node1 == client
							&& edge.node2.type == Node.NodeType.SERVER)
					.count();
			assertEquals(1, serverEdges);
		}
		for (Node printer : model.printers) {
			var assignedClients = new HashSet<Node>();
			model.edges.stream()
					.filter(edge -> edge.node2 == printer)
					.forEach(edge -> assignedClients.add(edge.node1));
			long printerEdges = model.edges.stream()
					.filter(edge -> edge.node2 == printer).count();
			assertEquals(printerEdges, assignedClients.size());
			assertTrue(printerEdges >= 1 && printerEdges <= 4);
		}
	}

	@Test
	void simulationMaintainsFiniteBoundedStateAndProducesDiagnostics() throws Exception {
		NetworkModel model = NetworkModel.random(4, 12, 2, new Random(23L));
		for (Node node : model.nodes) {
			node.worldRadius = 0.02;
		}
		NetworkDeclutterSimulation simulation = new NetworkDeclutterSimulation(model);
		SimulationContext context = context();
		simulation.init(context);

		for (int i = 0; i < 100 && simulation.step(context); i++) {
			// Exercise a representative portion of the relaxation.
		}

		assertTrue(simulation.getStep() > 0);
		assertFalse(simulation.getDiagnosticsSamples().isEmpty());
		for (Node node : model.nodes) {
			assertTrue(Double.isFinite(node.x));
			assertTrue(Double.isFinite(node.y));
			assertTrue(Double.isFinite(node.vx));
			assertTrue(Double.isFinite(node.vy));
			assertTrue(node.x >= node.worldRadius && node.x <= 1.0 - node.worldRadius);
			assertTrue(node.y >= node.worldRadius && node.y <= 1.0 - node.worldRadius);
		}

		NetworkDeclutterSimulation.Energy energy = simulation.computeEnergy();
		assertTrue(Double.isFinite(energy.spring));
		assertTrue(Double.isFinite(energy.repulsion));
		assertTrue(Double.isFinite(energy.center));
		assertTrue(Double.isFinite(energy.kinetic));
	}

	@Test
	void cancellationStopsBeforeAdvancing() throws Exception {
		NetworkDeclutterSimulation simulation = new NetworkDeclutterSimulation(
				NetworkModel.random(4, 6, 0, new Random(31L)));
		SimulationEngine engine = engineForContext();
		SimulationContext context = engine.getContext();
		simulation.init(context);
		engine.requestCancel();

		assertFalse(simulation.step(context));
		assertEquals(0, simulation.getStep());
	}

	@Test
	void simulationAlwaysTerminatesByItsHardStepLimit() throws Exception {
		NetworkDeclutterSimulation simulation = new NetworkDeclutterSimulation(
				NetworkModel.random(4, 6, 1, new Random(41L)));
		SimulationContext context = context();
		simulation.init(context);

		boolean keepRunning = true;
		while (keepRunning) {
			keepRunning = simulation.step(context);
			assertTrue(simulation.getStep() <= 2000,
					"simulation exceeded its documented hard limit");
		}

		assertTrue(simulation.getStep() >= 250,
				"simulation terminated before its documented minimum settling period");
	}

	private static SimulationContext context() {
		return engineForContext().getContext();
	}

	private static SimulationEngine engineForContext() {
		return new SimulationEngine(new Simulation() {
			@Override public void init(SimulationContext ctx) {}
			@Override public boolean step(SimulationContext ctx) { return false; }
		}, new SimulationEngineConfig(0, 0, 0, false));
	}
}
