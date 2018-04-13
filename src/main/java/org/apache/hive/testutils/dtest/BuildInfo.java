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
package org.apache.hive.testutils.dtest;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildInfo implements Comparable<BuildInfo> {
  private final Pattern dockerable = Pattern.compile("[A-Za-z0-9_\\-]+");
  private final String branch;
  private final String repo;
  private final String label;
  private long queueTime;
  private long startTime;
  private long completionTime;
  private String dir;
  private boolean success;
  private boolean killed;

  public BuildInfo(String branch, String repo, String label) throws IOException {
    this.branch = branch;
    this.repo = repo;
    this.label = checkLabelIsDockerable(label);
    startTime = queueTime = 0;
    dir = null;
    success = killed = false;
  }

  /**
   * Determine a create a directory for this Build.
   * @return directory name
   * @throws IOException if the directory can't be built.
   */
  String buildDir(String baseDir) throws IOException {
    if (dir != null) return dir;
    File d = new File(baseDir, label);
    d.mkdir();
    dir = d.getAbsolutePath();
    return dir;
  }

  public String getBranch() {
    return branch;
  }

  public String getRepo() {
    return repo;
  }

  public String getLabel() {
    return label;
  }

  public String getDir() {
    return dir;
  }

  public long getQueueTime() {
    return queueTime;
  }

  public void setQueueTime(long queueTime) {
    this.queueTime = queueTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(long completionTime) {
    this.completionTime = completionTime;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public boolean isKilled() {
    return killed;
  }

  public void setKilled(boolean killed) {
    this.killed = killed;
  }

  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BuildInfo)) return false;
    BuildInfo that = (BuildInfo)obj;
    return label.equals(that.label);
  }

  @Override
  public String toString() {
    return "repo: " + repo + ", branch: " + branch + (label == null ? "" : ", label: " + label);
  }

  @Override
  public int compareTo(BuildInfo o) {
    return label.compareTo(o.label);
  }

  private String checkLabelIsDockerable(String label) throws IOException {
    Matcher m = dockerable.matcher(label);
    if (m.matches()) {
      return label;
    } else {
      throw new IOException("Label must be usable in docker container name, should only contain " +
          "[A-Za-z0-9_\\-]");
    }
  }
}
