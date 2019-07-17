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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.Cause.UpstreamCause;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import io.jenkins.plugins.model.graph.DirectedGraph;
import jenkins.model.Jenkins;

public class RunTriggerGraph extends DirectedGraph<Run> {

	private Run run;
	
	public RunTriggerGraph(Run run) {
		super();
		this.run = run;
		this.addUpstreamRunsToGraph(this.run);
	}

	/**
	 * @return the last upstream cause of this run or null if it was not triggered by another run.
	 */
	public Cause getCause() {
		Run<?, ?> lastBuild = run;
		if (lastBuild != null) {
			return lastBuild.getCause(UpstreamCause.class);
		}
		return null;
	}
	
	/**
	 * calculates the dot-file-string of a given graph
	 * @param graph with adjacent-list
	 * @return string of the dot-file
	 */
	public String getDot() {
		StringBuilder dot = new StringBuilder();
		dot.append(String.format("digraph \"%s\" {%n\tnode [shape=box, style=rounded, fontname=sans];%n\tgraph [rankdir=BT]; %n", run.getFullDisplayName()));
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		for (Run r : getVertices()) {
			String nodeIcon = settings.isDrawBalls() ? String.format("<td><img src=\"%s\" /></td>", r.getIconColor().getImage()) : "";
			String nodeLabel = String.format("<table border=\"0\"><tr>%s<td>%s</td></tr></table>", nodeIcon, r.getFullDisplayName());
			dot.append(String.format("\t\"%s\" [label=<%s>];%n", r.getFullDisplayName(), nodeLabel));
		}
		for (Run sourceRun : getVertices()) {
			Set<Run> targetRuns = getSuccessors(sourceRun);
			for (Run targetRun : targetRuns) {
				dot.append("\t\""+targetRun.getFullDisplayName() + "\" -> \""+sourceRun.getFullDisplayName()+"\";\n");
			}
		}
		dot.append("}");
		return dot.toString();
	}
	
	/**
	 * Gets the upstream causes of the current run
	 * @return a list of all upstream-causes
	 */
	public List<UpstreamCause> getUpstreamCauses() {
		LinkedList<UpstreamCause> causes = new LinkedList<>();
		Run<?, ?> lastBuild = this.run;
		if (lastBuild != null) {
			UpstreamCause lastCause = lastBuild.getCause(UpstreamCause.class);
			while (lastCause != null) {
				causes.add(lastCause);
				Run<?, ?> upstreamRun = lastCause.getUpstreamRun();
				if (upstreamRun != null) {
					lastCause = upstreamRun.getCause(UpstreamCause.class);
				} else {
					lastCause = null;
				}
			} 
		}
		return causes;
	}
	
	/**
	 * Adds all upstream runs which have caused a given run to a given graph
	 * @param run to calculate upstream runs
	 * @param graph graph to save adjacent nodes
	 */
	private void addUpstreamRunsToGraph(Run<?, ?> run) {
		if (run != null) {
			
			// retrieve causes of given run
			List<Cause> causes = run.getCauses();
			for (Cause cause : causes) {
				
				// check for cause-type and continue recursively if type of UpstreamCause
				if (cause instanceof UpstreamCause) {
					UpstreamCause upstreamCause = (UpstreamCause)cause;
					Run upstreamRun = upstreamCause.getUpstreamRun();
					addUpstreamRunsToGraph(upstreamRun);
					addEdge(run, upstreamRun);
				}
			}
		}
	}
}
