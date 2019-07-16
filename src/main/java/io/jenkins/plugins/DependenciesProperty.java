package io.jenkins.plugins;

import java.io.File;

import org.jenkinsci.plugins.workflow.FilePathUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.Functions;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class DependenciesProperty extends AbstractDescribableImpl<DependenciesProperty> {

	public DependenciesProperty() {
		// TODO Auto-generated constructor stub
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<DependenciesProperty> {
		
		private String dotExe;
		private String imagePath;
		private boolean drawBalls;

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			setDotExe(Util.fixEmptyAndTrim(json.getString("dotExe")));
			setImagePath(Util.fixEmptyAndTrim(json.getString("imagePath")));
			setDrawBalls(json.getBoolean("drawBalls"));
			return true;
		}

		public String getDotExeOrDefault() {
            if (Util.fixEmptyAndTrim(dotExe) == null) {
                return Functions.isWindows() ? "dot.exe" : "dot";
            } else {
                return dotExe;
        	}
		}

		public String getDotExe() {
			return dotExe;
		}
		
		public void setDotExe(String dotExe) {
			this.dotExe = dotExe;
			save();
		}

		public String getImagePath() {
			return imagePath;
		}

		public void setImagePath(String imagePath) {
			this.imagePath = imagePath;
			save();
		}

		public boolean isDrawBalls() {
			return drawBalls;
		}

		public void setDrawBalls(boolean drawBalls) {
			this.drawBalls = drawBalls;
			save();
		}

		public FormValidation doCheckDotExe(@QueryParameter final String value) {
            return FormValidation.validateExecutable(value);
		}

		public FormValidation doCheckImagePath(@QueryParameter final String value) {
            File file = new File(value);
            if (!file.isDirectory()) {
            	return FormValidation.error("The given path does not point to a valid directory");
            }
            return FormValidation.ok();
		}
	}
}
