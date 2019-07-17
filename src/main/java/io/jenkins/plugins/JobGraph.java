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
package io.jenkins.plugins;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.model.AbstractProject;
import hudson.model.Item;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import io.jenkins.plugins.model.graph.DirectedGraph;
import io.jenkins.plugins.model.wrapper.ProjectWrapper;
import io.jenkins.plugins.model.wrapper.JobWrapper;
import io.jenkins.plugins.model.wrapper.WorkflowJobWrapper;
import jenkins.model.Jenkins;

public class JobGraph extends DirectedGraph<JobWrapper> {

	private Map<JobWrapper, Integer> totalTriggerCount;
	private DescriptorImpl settings;
	
	private static final String DISABLED_NODE_COLOR = "gray";
	private static final String NODE_COLOR = "black";
	private static final Logger logger = Logger.getLogger(JobGraph.class.getName());

	public JobGraph() {
		super();
		update();
	}

	/**
	 * retrieve the upstream dependencies of all jobs and save them into a graph
	 */
	public void update() {
		settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		reset();
		for (Item item : Jenkins.get().getAllItems()) {
			if (item instanceof WorkflowJob) {
				WorkflowJobWrapper workflowJob = new WorkflowJobWrapper((WorkflowJob)item);
				loadUpstreamOfJob(workflowJob);
			} 
			else if (item instanceof AbstractProject) {
				ProjectWrapper proj = new ProjectWrapper((AbstractProject)item);
				loadUpstreamOfJob(proj);
			}
			else {
				logger.log(Level.FINE, "ignoring item "+item.getFullName()+" is an instance of "+item.getClass().getName());
			}
		}
		if (settings.isCountTriggersTransitively()) {
			countTotalTriggerPaths();
		}
	}
	
	/**
	 * Generates a representations of the graph as GraphViz dot
	 * @param current a selected note which should be highlighted and all non-connected node removed
	 * @return the dot-string representation
	 */
	public String getDotString(JobWrapper current) {
        String rankdir = settings.isLeftToRightLayout() ? "LR" : "TB";
        StringBuilder dot = new StringBuilder();
		dot.append(String.format("digraph { %n\tnode [shape=box, style=rounded, fontname=sans ];%n\tgraph [rankdir=%s]; %n", rankdir));
		for (JobWrapper j : getVertices()) {
			if (isJobVisible(j)) {
				dot.append(getDotNode(j, current));
			}
		}
		for (JobWrapper targetJob : getVertices()) {
			Set<JobWrapper> sourceJobs = getPredecessors(targetJob);
			for (JobWrapper sourceJob : sourceJobs) {
				if (isJobVisible(sourceJob) && isJobVisible(targetJob)) {
					dot.append(getDotEdge(sourceJob, targetJob, current));
				}
			}
		}
		dot.append("}");
		return dot.toString();
	}
	
	/**
	 * Removes all node which are not connected to the given node
	 * @param node the node which all other nodes will be connected to
	 */
	public void removeUnconnectedNodes(JobWrapper node) {
		removeDisconnectedComponents(node, !settings.isLinearUpstreamOfProject());
	}
	
	public Set<JobWrapper> getJobs() {
		return getVertices();
	}

	/**
	 * Get the predecessor of a specific job
	 * @param job which gets triggered by other jobs
	 * @return the jobs which trigger the given job
	 */
	public Set<JobWrapper> getUpstreamOfJob(JobWrapper job) {
		return getPredecessors(job);
	}

	/**
	 * Get the successor of a specific job
	 * @param job which triggers by other jobs
	 * @return the jobs which are triggered the given job
	 */
	public Set<JobWrapper> getDownstreamOfJob(JobWrapper job) {
		return getSuccessors(job);
	}

	/**
	 * @return the a map of the number of triggers of all jobs
	 */
	public Map<JobWrapper, Integer> getTotalTriggerCount() {
		return totalTriggerCount;
	}
	
