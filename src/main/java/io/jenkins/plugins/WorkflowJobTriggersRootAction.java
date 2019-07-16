package io.jenkins.plugins;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.RootAction;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;

@Extension
public class WorkflowJobTriggersRootAction implements RootAction {
	
	private WorkflowJobGraph workflowGraph;
	private Jenkins jenkins;

	private static final Logger logger = Logger.getLogger(WorkflowJobTriggersAction.class.getName());

	public WorkflowJobTriggersRootAction() {
		this.jenkins = Jenkins.get();
		this.workflowGraph = WorkflowJobGraph.get();
	}
	
	public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException, ServletException, InterruptedException {
		String path = req.getRestOfPath();
		if (path.startsWith("/graph.")) {
			String extension = path.substring(path.lastIndexOf(".")+1);
			String dot = workflowGraph.getDotString(null);
			VizGraph.runDot(res.getCompressedOutputStream(req), new ByteArrayInputStream(dot.getBytes()), extension);	
			return;
		} else {
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	public WorkflowJobGraph getWorkflowGraph() {
		return workflowGraph;
	}

	public Jenkins getJenkins() {
		return jenkins;
	}

	@Override
	public String getIconFileName() {
		return "clipboard.png";
	}

	@Override
	public String getDisplayName() {
		return "Job Trigger Graph";
	}

	@Override
	public String getUrlName() {
		return "job_triggers";
	}

}
