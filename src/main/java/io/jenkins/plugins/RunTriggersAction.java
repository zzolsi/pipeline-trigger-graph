package io.jenkins.plugins;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.DependencyGraph;
import hudson.model.Run;
import hudson.model.Run;
import hudson.model.Cause.UpstreamCause;
import hudson.triggers.Trigger;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import jenkins.model.Jenkins;
import jenkins.triggers.ReverseBuildTrigger;

/**
 * TODO: refactor this class, remove unused methods
 */
public class RunTriggersAction implements Action {
	
	private static final Logger logger = Logger.getLogger(RunTriggersAction.class.getName());

	private Run run;
	private List<UpstreamCause> upstreamCauses;
	
	public RunTriggersAction(Run run) {
		this.run = run;
	}
	
	/**
	 * @return the last upstream cause of this run or null if it was not triggered by another run.
	 */
	public Cause getCause() {
		Run lastBuild = run;
		logger.info("lastBuild: "+lastBuild);
		if (lastBuild != null) {
			UpstreamCause cause = (UpstreamCause)lastBuild.getCause(UpstreamCause.class);
			logger.info("cause: "+cause);
			return cause;
		}
		return null;
	}
	
	/**
	 * @return all runs known by jenkins.
	 * 
	public List<Run> getAllRuns() {
		Jenkins jenkins = Jenkins.get();
		List<Run> runs = jenkins.getAllItems(Run.class);
		return runs;
	}
	 */
	

	
	public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException, ServletException, InterruptedException {
		String path = req.getRestOfPath();
		logger.info(path);
		if (path.startsWith("/graph.")) {
			String extension = path.substring(path.lastIndexOf(".")+1);
			String dot = getDotFromUpstream();
			VizGraph.runDot(res.getCompressedOutputStream(req), new ByteArrayInputStream(dot.getBytes()), extension);	
			return;
		} else {
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	/**
	 * @return a String of a Dotfile-graph of the last builds of all runs
	 */
	public String getTriggerDot() {
		// List<Run> runs = this.getAllRuns();
		Map<Run, List<Run>> runGraph = getUpstreamRunGraph(this.run);
		// for (Run j : runs) {
		// 	addUpstreamRunsToGraph(j, runGraph);
		// }
		// addUpstreamRunsToGraph(run, runGraph);
		return getDotFromGraph(runGraph);
	}
	
	/**
	 * @return a String of a dotfile-graph of all upstream run which causes this run transitive
	 */
	public String getDotFromUpstream() {
		Map<Run, List<Run>> runGraph = getUpstreamRunGraph(this.run);
		return getDotFromGraph(runGraph);
	}
	
	/**
	 * calculates the dot-file-string of a given graph
	 * @param graph with adjacent-list
	 * @return string of the dot-file
	 */
	public String getDotFromGraph(Map<Run, List<Run>> graph) {
		String dot = String.format("digraph \"%s\" {\n\tnode [shape=box, style=rounded, fontname=sans];\n", run.getFullDisplayName());
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		for (Run r : graph.keySet()) {
			String nodeLabel = settings.isDrawBalls() ? String.format("<table border=\"0\"><tr><td><img src=\"%s\" /></td><td>%s</td></tr></table>",r.getIconColor().getImage(), r.getFullDisplayName()) : "";
			dot += String.format("\t\"%s\" [label=<%s>];\n", r.getFullDisplayName(), nodeLabel);
		}
		for (Run sourceRun : graph.keySet()) {
			List<Run> targetRuns = graph.get(sourceRun);
			for (Run targetRun : targetRuns) {
				dot += "\t\""+targetRun.getFullDisplayName() + "\" -> \""+sourceRun.getFullDisplayName()+"\";\n";
			}
		}
		return dot+"}";
	}
	
	/**
	 * Gets the upstream causes of the current run
	 * @return a list of all upstream-causes
	 */
	public List<UpstreamCause> getUpstreamCauses() {
		LinkedList<UpstreamCause> causes = new LinkedList<UpstreamCause>();
		Run lastBuild = this.run;
		if (lastBuild != null) {
			UpstreamCause lastCause = (UpstreamCause)lastBuild.getCause(UpstreamCause.class);
			while (lastCause != null) {
				logger.info("lastCause = "+lastCause);
				causes.add(lastCause);
				lastCause = (UpstreamCause)lastCause.getUpstreamRun().getCause(UpstreamCause.class);
			} 
		}
		logger.info("causes: "+causes);
		return causes;
	}
	
	/**
	 * Calculates the graph of all runs which have triggered a given run transitively
	 * @param j the Run which has been triggered
	 * @return Graph of the causing runs
	 */
	public Map<Run, List<Run>> getUpstreamRunGraph(Run j) {
		System.out.println("building run graph");
		Map<Run, List<Run>> runGraph = new HashMap<>();
		Run currentBuild = this.run;
		this.addUpstreamRunsToGraph(currentBuild, runGraph);
		logger.info("rungraph = "+runGraph);
		return runGraph;
	}
	
	/**
	 * Adds all upstream runs which have caused a given run to a given graph
	 * @param run to calculate upstream runs
	 * @param graph graph to save adjacent nodes
	 * @return
	 */
	private Map<Run, List<Run>> addUpstreamRunsToGraph(Run run, Map<Run, List<Run>> graph) {
		if (run != null) {
			
			// retrieve causes of given run
			List<Cause> causes = run.getCauses();
			List<Run> upstreamRuns = new LinkedList<Run>();
			for (Cause cause : causes) {
				
				// check for cause-type and continue recursively if type of UpstreamCause
				if (cause instanceof UpstreamCause) {
					UpstreamCause upstreamCause = (UpstreamCause)cause;
					Run upstreamRun = upstreamCause.getUpstreamRun();
					addUpstreamRunsToGraph(upstreamRun, graph);
					upstreamRuns.add(upstreamRun);
				}
			}
			graph.put(run, upstreamRuns);
		}
		return graph;
	}
	
	/**
	 * Calculates the graph of all runs which have triggered a given run transitively
	 * @param j the Run which has been triggered
	 * @return Graph of the causing runs
	 */
	/*public Map<Run, List<Run>> getUpstreamRunGraph(Run j) {
		System.out.println("building run graph");
		Map<Run, List<Run>> runGraph = new HashMap<>();
		this.addUpstreamRunsToGraph(j, runGraph);
		logger.info("rungraph = "+runGraph);
		return runGraph;
	}*/
	
	/**
	 * Add all upstream runs, which can trigger a given run transitively, to a given graph
	 * @param run to calculate upstream runs
	 * @param graph graph to save adjacent nodes
	 * @return
	 */
	/*private Map<Run, List<Run>> addUpstreamRunsToGraph(Run run, Map<Run, List<Run>> graph) {
		if (run != null) {
			
			// retrieve triggers of given run
			List<RunProperty> runProperties = run.getAllProperties()
			for (RunProperty runProperty : runProperties) {
				logger.info("run "+run+" hasProperty "+runProperty);
				runProperty.
			}
			List<Run> upstreamRuns = new LinkedList<Run>();
			for (Cause cause : causes) {
				
				// check for cause-type and continue recursively if type of UpstreamCause
				if (cause instanceof UpstreamCause) {
					UpstreamCause upstreamCause = (UpstreamCause)cause;
					Run upstreamRun = upstreamCause.getUpstreamRun();
					addUpstreamRunsToGraph(upstreamRun, graph);
					upstreamRuns.add(upstreamRun);
				}
			}
			graph.put(run, upstreamRuns);
		}
		return graph;
	}*/

	public Run getRun() {
		return run;
	}

	public void setRun(Run run) {
		this.run = run;
	}

	@Override
	public String getIconFileName() {
        return "clipboard.png";
	}

	@Override
	public String getDisplayName() {
        return "Run Triggers";
	}

	@Override
	public String getUrlName() {
        return "triggers";
	}
}
