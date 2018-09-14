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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.Jenkins;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.StatusBuildSelector;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;


/**
 */
public class vSphereCloudJobProperty extends JobProperty<Job<?,?>> {
    public static final String PROPERTY_NAME = "vsphere-cloud-job-property";
    private static final BuildSelector DEFAULT_BUILD_SELECTOR = new StatusBuildSelector(true);

    private String projectName;
    private String customWorkspace;
    private BuildSelector buildSelector;

    public String getProjectName() {
        return projectName;
    }

    public String getCustomWorkspace() {
        return customWorkspace;
    }

    public BuildSelector getBuildSelector() {
        return buildSelector;
    }

    @DataBoundConstructor
    public vSphereCloudJobProperty(String projectName, String customWorkspace) {
        this.projectName = projectName;
        this.customWorkspace = customWorkspace;
        setBuildSelector(DEFAULT_BUILD_SELECTOR);
    }

    @DataBoundSetter
    public void setBuildSelector(@Nonnull BuildSelector buildSelector) {
        this.buildSelector = buildSelector;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        public String getPropertyName() {
            return PROPERTY_NAME;
        }

        @Override
        public String getDisplayName() {
            return Messages.vSphereCloudJobProperty_DisplayName();
        }

        /**
         * @return {@link BuildSelector}s available for BuildSelectorParameter.
         */
        public List<Descriptor<BuildSelector>> getAvailableBuildSelectorList() {
            List<Descriptor<BuildSelector>> wanted = new ArrayList<Descriptor<BuildSelector>>();

            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                return wanted;
            }

            for (Descriptor<BuildSelector> item : jenkins.getDescriptorList(BuildSelector.class)) {
                String simpleName = item.clazz.getSimpleName();
                if (simpleName.equals("ParameterizedBuildSelector") || simpleName.equals("StatusBuildSelector")) {
                    wanted.add(item);
                }
            }
            return wanted;
        }

        @Override
        public vSphereCloudJobProperty newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            if(formData == null || formData.isNullObject()) {
                return null;
            }
            JSONObject form = formData.getJSONObject(getPropertyName());
            if(form == null || form.isNullObject()) {
                return null;
            }
            return (vSphereCloudJobProperty)super.newInstance(req, form);
        }
    }
}
