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
package org.dtest.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utilities for testing.  In this package so that implementations don't have to include a specific test module.
 */
public class TestUtils {

  /**
   * Construct a set of properties from string pairs.
   * @param vals property keys and values, as key, value, [key, value...].  There must be an even number of strings.
   * @return properties object.
   */
  public static Properties buildProperties(String... vals) {
    Properties props = new Properties();
    assert vals.length % 2 == 0 : "Requires even number of args";
    for (int i = 0; i < vals.length; i += 2) {
      props.setProperty(vals[i], vals[i+1]);
    }
    return props;
  }

  /**
   * Construct a config file from string pairs.
   * @param vals property keys and values, as key, value, [key, value...].  There must be an even number of strings.
   * @return config object.
   */
  public static Config buildCfg(String... vals) {
    return new Config(buildProperties(vals));
  }

  /**
   * Get the configuration directory for running unit tests.  This assumes that any dtest.properites or dtest.yaml
   * files are in src/test/resources and you have defined java.io.tmpdir to point to your target directory.
   * @return directory where the configuration files are
   */
  public static String getConfDir() {
    return System.getProperty("java.io.tmpdir") + File.separator + "test-classes";
  }

  /**
   * An implementation of DTestLogger that collects all of the log output so it can be examined by tests.  Results can,
   * and should, be dumped to Slf4j log by calling {@link #dumpToLog()} at the end of the test.
   */
  public static class TestLogger implements DTestLogger {
    private Logger log = LoggerFactory.getLogger(TestUtils.class);
    private List<String> entries = new ArrayList<>();

    @Override
    public void error(String msg) {
      append("ERROR", msg);
    }

    @Override
    public void error(String msg, Throwable t) {
      append("ERROR", msg, t);
    }

    @Override
    public void warn(String msg) {
      append("WARN", msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
      append("WARN", msg, t);
    }

    @Override
    public void info(String msg) {
      append("INFO", msg);
    }

    @Override
    public void info(String msg, Throwable t) {
      append("INFO", msg, t);
    }

    @Override
    public void debug(String msg) {
      append("DEBUG", msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
      append("DEBUG", msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public boolean isDebugEnabled() {
      return true;
    }

    @Override
    public String toString() {
      // Returns the entire log in one big string, with each entry separated by returns
      StringBuilder buf = new StringBuilder();
      for (String entry : entries) buf.append(entry).append("\n");
      return buf.toString();
    }

    /**
     * Dump the contents of this log to the slf4j log, at info level.
     */
    public void dumpToLog() {
      log.info("Entire logs from test:");
      for (String entry : entries) log.info(entry);
    }

    private void append(String level, String msg) {
      append(level, msg, null);
    }

    private void append(String level, String msg, Throwable t) {
      entries.add(level + ": " + msg);
      if (t != null) {
        entries.add("Caught exception " + t.getClass().getName() + " with message " + t.getMessage());
        StackTraceElement[] stack = t.getStackTrace();
        for (StackTraceElement element : stack) entries.add("   " + element.toString());
      }

    }
  }

  /**
   * Build a yaml file specific to dtest.
   * @return a yaml file.
   */
  public static BuildYaml getYaml() {
    BuildYaml yaml = new BuildYaml();
    yaml.setBaseImage("centos");
    yaml.setRequiredPackages(new String[] {"java-1.8.0-openjdk-devel"});
    yaml.setProjectName("dtest");
    yaml.setJavaPackages(new String[] {"org.dtest"});
    ModuleDirectory[] dirs = new ModuleDirectory[2];
    dirs[0] = new ModuleDirectory();
    dirs[0].setDir("core");
    dirs[1] = new ModuleDirectory();
    dirs[1].setDir("maven");
    yaml.setDirs(dirs);
    return yaml;
  }

}
