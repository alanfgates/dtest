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
package org.dtest.core.testutils;

import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.DTestLogger;
import org.dtest.core.DockerTest;
import org.dtest.core.ModuleDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Utilities functions for testing.
 */
public class TestUtilities {

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
  public static File getConfDir() {
    return new File(System.getProperty("java.io.tmpdir"), "test-classes");
  }

  public static File createBuildDir() throws IOException {
    File buildDir = new File(System.getProperty("java.io.tmpdir"), "build-dir" + System.currentTimeMillis());
    if (!buildDir.mkdir() && !buildDir.isDirectory()) {
      throw new IOException("Failed to create temporary directory " + buildDir.getAbsolutePath());
    }
    return buildDir;
  }

  /**
   * Convience method to read the Yaml file for tests.  This will read the Yaml file from test/resources, and thus
   * will return different results than {@link #getYaml()} below.
   * @param cfg config object
   * @param log log object.
   * @return the yaml file
   * @throws IOException if readYaml underneath does.
   */
  public static BuildYaml buildYaml(Config cfg, DTestLogger log) throws IOException {
    return buildYaml(cfg, log, "profile");
  }

  /**
   * Build a Yaml file from a particular profile
   * @param cfg config object
   * @param log dtest logger
   * @param profile profile to build, you must have a matching yaml file
   * @return the read yaml file object
   * @throws IOException if readYaml underneath fails.
   */
  public static BuildYaml buildYaml(Config cfg, DTestLogger log, String profile) throws IOException {
    return BuildYaml.readYaml(getConfDir(), cfg, log, "repo", profile, null);
  }

  public static void assertFile(String expected, File file) throws IOException {
    if (!file.exists()) throw new AssertionError("Expected file " + file.getName() + " to exist");
    StringBuilder buf = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    reader.lines().forEach(s -> buf.append(s).append('\n'));
    reader.close();
    String actual = buf.toString();
    if (!expected.equals(actual)) {
      StringBuilder errorMsg = new StringBuilder("Files differ\n");
      BufferedReader expectedReader = new BufferedReader(new StringReader(expected));
      BufferedReader actualReader = new BufferedReader(new StringReader(actual));
      int lineNum = 0;
      while (true) {
        lineNum++;
        String expectedLine = expectedReader.readLine();
        String actualLine = actualReader.readLine();
        if (expectedLine == null && actualLine == null) break;
        if (expectedLine == null) {
          errorMsg.append("has unexpected line: ")
              .append(actualLine)
              .append("\n");
        } else if (actualLine == null) {
          errorMsg.append("expected to fine line: ")
              .append(expectedLine)
              .append("\n");
        } else if (!actualLine.equals(expectedLine)){
          errorMsg.append("files differ at line ")
              .append(lineNum)
              .append(" expected: ")
              .append(expectedLine)
              .append(" actual: ")
              .append(actualLine)
              .append("\n");
        }
      }
      throw new AssertionError(errorMsg.toString());
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

  /**
   * Read a log file and return the contents as a single string.
   * @param filename Path to the file
   * @return Contents of the file as a single string.
   * @throws IOException if the file cannot be opened or read
   */
  public static String readLogFile(String filename) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    StringBuilder log = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) log.append(line).append("\n");
    reader.close();
    return log.toString();
  }

  /**
   * Get a prefab DockerTest instance with a baked in profile (that matches the .yaml file in
   * test/resources) and the builddir set to target.
   * @param props Config properties to pass to this build.
   * @param log Logger to use in this build.
   * @return prefab DockerTest
   * @throws IOException if one of the called methods throws it.
   */
  public static DockerTest getAndPrepDockerTest(Properties props, DTestLogger log) throws IOException {
    return getAndPrepDockerTest(props, log, "profile");
  }

  /**
   * Get a prefab DockerTest instance with a baked in profile (that matches the .yaml file in
   * test/resources) and the builddir set to target.
   * @param props Config properties to pass to this build.
   * @param log Logger to use in this build.
   * @param profile profile name
   * @return prefab DockerTest
   * @throws IOException if one of the called methods throws it.
   */
  public static DockerTest getAndPrepDockerTest(Properties props, DTestLogger log, String profile) throws IOException {
    DockerTest test = new DockerTest();
    test.parseArgs(new String[] {"-p", profile, "-d", System.getProperty("java.io.tmpdir")});
    test.determineCfgDir();
    test.buildConfig(props);
    test.setLogger(log);
    return test;
  }


}
