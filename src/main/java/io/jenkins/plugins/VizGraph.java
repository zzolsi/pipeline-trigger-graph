package io.jenkins.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Launcher;
import hudson.model.Run;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import jenkins.model.Jenkins;

public class VizGraph {
	
	private static final Logger logger = Logger.getLogger(RunTriggersAction.class.getName());
	
    /**
     * from https://github.com/kohsuke/depgraph-view-plugin/blob/master/src/main/java/hudson/plugins/depgraph_view/AbstractDependencyGraphAction.java
     * Execute the dot commando with given input and output stream
     * @param type the parameter for the -T option of the graphviz tools
     */
    static void runDot(OutputStream output, InputStream input, String type)
            throws IOException {
    	DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
        String dotPath = descriptor.getDotExeOrDefault();//"C:\\Users\\OLSI\\Downloads\\graphviz\\bin\\dot.exe";
        String imagePath = descriptor.getImagePath();
        Launcher launcher = Jenkins.get().createLauncher(new LogTaskListener(logger, Level.CONFIG));
        logger.info("current Workspace-Dir: "+System.getProperty("user.dir"));
        try {
        	int exitCode = launcher.launch()
                    .cmds(dotPath, "-q", "-T" + type, "-Kdot")//, "GV_FILE_PATH=\""+imagePath+"\"")
                    .stdin(input)
                    .stdout(output).start().join();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while waiting for dot-file to be created",e);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
