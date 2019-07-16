package io.jenkins.plugins;

import hudson.model.Action;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class WorkflowJobTriggersAction implements Action {

	private WorkflowJob job;
	private WorkflowJobGraph workflowGraph;
	
	private static final Logger logger = Logger.getLogger(WorkflowJobTriggersAction.class.getName());
	
	public WorkflowJobTriggersAction(WorkflowJob target) {
		this.job = target;
		this.workflowGraph = WorkflowJobGraph.get();
	}
	
	public String getDot() {
		return workflowGraph.getDotString(job);
	}
	
	public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException, ServletException, InterruptedException {
		String path = req.getRestOfPath();
		logger.info(path);
		if (path.startsWith("/graph.")) {
			String extension = path.substring(path.lastIndexOf(".")+1);
			String dot = workflowGraph.getDotString(job);
			VizGraph.runDot(res.getCompressedOutputStream(req), new ByteArrayInputStream(dot.getBytes()), extension);	
			return;
		} else {
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	public Map<WorkflowJob, Set<WorkflowJob>> getUpstreamGraph() {
		return workflowGraph.getUpstream();
	}

	public Set<WorkflowJob> getUpstreamJobs() {
		return workflowGraph.getUpstreamOfJob(job);
	}

	public Set<WorkflowJob> getDownstreamJobs() {
		return workflowGraph.getDownstreamOfJob(job);
	}

	@Override
	public String getIconFileName() {
		return "clipboard.png";
	}

	@Override
	public String getDisplayName() {
		return "Triggers Graph";
	}

	@Override
	public String getUrlName() {
		return "triggers";
	}

	public WorkflowJob getJob() {
		return job;
	}
}