	/**
	 * calculate the upstream dependency graph of a given job
	 * @param job to get its upstream dependencies of
	 */
	private void loadUpstreamOfJob(JobWrapper job) {
		if (!containsVertex(job)) {
			Queue<JobWrapper> queue = new LinkedList<>();
			Map<JobWrapper, Boolean> discovered = new HashMap<>();
			queue.add(job);
			while (!queue.isEmpty()) {
				JobWrapper j = queue.poll();
				addVertex(j);
				discovered.put(j, true);
				Set<JobWrapper> predecessors = j.getPredecessor();
				for (JobWrapper predecessor : predecessors) {
					addEdge(predecessor, j);
					if (!discovered.getOrDefault(predecessor, false)) {
						queue.add(predecessor);
					}
				}
			}
		}
	}
	
	/**
	 * Get the string which represents the node in the dot-file
	 * @param node job to draw in the graph
	 * @param current selected job
	 * @return the dot code for a given job
	 */
	private String getDotNode(JobWrapper node, JobWrapper current) {
		String nodeStyle = node.equals(current) ?  "rounded,filled" : "rounded"; // <td><img src=\""+node.getIconColor().getImage()+"\" /></td>
		String nodeImage = settings.isDrawBalls() ? String.format("<td><img src=\"%s%s\" /></td>", settings.getImagePath(), node.getIconColor().getImage()) : "";
		String nodeTriggerCount = settings.isCountTriggersTransitively() ? String.format("<td>(%d)</td>", totalTriggerCount.get(node)) : "";
		String nodeLabel = String.format("<table border=\"0\"><tr>%s<td>%s</td>%s</tr></table>", nodeImage, node.getFullName(), nodeTriggerCount);
		String nodeHref = node.getAbsoluteUrl()+"triggers/";
		String nodeColor = node.isDisabled()  || node.equals(current) ? DISABLED_NODE_COLOR : NODE_COLOR;
		String nodeFontcolor = node.isDisabled() && !node.equals(current) ? DISABLED_NODE_COLOR : NODE_COLOR;
		return String.format("\t\"%s\" [style=\"%s\", label=<%s>, href=\"%s\", color=\"%s\", fontcolor=\"%s\"]; %n", node.getFullName(), nodeStyle, nodeLabel, nodeHref, nodeColor, nodeFontcolor);
	}
	
	/**
	 * Get the string which represents an directed edge (a->b) in the dot-file
	 * @param source node a
	 * @param target node b
	 * @param current a node whose adjacent edges are highlighted
	 * @return the dot code for an edge between two jobs
	 */
	private String getDotEdge(JobWrapper source, JobWrapper target, JobWrapper current) {
		String edgeColor = (source.isDisabled() || target.isDisabled()) ? DISABLED_NODE_COLOR : NODE_COLOR;
		String edgeWidth = source.equals(current) || target.equals(current) ? String.valueOf(settings.getSelectedEdgeWidth()) : "1";
		String edgeProperties = String.format("[color=\"%s\", penwidth=\"%s\"]", edgeColor, edgeWidth);
		return String.format("\t\"%s\" -> \"%s\" %s; %n", source.getFullName(), target.getFullName(), edgeProperties);
	}
	
	
	/**
	 * Get if a job is visible because its not disabled or disabled jobs are shown because of user settings
	 * @param job job to get visibility state
	 * @return if a job is visible
	 */
	private boolean isJobVisible(JobWrapper j) {
		return !j.isDisabled() || (!settings.isHideDisabled() && j.isDisabled());
	}
	
	/**
	 * Calculates how often a job gets triggered transitively over all jobs by counting the number of paths to each node
	 */
	private void countTotalTriggerPaths() {
		totalTriggerCount = new HashMap<>();
		Deque<JobWrapper> sorted = getTopologicalOrder();
		for (JobWrapper j : getJobs()) {
			totalTriggerCount.put(j, 1);
		}
		while (!sorted.isEmpty()) {
			JobWrapper j = sorted.pop();
			Integer pathCount = 0;
			Set<JobWrapper> upstreamOfJ = getUpstreamOfJob(j);
			if (upstreamOfJ.isEmpty()) {
				pathCount = 1;
			}
			for (JobWrapper k : upstreamOfJ) {
				pathCount += totalTriggerCount.get(k);
			}
			totalTriggerCount.put(j, pathCount);
		}
	}
}
