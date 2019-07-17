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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Launcher;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.DependenciesProperty.DescriptorImpl;
import jenkins.model.Jenkins;

public class GraphViz {
	
	private static final Logger logger = Logger.getLogger(RunTriggersAction.class.getName());
	
	private GraphViz() {
		throw new IllegalStateException("Utility class");
	}
	
    /**
     * from https://github.com/kohsuke/depgraph-view-plugin/blob/master/src/main/java/hudson/plugins/depgraph_view/AbstractDependencyGraphAction.java
     * Execute the dot commando with given input and output stream
     * @param type the parameter for the -T option of the graphviz tools
     * @throws InterruptedException 
     */
    static void runDot(OutputStream output, InputStream input, String type)
            throws IOException, InterruptedException {
    	DescriptorImpl settings = Jenkins.get().getDescriptorByType(DependenciesProperty.DescriptorImpl.class);
        String dotPath = settings.getDotExe();
        Launcher launcher = Jenkins.get().createLauncher(new LogTaskListener(logger, Level.CONFIG));
        try {
        	launcher.launch()
                    .cmds(dotPath, "-q", "-T" + type, "-Kdot")
                    .stdin(input)
                    .stdout(output).start().join();
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
