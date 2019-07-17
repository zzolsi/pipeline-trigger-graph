# Trigger-Graph Viewer for Pipeline Jobs
This plugin shows the up- and downstream dependencies of Jenkins' pipeline jobs and freestyle projects to visualize your build setup. 


## Motivation

The reason for writing a new plugin to display build trigger dependencies is that the existing plugin 
([depgraph-view-plugin](https://github.com/jenkinsci/depgraph-view-plugin)) 
is only working with freestyle projects since they are using the internal 
JobDependencyGraph which does not include pipeline jobs 
(See [Jenkins issue 29913](https://issues.jenkins-ci.org/browse/JENKINS-29913)). 
This Plugin is managing the dependency graph by itself as a workaround to this problem. 


## Features

- Show a graph overview of all pipeline and freestyle jobs.
- Show the up- and downstream dependencies of a specific job.
- Show the upstream causes of a build.
- Highlight a selected job.
- Download the GraphViz source file.
- Hide jobs which are not reachable from a shown job.
- Hide or gray out deactivated jobs.


## Building and running

It is recommended to use Jenkins version 2.164.1 or higher in order to run this plugin.
To start it locally run `mvn hpi:run`.
To install this plugin run `mvn package` and upload the .hpi-file from the `target/`-directory to your Jenkins installation. 
GraphViz must be installed on your jenkins server.
Specify the path to the dot executable in the settings and click on *Triggers Graph* to display your build dependencies.


## Settings

- **Path to the GraphViz-Dot Executable** specifies the path to the GraphViz executable on the machine where jenkins is running.
- **Hide disabled jobs** Disabled jobs will be hidden in the graph if this option is active. Otherwise disabled Jobs will shown grayed out.
- **Draw graph from left to right** specifies if the graph layouts in reading direction or from top to bottom if disabled.
- **Count triggers transitively** If this option is activated the number of paths to each job will be calculated and shown bracketed next to the name of the job
- **Show a linear upstream of a project** specifies if nodes which are not directly upstream of a selected job will be drawn. 
- **Line width of all incident edges of a selected node** If a job is selected all ingoing and outgoing edges will be drawn with this width.


## Missing Features

- respect access permissions
- respect sub projects
- restrict jobs to one view
- display the last build state
- integration into blue-ocean
- directly update triggers throug the UI

## Contributing to the Plugin
Any contribution is welcome. Plugin source code is hosted on GitHub. New feature proposals and bug fix proposals should be submitted as pull requests. Fork the repository. Make the desired changes in your forked copy. Submit a pull request to the master branch. Your pull request will be evaluated by the Jenkins job.

Before submitting your pull request, please add tests which verify your change. Tests help us assure that we're delivering a reliable plugin and that we've communicated our intent to other developers in a way that they can detect when they run tests.


## Licence
This project is licenced under the [MIT License](LICENSE.txt).
