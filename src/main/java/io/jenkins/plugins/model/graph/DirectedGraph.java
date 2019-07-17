/*
 * The MIT License
 *
 * Copyright (c) 2019, Bachmann electronics GmbH, Ole Siemers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.model.graph;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author OLSI
 * @param <V> the type of the vertices
 */
public class DirectedGraph<V> {
	
	private Map<V, Set<V>> forwardEdges;
	private Map<V, Set<V>> backwardEdges;
	
	public DirectedGraph() {
		reset();
	}
	
	/**
	 * Deletes all noded and edges from this graph
	 */
	protected void reset() {
		this.forwardEdges = new HashMap<>();
		this.backwardEdges = new HashMap<>();
	}

	/**
	 * Inserts a single node in this graph.
	 * @param vertex element to be inserted
	 */
	public void addVertex(V vertex) {
		forwardEdges.putIfAbsent(vertex, new HashSet<V>());
		backwardEdges.putIfAbsent(vertex, new HashSet<V>());
	}

	/**
	 * Inserts an edge to this graph.
	 * @param from vertex with outgoing edge
	 * @param to vertex with ingoing edge
	 */
	public void addEdge(V from, V to) {
		addVertex(from);
		addVertex(to);
		forwardEdges.get(from).add(to);
		backwardEdges.get(to).add(from);
	}
	
	/**
	 * Removes the spcified vertex from this graph if present.
	 * @param vertex vertex to be removed
	 */
	public void removeVertex(V vertex) {
		forwardEdges.forEach( (source, target) -> target.remove(vertex));
		forwardEdges.remove(vertex);
		backwardEdges.forEach( (source, target) -> target.remove(vertex));
		backwardEdges.remove(vertex);
	}

	/**
	 * Removes an edge from this graph.
	 * @param from the incident start vertex
	 * @param to the incident end vertex
	 */
	public void removeEdge(V from, V to) {
		if (!(forwardEdges.containsKey(from) && backwardEdges.containsKey(to))) {
			return;
		}
		forwardEdges.get(from).remove(to);
		backwardEdges.get(to).remove(from);
	}

	/**
	 * Returns the vertices of this graph.
	 * @return a set containing all vertices in this graph
	 */
	public Set<V> getVertices() {
		return forwardEdges.keySet();
	}
	
	/**
	 * Returns the predecessors of a given vertex
	 * @param vertex the end vertex
	 * @return the set of predecessors of the vertex
	 */
	public Set<V> getPredecessors(V vertex) {
		return backwardEdges.get(vertex);
	}
	
	/**
	 * Returns the successors of a given vertex
	 * @param vertex the start vertex
	 * @return the set successors of the vertex
	 */
	public Set<V> getSuccessors(V vertex) {
		return forwardEdges.get(vertex);
	}

	
	/**
	 * Returns all vertices which haven't an predecessor
	 * @return set of all roots in the graph
	 */
	public Set<V> getEntries() {
		return getEntriesOrExits(true);
	}
	
	/**
	 * Returns all vertices which haven't an predecessor
	 * @return set of all leafes in the graph
	 */
	public Set<V> getExits() {
		return getEntriesOrExits(false);
	}

	/**
	 * Lookup all vertices which haven't an predecessor or successor
	 * @param getEntry specifies if roots or leafs will be looked up
	 * @return set of all root or leaf nodes
	 */
	private Set<V> getEntriesOrExits(boolean getEntry) {
		Set<V> entriesOrExits = new HashSet<>();
		Map<V, Set<V>> graph = getEntry ? backwardEdges : forwardEdges;
		for (Map.Entry<V, Set<V>> edge : graph.entrySet()) {
			if (edge.getValue().isEmpty() && !entriesOrExits.contains(edge.getKey())) {
				entriesOrExits.add(edge.getKey());
			}
		}
		return entriesOrExits;
	}
	
	/**
	 * Returns true if this graph contains the specified vertex.
	 * @param vertex vertex whose presence in this graph is to be tested
	 * @return true if this graph contains the specified vertex
	 */
	public boolean containsVertex(V vertex) {
		return forwardEdges.containsKey(vertex);
	}
	
	/**
	 * Returns true if this graph contains the specified edge
	 * @param from the start vertex of edge whose presence in this graph is to be tested
	 * @param to the end vertex of the edge whose presence in this graph is to be tested
	 * @return true if this graph contains the specified edge
	 */
	public boolean containsEdge(V from, V to) {
		return forwardEdges.containsKey(from) && forwardEdges.get(from).contains(to);
	}

	/**
	 * Removes all vertices which are not connected to the given vertex
	 * @param connectedVertex the vertex which all other vertices are connected to
	 * @param removes all node
	 */
	public void removeDisconnectedComponents(V connectedVertex, boolean keepUnreachableSiblings) {
		Map<V, Boolean> discovered = discoverVertices(connectedVertex, keepUnreachableSiblings);
		for (Map.Entry<V, Boolean> vertexDiscovered : discovered.entrySet()){
			if (!vertexDiscovered.getValue()) {
				removeVertex(vertexDiscovered.getKey());
			}
		}
	}

	private Map<V, Boolean> discoverVertices(V connectedVertex, boolean keepUnreachableSiblings) {
		// perform breadth-first-search on backwardEdges and forwardEdges
		Queue<V> q = new LinkedList<>();
		Map<V, Boolean> discovered = new HashMap<>();
		for (V v : getVertices()) {
			discovered.put(v, false);
		}
		discovered.put(connectedVertex, true);
		if (!keepUnreachableSiblings) {
			for (V p : findParentsOfVertex(connectedVertex)) {
				discovered.put(p, true);
			}
		}
		q.add(connectedVertex);
		while (!q.isEmpty()) {
			V v = q.poll();
			for (V w : getPredecessors(v)) {
				if (!discovered.get(w)) {
					discovered.put(w, true);
					q.add(w);
				}
			}
			for (V w : getSuccessors(v)) {
				if (!discovered.get(w)) {
					discovered.put(w, true);
					q.add(w);
				}
			}
		}
		return discovered;
	}
	
	public Deque<V> getTopologicalOrder() {
		Deque<V> sorted = new LinkedList<>();
		Map<V, Boolean> visited = new HashMap<>();
		for (V v : getVertices()) {
			visited.put(v, false);
		}
		
		for (V v : getEntries()) {
			if (!visited.get(v)) {
				visitNode(v, visited, sorted);
			}
		}
		return sorted;
	}
	

	private void visitNode(V node, Map<V, Boolean> visited, Deque<V> sorted) {
		visited.put(node, true);
		for (V k : getSuccessors(node)) {
			if (!visited.get(k)) {
				visitNode(k, visited, sorted);
			}
		}
		sorted.push(node);
	}
	
	private Set<V> findParentsOfVertex(V vertex) {
		Set<V> parents = new HashSet<>();
		Queue<V> queue = new LinkedList<>();
		queue.add(vertex);
		while (!queue.isEmpty()) {
			V current = queue.poll();
			Set<V> predecessors = getPredecessors(current);
			if (!predecessors.isEmpty()) {
				parents.addAll(predecessors);
				queue.addAll(predecessors);
			}
		}
		return parents;
	}
}
