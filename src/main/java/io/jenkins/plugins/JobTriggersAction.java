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

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import io.jenkins.plugins.model.wrapper.ProjectWrapper;
import io.jenkins.plugins.model.wrapper.JobWrapper;
import io.jenkins.plugins.model.wrapper.WorkflowJobWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class JobTriggersAction implements Action {

	private JobWrapper jobWrapper;
	private JobGraph jobGraph;
	
	public JobTriggersAction(Job target) {
		this.jobWrapper = target instanceof WorkflowJob ? 
				new WorkflowJobWrapper((WorkflowJob)target) : 
				new ProjectWrapper((AbstractProject)target);
		this.jobGraph = new JobGraph();
		this.jobGraph.removeUnconnectedNodes(jobWrapper);
	}
	
	public String getDot() {
		return jobGraph.getDotString(jobWrapper);
	}
	
	public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException, InterruptedException {
		String path = req.getRestOfPath();
		if (path.startsWith("/graph.")) {
			String extension = path.substring(path.lastIndexOf('.')+1);
			String dot = jobGraph.getDotString(jobWrapper);
			GraphViz.runDot(res.getCompressedOutputStream(req), new ByteArrayInputStream(dot.getBytes(StandardCharsets.UTF_8)), extension);	
		} else {
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	public Set<JobWrapper> getUpstreamJobs() {
		return jobGraph.getUpstreamOfJob(jobWrapper);
	}

	public Set<JobWrapper> getDownstreamJobs() {
		return jobGraph.getDownstreamOfJob(jobWrapper);
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

	public Job getJob() {
		return jobWrapper.getJob();
	}
}
