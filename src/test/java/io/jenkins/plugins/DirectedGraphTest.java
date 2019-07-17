package io.jenkins.plugins;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.jenkins.plugins.model.graph.DirectedGraph;

public class DirectedGraphTest {
	
	DirectedGraph<Integer> dg;
	
	@Before
	public void buildGraph() {
		dg = new DirectedGraph<>();
		dg.addEdge(1, 3);
		dg.addEdge(1, 4);
		dg.addEdge(2, 4);
		dg.addEdge(3, 6);
		dg.addEdge(4, 6);
		dg.addEdge(5, 6);
	}

	@Test
	public void testAddVertices() {
		Set<Integer> vertices = new HashSet<Integer>(Arrays.asList(1,2,3,4,5,6));
		assertEquals(vertices, dg.getVertices());
		assertTrue(dg.containsVertex(1));
		assertTrue(dg.containsVertex(2));
		assertTrue(dg.containsVertex(3));
		assertTrue(dg.containsVertex(4));
		assertTrue(dg.containsVertex(5));
		assertTrue(dg.containsVertex(6));
		assertTrue(dg.containsEdge(1, 3));
		assertTrue(dg.containsEdge(1, 4));
		assertTrue(dg.containsEdge(2, 4));
		assertTrue(dg.containsEdge(3, 6));
		assertTrue(dg.containsEdge(4, 6));
		assertTrue(dg.containsEdge(5, 6));
		assertFalse(dg.containsEdge(3, 3));
		assertFalse(dg.containsEdge(4, 1));
		assertFalse(dg.containsEdge(1, 9));
		assertFalse(dg.containsEdge(9, 1));
	}

	@Test
	public void testRemoveVertices() {
		Set<Integer> vertices = new HashSet<Integer>(Arrays.asList(3,4,5,6));
		dg.removeVertex(1);
		dg.removeVertex(2);
		assertEquals(vertices, dg.getVertices());
	}

	@Test
	public void testRemoveEdges() {
		Set<Integer> vertices = new HashSet<Integer>(Arrays.asList(1,2,3,4,5,6));
		assertEquals(2, dg.getSuccessors(1).size());
		assertEquals(1, dg.getPredecessors(3).size());
		assertEquals(1, dg.getSuccessors(5).size());
		assertEquals(3, dg.getPredecessors(6).size());
		
		dg.removeEdge(1, 3);
		dg.removeEdge(5, 6);
		dg.removeEdge(7, 8);
		assertEquals(vertices, dg.getVertices());
		assertEquals(1, dg.getSuccessors(1).size());
		assertEquals(0, dg.getPredecessors(3).size());
		assertEquals(0, dg.getSuccessors(5).size());
		assertEquals(2, dg.getPredecessors(6).size());
	}
	
	@Test
	public void testGetPredecessors() {
		assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), dg.getPredecessors(6));
		assertEquals(new HashSet<>(), 						dg.getPredecessors(5));
		assertEquals(new HashSet<>(Arrays.asList(1, 2)), 	dg.getPredecessors(4));
		assertEquals(new HashSet<>(Arrays.asList(1)), 		dg.getPredecessors(3));
		assertEquals(new HashSet<>(), 						dg.getPredecessors(2));
		assertEquals(new HashSet<>(),			 			dg.getPredecessors(1));
	}
	
	@Test
	public void testGetSuccessors() {
		assertEquals(new HashSet<>(Arrays.asList(3, 4)), 	dg.getSuccessors(1));
		assertEquals(new HashSet<>(Arrays.asList(4)),		dg.getSuccessors(2));
		assertEquals(new HashSet<>(Arrays.asList(6)),	 	dg.getSuccessors(3));
		assertEquals(new HashSet<>(Arrays.asList(6)),		dg.getSuccessors(4));
		assertEquals(new HashSet<>(Arrays.asList(6)), 		dg.getSuccessors(5));
		assertEquals(new HashSet<>(),			 			dg.getSuccessors(6));
	}
	
	@Test
	public void testRemoveDisconnectedComponents() {
		dg.removeEdge(5, 6);
		dg.removeEdge(1, 4);
		dg.removeEdge(3, 6);
		dg.removeDisconnectedComponents(4, true);
		assertEquals(Collections.singleton(4), dg.getSuccessors(2));
		assertEquals(Collections.singleton(6), dg.getSuccessors(4));
		assertEquals(Collections.emptySet(),   dg.getSuccessors(6));
		assertEquals(Collections.emptySet(),   dg.getPredecessors(2));
		assertEquals(Collections.singleton(2), dg.getPredecessors(4));
		assertEquals(Collections.singleton(4), dg.getPredecessors(6));
	}
	
	@Test
	public void testRemoveDisconnectedComponentsWithoutUnreachableSiblings() {
		dg.removeEdge(3, 6);
		dg.removeDisconnectedComponents(4, false);
		assertEquals(new HashSet<>(Arrays.asList(1, 2, 4, 5, 6)), dg.getVertices());
	}
	
	@Test
	@Ignore("Not ready yet")
	public void testTopologicalOrder() {
		fail("not implemented yet");
	}
}
