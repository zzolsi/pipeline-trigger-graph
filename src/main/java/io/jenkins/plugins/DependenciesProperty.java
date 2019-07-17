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

import java.io.File;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.Functions;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class DependenciesProperty extends AbstractDescribableImpl<DependenciesProperty> {

	@Extension
	public static class DescriptorImpl extends Descriptor<DependenciesProperty> {
		
		private String dotExe = Functions.isWindows() ? "dot.exe" : "dot";
		private String imagePath;
		private boolean drawBalls = false;
		private boolean hideDisabled = false;
		private boolean leftToRightLayout = true;
		private boolean countTriggersTransitively = false;
		private boolean linearUpstreamOfProject = false;
		private int selectedEdgeWidth = 1;

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			setDotExe(Util.fixEmptyAndTrim(json.getString("dotExe")));
			setHideDisabled(json.getBoolean("hideDisabled"));
			setLeftToRightLayout(json.getBoolean("leftToRightLayout"));
			setSelectedEdgeWidth(json.getInt("selectedEdgeWidth"));
			setCountTriggersTransitively(json.getBoolean("countTriggersTransitively"));
			setLinearUpstreamOfProject(json.getBoolean("linearUpstreamOfProject"));
			return true;
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

		public boolean isHideDisabled() {
			return hideDisabled;
		}

		public void setHideDisabled(boolean hideDisabled) {
			this.hideDisabled = hideDisabled;
			save();
		}

		public boolean isLeftToRightLayout() {
			return leftToRightLayout;
		}

		public void setLeftToRightLayout(boolean leftToRightLayout) {
			this.leftToRightLayout = leftToRightLayout;
			save();
		}

		public int getSelectedEdgeWidth() {
			return selectedEdgeWidth;
		}

		public void setSelectedEdgeWidth(int selectedEdgeWidth) {
			this.selectedEdgeWidth = selectedEdgeWidth;
			save();
		}

		public boolean isCountTriggersTransitively() {
			return countTriggersTransitively;
		}

		public void setCountTriggersTransitively(boolean countTriggersTransitively) {
			this.countTriggersTransitively = countTriggersTransitively;
			save();
		}

		public boolean isLinearUpstreamOfProject() {
			return linearUpstreamOfProject;
		}

		public void setLinearUpstreamOfProject(boolean linearUpstreamOfProject) {
			this.linearUpstreamOfProject = linearUpstreamOfProject;
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

		public FormValidation doCheckSelectedEdgeWidth(@QueryParameter final String value) {
			return FormValidation.validatePositiveInteger(value);
		}
	}
}
