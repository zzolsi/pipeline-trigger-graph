package io.jenkins.plugins;

import java.util.Collection;
import java.util.Collections;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;

@Extension
public class RunTriggersActionFactory extends TransientActionFactory<Run> {

	@Override
	public Class<Run> type() {
		return Run.class;
	}

	@Override
	public Collection<? extends Action> createFor(Run target) {
        return Collections.singleton(new RunTriggersAction(target)); 
	}
}
