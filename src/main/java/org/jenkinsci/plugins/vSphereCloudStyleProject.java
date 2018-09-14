/*
 * Copyright (c) 2018 Datrium Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package org.jenkinsci.plugins;

import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject.AbstractProjectDescriptor;
import hudson.Extension;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import jenkins.util.TimeDuration;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;

import java.lang.Cloneable;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;


/*
    Create a new project type so that we can control the label and build methods.
 */
public class vSphereCloudStyleProject extends Project<vSphereCloudStyleProject, vSphereCloudStyleBuild>
        implements TopLevelItem, Cloneable, vSphereCloudParameterizedJobMixIn.ParameterizedJob<vSphereCloudStyleProject, vSphereCloudStyleBuild> {

    private static final Logger LOGGER = Logger.getLogger(vSphereCloudStyleProject.class.getName());

    private Label uniqueLabel = null;  // A unique label is assigned to every build.

    public vSphereCloudStyleProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Override
    protected Class<vSphereCloudStyleBuild> getBuildClass() {
        return vSphereCloudStyleBuild.class;
    }

    private Label getOrigLabel() {
        return super.getAssignedLabel();
    }

    @Override
    public @CheckForNull
    Label getAssignedLabel() {
        if (uniqueLabel == null) {
            return getOrigLabel();
        }
        return uniqueLabel;
    }

    @Override
    public String getCustomWorkspace() {
        String ws = getProperty(vSphereCloudJobProperty.class).getCustomWorkspace();
        if (ws.isEmpty()) {
            return null;
        }
        return ws;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        vSphereCloudStyleProject c = (vSphereCloudStyleProject) super.clone();
        return c;
    }

    /*
        Check the label assigned to this job and return true if is cloneable.

        NOTE: This plugin does not yet support multiple or complex label definitions.
     */
    private boolean isCloneable() {
        Label label = getAssignedLabel();
        if (label == null) {
            return false;
        }
        String supported = vSphereCloud.class.getName();
        for(Cloud c : Jenkins.getInstance().clouds) {
            if (c.getClass().getName() == supported && c.canProvision(label)) {
                return true;
            }
        }
        return false;
    }

    /*
        Override doBuild so that we can provide a unique slave label to the build. vSphereCloudParameterizedJobMixin
        provides a doCloneBuild method to implement the actual builder.
     */
    @Override
    public void doBuild(StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay) throws IOException, ServletException {
        if (!isCloneable()) {
            super.doBuild(req, rsp, delay);
            return;
        }
        try {
            Date n = new Date();
            SimpleDateFormat f = new SimpleDateFormat("yyMMddHHmmssSSS");
            vSphereCloudStyleProject c = (vSphereCloudStyleProject) this.clone();
            // Unique label has the format cloudlabel__PEZ__jobname__yymmddHHMMSSSSS
            // The provisioner will use the cloudlabel to find the appropriate cloud.
            // TODO(kyle) This can't exceed maximum VM name length (or we need to handle that elsewhere).
            c.uniqueLabel = Jenkins.getInstance().getLabel(this.getOrigLabel().getName() + "__PEZ__" + this.getName() + "__" + f.format(n));
            LOGGER.log(Level.INFO, "doBuild is using a unique label: " + c.uniqueLabel);
            LOGGER.log(Level.INFO, "doBuild is using a custom workspace: " + c.getCustomWorkspace());
            new CloneJobMixIn(c).doCloneBuild(req, rsp, delay);
        } catch (CloneNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, "duBuild failed to clone the job" + ex);
        }
    }

    /*
        Extend the ParameterizedJob Interface so that we can override the abstract asJob() method. We want to change the
        label only for the job instance contained within the Queue.task. So, we need to override asJob and pass it a
        cloned instance with a unique label.
     */
    static class CloneJobMixIn<JobT extends Job<JobT, RunT> & vSphereCloudParameterizedJobMixIn.ParameterizedJob<JobT, RunT> & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends vSphereCloudParameterizedJobMixIn<JobT, RunT> {
        private JobT job;

        CloneJobMixIn(JobT job) {
            this.job = job;
        }

        @Override
        protected JobT asJob() {
            return job;
        }
    }

    @Extension(ordinal=1010)
    public static class DescriptorImpl extends AbstractProjectDescriptor {

        public String getDisplayName() {
            return Messages.vSphereCloudStyleProject_DisplayName();
        }

        public vSphereCloudStyleProject newInstance(ItemGroup parent, String name) {
            return new vSphereCloudStyleProject(parent, name);
        }

        // TODO(kyle) Not sure why the compiler claims some of these method signatures do not exist.
        //@Override
        public String getDescription() {
            return Messages.vSphereCloudStyleProject_Description();
        }

        //@Override
        public String getCategoryId() {
            // TODO(kyle) Requires newer Jenkins version.
            //return StandaloneProjectsCategory.ID;
            return "standalone-projects";
        }

        //@Override
        public String getIconFilePathPattern() {
            return (Jenkins.RESOURCE_PATH + "/images/:size/freestyleproject.png").replaceFirst("^/", "");
        }

        //@Override
        public String getIconClassName() {
            return "icon-freestyle-project";
        }

        static {
            IconSet.icons.addIcon(new Icon("icon-freestyle-project icon-sm", "16x16/freestyleproject.png", Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(new Icon("icon-freestyle-project icon-md", "24x24/freestyleproject.png", Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(new Icon("icon-freestyle-project icon-lg", "32x32/freestyleproject.png", Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(new Icon("icon-freestyle-project icon-xlg", "48x48/freestyleproject.png", Icon.ICON_XLARGE_STYLE));
        }
    }
}
