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

public class BuildInfo implements Comparable<BuildInfo> {
  public final String branch;
  public final String repo;
  public final String label;
  private long queueTime;
  private long startTime;
  private long completionTime;
  private String dir;

  public BuildInfo(String branch, String repo, String label) {
    this.branch = branch;
    this.repo = repo;
    this.label = cleanseName(label);
    startTime = queueTime = 0;
    dir = null;
  }

  public BuildInfo(String branch, String repo) {
    this(branch, repo, null);
  }

  /**
   * Determine a create a directory for this Build.
   * @return directory name
   * @throws IOException if the directory can't be built.
   */
  String buildDir(String baseDir) throws IOException {
    if (dir != null) return dir;
    StringBuilder buf = new StringBuilder(cleanseName(branch));
    if (label != null) {
        buf.append('-')
            .append(label);
    }
    File d = new File(baseDir, buf.toString());
    d.mkdir();
    dir = d.getAbsolutePath();
    return dir;
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

  private String cleanseName(String name) {
    return name == null ? null : name.replace('/', '-').replace(' ', '-');
  }

  @Override
  public int hashCode() {
    int code = branch.hashCode() * 31 + repo.hashCode();
    if (label != null) code = code * 31 + label.hashCode();
    return code;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BuildInfo)) return false;
    BuildInfo that = (BuildInfo)obj;
    if (branch.equals(that.branch) && repo.equals(that.repo)) {
      if ((label == null && that.label == null) ||
          (label != null && label.equals(that.label))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "repo: " + repo + ", branch: " + branch + (label == null ? "" : ", label: " + label);
  }

  @Override
  public int compareTo(BuildInfo o) {
    int c = repo.compareTo(o.repo);
    if (c != 0) return c;
    c = branch.compareTo(o.branch);
    if (c != 0) return c;
    if (label == null) {
      if (o.label == null) return 0;
      else return -1;
    } else {
      if (o.label == null) return 1;
      else return label.compareTo(o.label);
    }
  }
}
