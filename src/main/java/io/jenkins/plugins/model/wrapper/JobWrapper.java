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
package io.jenkins.plugins.model.wrapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.model.AbstractProject;
import hudson.model.BallColor;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.Jenkins;
import jenkins.triggers.ReverseBuildTrigger;

/**
 * Wrapper to generalize the use of AbstractProject and WorkflowJob
 * https://issues.jenkins-ci.org/browse/JENKINS-29913
 * @author OLSI
 */
public abstract class JobWrapper {
	
	protected Job job;

	public JobWrapper(Job job) {
		this.job = job;
	}

	public Job getJob() {
		return job;
	}

	public Set<JobWrapper> getPredecessor() {
		Map<TriggerDescriptor,Trigger<?>> jobTriggers = getTriggers();
		Set<JobWrapper> predecessors = new HashSet<>();
		for (Map.Entry<TriggerDescriptor, Trigger<?>> trigger : jobTriggers.entrySet()) {
			Trigger<?> jobTrigger = trigger.getValue();
			if (jobTrigger instanceof ReverseBuildTrigger) {
				ReverseBuildTrigger buildTrigger = (ReverseBuildTrigger)jobTrigger;
				String upProj = buildTrigger.getUpstreamProjects();
				predecessors.addAll(getJobListFromString(upProj));
			}
		}
		return predecessors;
	}
	
	@SuppressWarnings("squid:S1452")
	public Map<TriggerDescriptor,Trigger<?>> getTriggers() {
		throw new UnsupportedOperationException();
	}
	
	public String getFullName() {
		return job.getFullName();
	}
	
	public BallColor getIconColor() {
		return job.getIconColor();
	}
	
	public String getUrl() {
		return job.getUrl();
	}
	
	public String getAbsoluteUrl() {
		return job.getAbsoluteUrl();
	}
	
	public boolean isDisabled() {
		throw new UnsupportedOperationException();
	}
	
	public String getBuildStatusIconClassName() {
		return job.getBuildStatusIconClassName();
	}
	
	private Set<JobWrapper> getJobListFromString(String projects) {
		Set<JobWrapper> jobs = new HashSet<>();
		for (String jobName : projects.split(Pattern.quote(","))) {
			jobName = jobName.trim();
			if (!jobName.isEmpty()) {
				JobWrapper j = getJobByName(jobName);
				if (j != null) {
					jobs.add(j);
				}
			}
		}
		return jobs;
	}
	
	private JobWrapper getJobByName(String jobName) {
		Job j = Jenkins.get().getItemByFullName(jobName, Job.class);
		if (j != null && j.getFullName().equals(jobName)) {
			if (j instanceof WorkflowJob) {
				return new WorkflowJobWrapper((WorkflowJob)j);
			}
			else if (j instanceof AbstractProject) {
				return new ProjectWrapper((AbstractProject)j);
			}
		}
		return null;
	}
}
