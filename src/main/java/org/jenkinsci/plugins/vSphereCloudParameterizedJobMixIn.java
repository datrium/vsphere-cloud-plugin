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

import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.CauseAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue.WaitingItem;
import hudson.Util;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import jenkins.util.TimeDuration;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public abstract class vSphereCloudParameterizedJobMixIn<JobT extends Job<JobT, RunT> & vSphereCloudParameterizedJobMixIn.ParameterizedJob<JobT, RunT> & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {

    private static final Logger LOGGER = Logger.getLogger(vSphereCloudParameterizedJobMixIn.class.getName());

    /*
        _doBuild is from ParametersDefinitionProperty. There are issues with subclassing and calling it from doBuild.
        For this method, the only changes are to update its signature to take pp and call asJob() instead of getJob().
     */
    public void _doCloneBuild(ParametersDefinitionProperty pp, StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay) throws IOException, ServletException {
        if (delay == null) {
            delay = new TimeDuration(asJob().getQuietPeriod());
        }

        List<ParameterValue> values = new ArrayList<ParameterValue>();

        JSONObject formData = req.getSubmittedForm();
        JSONArray a = JSONArray.fromObject(formData.get("parameter"));

        for (Object o : a) {
            JSONObject jo = (JSONObject) o;
            String name = jo.getString("name");

            ParameterDefinition d = pp.getParameterDefinition(name);
            if(d == null) {
                throw new IllegalArgumentException("No such parameter definition: " + name);
            }
            ParameterValue parameterValue = d.createValue(req, jo);
            if (parameterValue != null) {
                values.add(parameterValue);
            } else {
                throw new IllegalArgumentException("Cannot retrieve the parameter value: " + name);
            }
        }

        LOGGER.log(Level.INFO, "_doCloneBuild() label: " + asJob().getAssignedLabel());
        WaitingItem item = Jenkins.getInstance().getQueue().schedule(
                asJob(), (int)delay.getTimeInMillis()/1000, new ParametersAction(values), new CauseAction(new Cause.UserIdCause()));
        if (item != null) {
            String url = formData.optString("redirectTo");
            if (url == null || !Util.isSafeToRedirectTo(url))   // avoid open redirect
                url = req.getContextPath()+'/'+item.getUrl();
            rsp.sendRedirect(formData.optInt("statusCode",SC_CREATED), url);
        } else
            // send the user back to the job top page.
            rsp.sendRedirect(".");
    }

    /*
        doBuild is final in the superclass, so create doCloneBuild and call it from the interface. The only change
        is to call _doCloneBuild and pass it the pp instance.
     */
    public void doCloneBuild(StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay) throws IOException, ServletException {
        if (delay == null) {
            delay = new TimeDuration(asJob().getQuietPeriod());
        }

        if (!asJob().isBuildable()) {
            throw HttpResponses.error(SC_CONFLICT, new IOException(asJob().getFullName() + " is not buildable"));
        }

        // if a build is parameterized, let that take over
        ParametersDefinitionProperty pp = asJob().getProperty(ParametersDefinitionProperty.class);
        if (pp != null && !req.getMethod().equals("POST")) {
            // show the parameter entry form.
            req.getView(pp, "index.jelly").forward(req, rsp);
            return;
        }

        hudson.model.BuildAuthorizationToken.checkPermission(asJob(), asJob().getAuthToken(), req, rsp);

        if (pp != null) {
            _doCloneBuild(pp, req, rsp, delay);
            return;
        }

        LOGGER.log(Level.INFO, "doCloneBuild() label: " + asJob().getAssignedLabel());
        Queue.Item item = Jenkins.getInstance().getQueue().schedule2(asJob(), 0, getBuildCause(asJob(), req)).getItem();
        if (item != null) {
            rsp.sendRedirect(SC_CREATED, req.getContextPath() + '/' + item.getUrl());
        } else {
            rsp.sendRedirect(".");
        }
    }
}
