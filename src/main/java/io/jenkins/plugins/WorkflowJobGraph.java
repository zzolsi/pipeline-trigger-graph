package io.jenkins.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import jenkins.model.Jenkins;
import jenkins.triggers.ReverseBuildTrigger;;

/**
 * Maintains a graph of job-triggers.
 * @author OLSI
 *
 */
public class WorkflowJobGraph {
	
	private Jenkins jenkins;
	private Map<WorkflowJob, Set<WorkflowJob>> upstream = new HashMap<WorkflowJob, Set<WorkflowJob>>();
	private Map<WorkflowJob, Set<WorkflowJob>> downstream = new HashMap<WorkflowJob, Set<WorkflowJob>>();

	private static WorkflowJobGraph instance;
	private static final Logger logger = Logger.getLogger(WorkflowJobTriggersAction.class.getName());

	private WorkflowJobGraph() {
		this.update();
	}

	/**
	 * retrieve the upstream dependencies of all jobs and save them into a graph
	 */
	public void update() {
		this.jenkins = Jenkins.get();
		List<WorkflowJob> allWorkflowJobs = jenkins.getAllItems(WorkflowJob.class);
		for (WorkflowJob workflowJob : allWorkflowJobs) {
			loadUpstreamOfJob(workflowJob);
		}
	}
	
	/**
	 * Gets the WorkflowJobGraph instance
	 * @return a singleton instance of the WorkflowJobGraph
	 * TODO: handle updates
	 */
	public static WorkflowJobGraph get() {
		//if (instance == null) {
			instance = new WorkflowJobGraph();
		//}
		//instance.update();
		return instance;
	}
	
	/**
	 * Generates a representations of the graph as GraphViz dot
	 * @return the dot-string representation
	 * TODO: color the nodes with respect to the buildstate
	 */
	public String getDotString(WorkflowJob current) {
		String dot = "digraph { \n\tnode [shape=box, style=rounded, fontname=sans ];\n";
		DescriptorImpl settings = jenkins.getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
		//dot += current.getDisplayName()+" [style=bold];\n";
		for (WorkflowJob job : upstream.keySet()) {
			String nodeStyle = job.equals(current) ?  "rounded,filled" : "rounded"; // <td><img src=\""+job.getIconColor().getImage()+"\" /></td>
			String nodeImage = settings.isDrawBalls() ? "<td><img src=\""+job.getIconColor().getImage()+"\" /></td>" : "";
			//String nodeImage = settings.isDrawBalls() ? "<td><img src=\"http://localhost:8080/jenkins/images/32x32/"+job.getIconColor().getImage()+"\" /></td>" : "";
			String nodeLabel = "<<table border=\"0\"><tr>"+nodeImage+"<td>"+job.getDisplayName()+"</td></tr></table>>";
			String nodeHref = job.getAbsoluteUrl();
			dot += "\t" + job.getDisplayName() + " [style=\""+nodeStyle+"\", label="+nodeLabel+", href=\""+nodeHref+"\"]; \n";
		}
		for (WorkflowJob targetJob : upstream.keySet()) {
			//dot += "\t"+targetJob.getDisplayName()+" [color="+colorOfBuildstatus(targetJob)+"]\n";
			Set<WorkflowJob> sourceJobs = upstream.get(targetJob);
			if (sourceJobs.isEmpty()) {
				dot += "\t"+targetJob.getDisplayName()+";\n";
			} else {
				for (WorkflowJob sourceJob : sourceJobs) {
					dot += "\t"+sourceJob.getDisplayName()+"->"+targetJob.getDisplayName()+";\n";
				}
			}
		}
		return dot+"}";
	}
	
	/**
	 * The Graph of the WorkflowJob dependencies
	 * @return Adjacent-Map of each Job to its predecessors
	 */
	public Map<WorkflowJob, Set<WorkflowJob>> getUpstream() {
		return upstream;
	}

	/**
	 * Get the predecessor of a specific job
	 * @param job which gets triggered by other jobs
	 * @return the jobs which trigger the given job
	 */
	public Set<WorkflowJob> getUpstreamOfJob(WorkflowJob job) {
		return upstream.get(job);
	}

	/**
	 * Get the successor of a specific job
	 * @param job which triggers by other jobs
	 * @return the jobs which are triggered the given job
	 */
	public Set<WorkflowJob> getDownstreamOfJob(WorkflowJob job) {
		return downstream.get(job);
	}
	
	/**
	 * Lookup all Jobs which haven't an predecessor
	 * @return all first ancestors in the graph
	 */
	public Set<WorkflowJob> getEntries() {
		return getEntriesOrExits(true);
	}
	
