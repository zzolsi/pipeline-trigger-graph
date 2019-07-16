package io.jenkins.plugins;

import java.util.Collection;
import java.util.Collections;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.Extension;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

@Extension
public class WorkflowJobTriggersActionFactory extends TransientActionFactory<WorkflowJob> {

	@Override
	public Class<WorkflowJob> type() {
		return WorkflowJob.class;
	}

	@Override
	public Collection<? extends Action> createFor(WorkflowJob target) {
		return Collections.singleton(new WorkflowJobTriggersAction(target));
	}

}
