/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hive.testutils.dtest.server;

import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.BuildState;
import org.apache.hive.testutils.dtest.DockerTest;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Path("dtest/v1")
public class DTestResource {
  private static final String STATUS_MSG = "Uh... had a slight weapons malfunction. But, uh, " +
        "everything's perfectly all right now. We're fine. We're all fine here, now, thank you. " +
        "How are you?";

  public static void initialize(DockerTest main) {
    DTestManager.initialize(main);
  }

  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatus() {
    return buildResponse(200, STATUS_MSG);
  }

  @POST
  @Path("/build/{label}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response submitBuild(
      @FormParam("repo") String repo,
      @FormParam("branch") String branch,
      @PathParam("label") String label) {
    try {
      BuildInfo info = new BuildInfo(branch, repo, label);
      DTestManager.get().submitBuild(info);
      return buildResponse(200, "OK");
    } catch (IOException e) {
      return buildResponse(406, "Build submission failed", "Details", e.getMessage());
    }
  }

  @GET
  @Path("/build/{label}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response fetchStatus(@PathParam("label") String label,
                              @QueryParam("logs") boolean getLogs) {
    BuildInfo info;
    info = DTestManager.get().findBuild(label);
    if (info == null) return buildResponse(404, label + ", no such build");
    if (getLogs) {
      try {
      String logs = DTestManager.get().getLogs(info);
      return buildResponse(200, "OK", "repo", info.getRepo(), "branch", info.getBranch(),
          "logs", logs);
      } catch (IOException e) {
        return buildResponse(500, "Unable to fetch logs", "Details", e.getMessage());
      }
    } else {
      return buildResponse(200, info.getState().name());
    }
  }

  @GET
  @Path("/builds")
  @Produces(MediaType.APPLICATION_JSON)
  public Response fetchAllStats() {
    Map<String, BuildState> m = DTestManager.get().getFullState();
    return Response.status(200).entity(m).build();
  }

  @DELETE
  @Path("/build/{label}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response killBuild(@PathParam("label") String label) {
    BuildInfo info;
    info = DTestManager.get().findBuild(label);
    if (info == null) return buildResponse(404, label + ", no such build");
    boolean killed = DTestManager.get().killBuild(info);
    return buildResponse(200, "Killed the build: " + killed);
  }

  private Response buildResponse(int statusCode, String statusMsg, String... additionalInfo) {
    Map<String, String> entity;
    if (additionalInfo == null || additionalInfo.length == 0) {
      entity = Collections.singletonMap("status", statusMsg);
    } else {
      assert additionalInfo.length % 2 == 0;
      entity = new HashMap<>(additionalInfo.length / 2 + 1);
      entity.put("status", statusMsg);
      for (int i = 0; i < additionalInfo.length; i += 2) {
        entity.put(additionalInfo[i], additionalInfo[i+1]);
      }
    }
    return Response
        .status(statusCode)
        .entity(entity)
        .build();
  }


}