	/**
	 * Lookup all Jobs which haven't an predecessor
	 * @return all first ancestors in the graph
	 */
	public Set<WorkflowJob> getExits() {
		return getEntriesOrExits(false);
	}
	
	/**
	 * Lookup all Jobs which haven't an predecessor
	 * @return all first ancestors in the graph
	 */
	private Set<WorkflowJob> getEntriesOrExits(boolean getEntry) {
		Set<WorkflowJob> entriesOrExits = new HashSet<WorkflowJob>();
		Map<WorkflowJob, Set<WorkflowJob>> graph = getEntry ? upstream : downstream;
		for (WorkflowJob job : graph.keySet()) {
			if (graph.get(job).isEmpty() && !entriesOrExits.contains(job)) {
				entriesOrExits.add(job);
			}
		}
		return entriesOrExits;
	}
	
	/**
	 * calculate the upstream dependency graph of a given job
	 * @param job to get its upstream dependencies of
	 */
	private void loadUpstreamOfJob(WorkflowJob job) {
		if (upstream.containsKey(job)) {
			return;
		} else {
			insertNode(job);
			Set<WorkflowJob> predecessors = this.getPredecessorsOfJob(job);
			for (WorkflowJob predecesssor : predecessors) {
				loadUpstreamOfJob(predecesssor);
				addPredecessorToUpstream(job, predecesssor);
			}
		}
	}
	
	/**
	 * retrieves a set of jobs which can trigger a given job
	 * @param job triggered by others
	 * @return set of jobs which have triggered the given job
	 */
	private Set<WorkflowJob> getPredecessorsOfJob(WorkflowJob job) {
		Map<TriggerDescriptor,Trigger<?>> jobTriggers = job.getTriggers();
		Set<WorkflowJob> predecessors = new HashSet<WorkflowJob>();
		for (TriggerDescriptor triggerDescriptor : jobTriggers.keySet()) {
			Trigger<?> jobTrigger = jobTriggers.get(triggerDescriptor);
			if (jobTrigger instanceof ReverseBuildTrigger) {
				ReverseBuildTrigger buildTrigger = (ReverseBuildTrigger)jobTrigger;
				String upProj = buildTrigger.getUpstreamProjects();
				predecessors.addAll(getJobListFromString(upProj));
			}
		}
		return predecessors;
	}
	
	/**
	 * Takes a comma-separated list of projectnames and assembles a list of WorkflowJobs know to jenkins
	 * @param projects String of comma-separated project names
	 * @return a Set of WorkflowJobs specified by the input String
	 */
	private Set<WorkflowJob> getJobListFromString(String projects) {
		Set<WorkflowJob> jobs = new HashSet<WorkflowJob>();
		for (String jobName : projects.split(Pattern.quote(","))) {
			jobName = jobName.trim();
			if (!jobName.isEmpty()) {
				logger.info("proj = "+jobName);
				WorkflowJob job = getJobByName(jobName);
				if (job != null) {
					jobs.add(job);
				}
			}
		}
		return jobs;
	}
	
	/**
	 * Get a WorkflowJob which is known to jenkins
	 * @param jobName to find
	 * @return the WorkflowJob-Object found by jenkins
	 * TODO: do something with freestyle projects;
	 */
	private WorkflowJob getJobByName(String jobName) {
		WorkflowJob workflowJob = jenkins.getItemByFullName(jobName, WorkflowJob.class);
		logger.info("looked up "+jobName+" workflow = "+workflowJob);
		if (workflowJob != null && workflowJob.getFullName().equals(jobName)) {
			return workflowJob;
		} else {
			logger.warning("workflowjob not added to list "+jobName); 
			return null;
		}
	}
	
	/**
	 * adds a triggering job
	 * @param node the job to add predecessor to
	 * @param predecessor list of upstream jobs
	 */
	private void addPredecessorToUpstream(WorkflowJob node, WorkflowJob predecessor) {
		insertNode(node);
		insertNode(predecessor);
		upstream.get(node).add(predecessor);
		downstream.get(predecessor).add(node);
	}
	
	/**
	 * adds a new job to the graph
	 * @param node job to add
	 */
	private void insertNode(WorkflowJob node) {
		if (upstream.containsKey(node)) {
			return;
		} else {
			upstream.put(node, new HashSet<WorkflowJob>());
			downstream.put(node, new HashSet<WorkflowJob>());
		}
	}
	
	private String colorOfBuildstatus(WorkflowJob job) {
		String buildIconClass = job.getBuildStatusIconClassName();
		return "light"+buildIconClass.substring(buildIconClass.lastIndexOf("-")+1);
	}
}
