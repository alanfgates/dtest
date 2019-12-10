/*
 * Copyright (C) 2018 Hortonworks Inc.
 *
 * Licenced under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dtest.core.mvn;

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ModuleDirectory;
import org.dtest.core.impl.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Container command that implements maven specific logic.
 */
public class MavenContainerCommand extends ContainerCommand {

  protected final String buildDir;
  protected final int cmdNumber;
  protected List<String> tests; // set of tests to run
  protected List<String> excludedTests; // set of tests to NOT run
  protected Map<String, String> envs;
  protected Map<String, String> properties; // properties to pass to maven (-DX=Y) val can be null

  /**
   * Protected because it should only be called by {@link MavenContainerCommandFactory} or subclasses.
   * @param moduleDir module directory for this command
   * @param buildDir working directory on the build machine.
   * @param cmdNumber command number for this command, used in logging and labeling containers.
   */
  protected MavenContainerCommand(ModuleDirectory moduleDir, String buildDir, int cmdNumber) {
    super(moduleDir);
    this.buildDir = buildDir;
    this.cmdNumber = cmdNumber;
    tests = new ArrayList<>();
    excludedTests = new ArrayList<>();
    envs = new HashMap<>();
    properties = new HashMap<>();
  }

  @Override
  public String containerSuffix() {
    return "unittest-" + cmdNumber;
  }

  @Override
  final public String[] shellCommand() {
    return Utils.shellCmdInRoot(buildDir, getCommandSupplier());
  }

  @Override
  public String containerDirectory() {
    return buildDir;
  }

  /**
   * Add a test to this container's list of tests.  Public so that it can be called by tests of subclasses.
   * @param test test to add
   */
  @VisibleForTesting
  public void addTest(String test) {
    tests.add(test);
  }

  /**
   * Give a list of tests that should not be run by this container.
   * @param toExclude tests to not run.
   */
  protected void excludeTests(String[] toExclude) {
    Collections.addAll(excludedTests, toExclude);
  }

  /**
   * Set a single environment variable for this container.
   * @param envVar variable name
   * @param value variable value.
   */
  protected void setEnv(String envVar, String value) {
    envs.put(envVar, value);
  }

  /**
   * Pass a set of environment variables to set for this container.
   * @param envs environment variables.
   */
  protected void addEnvs(Map<String, String> envs) {
    this.envs.putAll(envs);
  }

  /**
   * Pass a set of properties to set for this container.
   * @param props properties.
   */
  protected void addProperties(Map<String, String> props) {
    properties.putAll(props);
  }

  /**
   * Get the MavenCommandSupplier that should be used to generate this container command.
   * @return supplier
   */
  protected MavenCommandSupplier getCommandSupplier() {
    return new MavenCommandSupplier();
  }

  /**
   * A class to build maven commands based on configuration obtained from the build.yaml.  This class can be
   * overridden by subclasses of MavenContainerCommand to supply any additional or different maven commands
   * they require.
   */
  protected class MavenCommandSupplier implements Supplier<String> {
    public String get() {
      StringBuilder buf = new StringBuilder();
      for (Map.Entry<String, String> e : envs.entrySet()) {
        buf.append(e.getKey())
            .append('=')
            .append(e.getValue())
            .append(' ');
      }

      buf.append("/usr/bin/mvn test -Dsurefire.timeout=")
          .append(cfg.getAsTime(CFG_CONTAINERCOMMAND_SINGLERUNTIME, TimeUnit.SECONDS, CFG_CONTAINERCOMMAND_SINGLERUNTIME_DEFAULT));

      if (!tests.isEmpty()) {
        buf.append(" -Dtest=");
        boolean first = true;
        for (String test : tests) {
          if (first) first = false;
          else buf.append(',');
          buf.append(test);
        }
      }
      if (!excludedTests.isEmpty()) {
        buf.append(" -Dtest.excludes.additional=");
        boolean first = true;
        for (String excludedTest : excludedTests) {
          if (first) first = false;
          else buf.append(',');
          buf.append("**/")
            .append(excludedTest);
        }
      }
      for (Map.Entry<String, String> e : properties.entrySet()) {
        buf.append(" -D")
            .append(e.getKey());
        if (e.getValue() != null) {
          buf.append('=')
              .append(e.getValue());
        }
      }
      addAdditionalArguments(buf);
      return buf.toString();
    }

    /**
     * Allows subclasses to add additional arguments maven command if desired.  Defaults to NOP.
     * @param buf StringBuilder that will be used to construct the command line.
     */
    protected void addAdditionalArguments(StringBuilder buf) {

    }
  }
}
