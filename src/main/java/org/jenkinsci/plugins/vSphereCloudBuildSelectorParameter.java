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

import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.BuildSelectorParameter;


/**
 */
public class vSphereCloudBuildSelectorParameter extends BuildSelectorParameter {
    private static final Logger LOGGER = Logger.getLogger(vSphereCloudBuildSelectorParameter.class.getName());

    @DataBoundConstructor
    public vSphereCloudBuildSelectorParameter(String name, BuildSelector defaultSelector, String description) {
        super(name, defaultSelector, description);
    }

    @Extension
    public static class DescriptorImpl extends BuildSelectorParameter.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return Messages.vSphereCloudBuildSelectorParameter_DisplayName();
        }
    }
}
