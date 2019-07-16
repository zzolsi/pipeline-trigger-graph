package io.jenkins.plugins;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.job.*;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.triggers.ReverseBuildTrigger;

public class WorkflowJobGraphTest {
	
	@Rule public JenkinsRule jenkins = new JenkinsRule();
	@ClassRule public static BuildWatcher bw = new BuildWatcher(); 
	
	private static final Logger logger = Logger.getLogger(WorkflowJobGraphTest.class.getName());

	@Test
	public void testUpstreamOfJob() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		assertEquals(Collections.singleton(job1), jobGraph.getUpstreamOfJob(job2));
	}
	
	@Test
	public void testDownstreamOfJob() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		assertEquals(Collections.singleton(job2), jobGraph.getDownstreamOfJob(job1));
	}
	
	@Test
	public void testEntriesOfGraph() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		assertEquals(Collections.singleton(job1), jobGraph.getEntries());
	}
	
	@Test
	public void testExitsOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		assertEquals(Collections.singleton(job2), jobGraph.getExits());
	}
	
	@Test
	public void testManualUpdateOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		jobGraph.update();
		assertEquals(Collections.singleton(job2), jobGraph.getUpstreamOfJob(job3));
	}
	
	/*
	@Test
	public void testAutomaticUpdateOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		WorkflowJobGraph jobGraph = WorkflowJobGraph.get();
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		assertEquals(Collections.singleton(job2), jobGraph.getUpstreamOfJob(job3));
	} */
}
