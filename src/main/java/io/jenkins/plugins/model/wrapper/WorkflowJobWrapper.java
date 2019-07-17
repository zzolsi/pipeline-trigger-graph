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

import java.util.Map;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.model.BallColor;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

public class WorkflowJobWrapper extends JobWrapper {

	private WorkflowJob workflowJob;

	public WorkflowJobWrapper(WorkflowJob job) {
		super(job);
		this.workflowJob = job;
	}

	@Override
	public WorkflowJob getJob() {
		return workflowJob;
	}

	@Override
	public Map<TriggerDescriptor, Trigger<?>> getTriggers() {
		return workflowJob.getTriggers();
	}

	@Override
	public String getFullName() {
		return workflowJob.getFullName();
	}

	@Override
	public BallColor getIconColor() {
		return workflowJob.getIconColor();
	}

	@Override
	public String getUrl() {
		return workflowJob.getUrl();
	}

	@Override
	public String getAbsoluteUrl() {
		return workflowJob.getAbsoluteUrl();
	}

	@Override
	public boolean isDisabled() {
		return workflowJob.isDisabled();
	}

	@Override
	public String getBuildStatusIconClassName() {
		return workflowJob.getBuildStatusIconClassName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((workflowJob == null) ? 0 : workflowJob.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkflowJobWrapper other = (WorkflowJobWrapper) obj;
		if (workflowJob == null) {
			if (other.workflowJob != null)
				return false;
		} else if (!workflowJob.equals(other.workflowJob)) {
			return false;
		}
		return true;
	}
}
