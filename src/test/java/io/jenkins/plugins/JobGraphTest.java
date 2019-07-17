package io.jenkins.plugins;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.workflow.job.*;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import io.jenkins.plugins.model.wrapper.ProjectWrapper;
import io.jenkins.plugins.model.wrapper.JobWrapper;
import io.jenkins.plugins.model.wrapper.WorkflowJobWrapper;
import jenkins.model.Jenkins;
import jenkins.triggers.ReverseBuildTrigger;

public class JobGraphTest {
	
	@Rule public JenkinsRule jenkins = new JenkinsRule();
	@ClassRule public static BuildWatcher bw = new BuildWatcher(); 
	
	private static final Logger logger = Logger.getLogger(JobGraphTest.class.getName());

	@Test
	public void testGraphIsContainsAllJobs() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		JobGraph jobGraph = new JobGraph();
		assertEquals(1, jobGraph.getJobs().size());

		jenkins.createProject(WorkflowJob.class, "2");
		jenkins.createProject(WorkflowJob.class, "3");
		jenkins.createProject(FreeStyleProject.class, "4");
		jobGraph.update();
		assertEquals(4, jobGraph.getJobs().size());
	}

	@Test
	public void testUpstreamOfWorkflowJob() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(2, jobGraph.getJobs().size());
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job1)), jobGraph.getUpstreamOfJob(new WorkflowJobWrapper(job2)));
	}
	
	@Test
	public void testDownstreamOfWorkflowJob() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(2, jobGraph.getJobs().size());
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job2)), jobGraph.getDownstreamOfJob(new WorkflowJobWrapper(job1)));
	}
	
	@Test
	public void testEntriesOfGraph() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job1)), jobGraph.getEntries());
	}
	
	@Test
	public void testEntriesWithCircle() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job1.addTrigger(new ReverseBuildTrigger("3"));
		job2.addTrigger(new ReverseBuildTrigger("1"));
		job3.addTrigger(new ReverseBuildTrigger("2"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(Collections.emptySet(), jobGraph.getEntries());
	}
	
	@Test
	public void testExitsOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job2)), jobGraph.getExits());
	}
	
	@Test
	public void testExitsWithCircle() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job1.addTrigger(new ReverseBuildTrigger("2"));
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(Collections.emptySet(), jobGraph.getExits());
	}
	
	@Test
	public void testManualUpdateOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(2, jobGraph.getJobs().size());
		assertEquals(1, jobGraph.getEntries().size());
		assertEquals(1, jobGraph.getExits().size());
		
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		job2.setTriggers(Collections.emptyList());
		jobGraph.update();
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job2)), jobGraph.getUpstreamOfJob(new WorkflowJobWrapper(job3)));
		assertEquals(Collections.emptySet(), jobGraph.getUpstreamOfJob(new WorkflowJobWrapper(job2)));
		assertEquals(2, jobGraph.getEntries().size());
		assertEquals(2, jobGraph.getExits().size());
	}
	
	@Test
	public void testRemoveNonConnectedCompontent() throws IOException {

		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		jobGraph.removeUnconnectedNodes(new WorkflowJobWrapper(job3));
		assertEquals(1, jobGraph.getEntries().size());	
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job3)), jobGraph.getJobs());
		
		job3.addTrigger(new ReverseBuildTrigger("1"));
		jobGraph.update();
		assertEquals(1, jobGraph.getEntries().size());
		assertEquals(2, jobGraph.getExits().size());
		
		logger.info("entries before remove: "+jobGraph.getEntries());
		jobGraph.removeUnconnectedNodes(new WorkflowJobWrapper(job3));
		assertEquals(3, jobGraph.getJobs().size());	
		logger.info("entries after remove: "+jobGraph.getEntries());
	}
	
	@Test
	public void testTotalTriggerCount() throws IOException {
		/*
		 *       1  2   \/ downstream
		 *      /| /    
		 *     / |/
		 *    3  4  5
		 *     \ | /
		 *      \|/
		 *       6
		 */
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		WorkflowJob job4 = jenkins.createProject(WorkflowJob.class, "4");
		WorkflowJob job5 = jenkins.createProject(WorkflowJob.class, "5");
		WorkflowJob job6 = jenkins.createProject(WorkflowJob.class, "6");
		job3.addTrigger(new ReverseBuildTrigger("1"));
		job4.addTrigger(new ReverseBuildTrigger("1, 2"));
		job6.addTrigger(new ReverseBuildTrigger("3, 4, 5"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job4)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job5)).intValue());
		assertEquals(4, triggerCounts.get(new WorkflowJobWrapper(job6)).intValue());
	}
	
	@Test
	public void testTotalTriggerCount2() throws IOException {
		/*
		 *         2 
		 *       / |   
		 *      3  4
		 *      |  |
		 *      5  6  1
		 *       \ | /
		 *         7
		 */
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		WorkflowJob job4 = jenkins.createProject(WorkflowJob.class, "4");
		WorkflowJob job5 = jenkins.createProject(WorkflowJob.class, "5");
		WorkflowJob job6 = jenkins.createProject(WorkflowJob.class, "6");
		WorkflowJob job7 = jenkins.createProject(WorkflowJob.class, "7");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		job4.addTrigger(new ReverseBuildTrigger("2"));
		job5.addTrigger(new ReverseBuildTrigger("3"));
		job6.addTrigger(new ReverseBuildTrigger("4"));
		job7.addTrigger(new ReverseBuildTrigger("1, 5, 6"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job4)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job5)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job6)).intValue());
		assertEquals(3, triggerCounts.get(new WorkflowJobWrapper(job7)).intValue());
	}
	
	@Test 
	public void testTotalTriggerCount3() throws IOException {
		/* 
		 *   2
		 *   |
		 *   3  1
		 *   | /
		 *   4
		 */
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		WorkflowJob job4 = jenkins.createProject(WorkflowJob.class, "4");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		job4.addTrigger(new ReverseBuildTrigger("3, 1"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job4)).intValue());
	}
	
	@Test 
	public void testTotalTriggerCount4() throws IOException {
		/* 
		 *  3   2
		 *   \ /
		 *    4  1
		 *    | /
		 *    5
		 */

		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		WorkflowJob job4 = jenkins.createProject(WorkflowJob.class, "4");
		WorkflowJob job5 = jenkins.createProject(WorkflowJob.class, "5");
		job4.addTrigger(new ReverseBuildTrigger("2, 3"));
		job5.addTrigger(new ReverseBuildTrigger("4, 5"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job4)).intValue());
		assertEquals(3, triggerCounts.get(new WorkflowJobWrapper(job5)).intValue());
	}
	
	@Test
	public void testTriggerCountWithoutEntries() throws IOException {
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job1.addTrigger(new ReverseBuildTrigger("3"));
		job2.addTrigger(new ReverseBuildTrigger("1"));
		job3.addTrigger(new ReverseBuildTrigger("2"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
	}
	
	@Test
	public void testTriggerCountWithCircle() throws IOException {
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setCountTriggersTransitively(true);
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		WorkflowJob job4 = jenkins.createProject(WorkflowJob.class, "4");
		job2.addTrigger(new ReverseBuildTrigger("1, 4"));
		job3.addTrigger(new ReverseBuildTrigger("2"));
		job4.addTrigger(new ReverseBuildTrigger("3"));
		JobGraph graph = new JobGraph();
		Map<JobWrapper, Integer> triggerCounts = graph.getTotalTriggerCount();
		assertEquals(1, triggerCounts.get(new WorkflowJobWrapper(job1)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job2)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job3)).intValue());
		assertEquals(2, triggerCounts.get(new WorkflowJobWrapper(job4)).intValue());
	}
	
	@Test
	public void testDotStringContainsAllProjects() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph graph = new JobGraph();
		String dot = graph.getDotString(null);
		assertNotNull(dot);
		assertThat(dot, CoreMatchers.containsString("\"1\""));
		assertThat(dot, CoreMatchers.containsString("\"2\""));
	}

	
	@Test
	public void testIgnoreDisabledJobInDotString() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job1.setDisabled(true);
		DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		settings.setHideDisabled(false);
		JobGraph graph = new JobGraph();
		String dot = graph.getDotString(null);
		assertEquals(2, graph.getJobs().size());
		assertThat(dot, CoreMatchers.containsString("\"1\""));
		assertThat(dot, CoreMatchers.containsString("\"2\""));

		settings.setHideDisabled(true);
		graph.update();
		dot = graph.getDotString(null);
		assertThat(dot, CoreMatchers.containsString("\"2\""));
		assertFalse(dot.contains("\"1\""));
	}

	@Test
	public void testWorkflowUpstreamOfFreestyleProject() throws IOException {
		WorkflowJob job1 = jenkins.createProject(WorkflowJob.class, "1");
		FreeStyleProject job2 = jenkins.createProject(FreeStyleProject.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(2, jobGraph.getJobs().size());
		assertEquals(Collections.singleton(new WorkflowJobWrapper(job1)), jobGraph.getUpstreamOfJob(new ProjectWrapper(job2)));
	}

	@Test
	public void testFreestyleUpstreamOfWorkflowJob() throws IOException {
		FreeStyleProject job1 = jenkins.createProject(FreeStyleProject.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		assertEquals(2, jobGraph.getJobs().size());
		assertEquals(Collections.singleton(new ProjectWrapper(job1)), jobGraph.getUpstreamOfJob(new WorkflowJobWrapper(job2)));
	}
	
	/*
	@Test
	public void testAutomaticUpdateOfGraph() throws IOException {
		jenkins.createProject(WorkflowJob.class, "1");
		WorkflowJob job2 = jenkins.createProject(WorkflowJob.class, "2");
		job2.addTrigger(new ReverseBuildTrigger("1"));
		JobGraph jobGraph = new JobGraph();
		WorkflowJob job3 = jenkins.createProject(WorkflowJob.class, "3");
		job3.addTrigger(new ReverseBuildTrigger("2"));
		assertEquals(Collections.singleton(job2), jobGraph.getUpstreamOfJob(job3));
	} */
}
