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
package org.dtest.core.impl;

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.DTestLogger;
import org.dtest.core.DockerTest;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Utility methods.
 */
public class Utils {
  private static final String CONTAINER_BASE = "dtest-";

  /**
   * Get a Class object using the name of the class.
   * @param className the name of the class.
   * @param clazz class instance.  Junk argument so that the generic method knows what type to return.
   * @param <T> type of class instance to instantiate.
   * @return class instance.
   * @throws IOException if the class cannot be found or loaded.
   */
  @SuppressWarnings(value = "unchecked")
  public static <T> Class<? extends T> getClass(String className, Class<T> clazz)
      throws IOException {
    try {
      return (Class<? extends T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IOException("Failed to find class " + className, e);
    }
  }

  /**
   * Get an instance of a class using the class name.  This assumes the class has a no argument constructor.
   * @param theClass the class to get an instance of
   * @param <T> type the class should either be or extend
   * @return an instance of the class
   * @throws IOException if the class could not be found or the program doesn't have rights to instantiate the class
   */
  public static <T> T getInstance(Class<? extends T> theClass) throws IOException {
    if (theClass == null) return null;
    try {
      return theClass.newInstance();
    } catch (InstantiationException|IllegalAccessException e) {
      throw new IOException("Unable to instantiate " + theClass.getName(), e);
    }
  }

  /**
   * Run a process.  It is assumed these processes may generate large amounts of output and {@link StreamPumper} is
   * used to handle the output.
   * @param containerId id of the container, used in logging
   * @param secondsToWait how long to wait for this process, in seconds, before timing out.
   * @param log log object
   * @param cmd Command to run.  Executable should be the first element in the array, and the arguments passed
   *            as one element each.
   * @return the results of running the process.
   * @throws IOException if the process times out or is interrupted.  Note that this will not be thrown if running
   * the process itself fails.  That will be reflected in the return code of the ProcessResults.
   */
  public static ProcessResults runProcess(String containerId, long secondsToWait,
                                          DTestLogger log, String... cmd) throws IOException {
    log.info(DockerTest.EXEC_LOG, "Going to run: " + StringUtils.join(cmd, " "));
    Process proc = Runtime.getRuntime().exec(cmd);
    AtomicBoolean running = new AtomicBoolean(true);
    StreamPumper stdout = new StreamPumper(running, proc.getInputStream(), containerId, log);
    StreamPumper stderr = new StreamPumper(running, proc.getErrorStream(), containerId, log);
    new Thread(stdout).start();
    new Thread(stderr).start();
    try {
      if (!proc.waitFor(secondsToWait, TimeUnit.SECONDS)) {
        throw new IOException("Process " + cmd[0] + " failed to run in " + secondsToWait +
            " seconds");
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    } finally {
      running.set(false);
      stdout.finalPump();
      stderr.finalPump();
    }
    return new ProcessResults(stdout.getOutput(), stderr.getOutput(), proc.exitValue());
  }

  /**
   * Does the generic work for a shell command to be executed in the container root directory.
   * @param dir directory in the container
   * @param cmdGenerator A function that generates the command.  This should not change
   *                     directories and should assume that it runs in the container root.
   * @return arguments for an exec call.
   */
  public static String[] shellCmdInRoot(String dir, Supplier<String> cmdGenerator) {
    String[] cmd = new String[3];
    cmd[0] = "/bin/bash";
    cmd[1] = "-c";
    cmd[2] = "( cd " + dir + "; " + cmdGenerator.get() + ")";
    return cmd;
  }

  /**
   * Build the name of the container.  This should always be used when naming a container to make
   * sure you name it correctly.
   * @param label label of this build
   * @param name unique name for this container
   * @return the container name
   */
  public static String buildContainerName(String label, String name) {
    return CONTAINER_BASE + label + "_" + name;
  }

  private static WordGenerator wordGenerator = null;
  private static Random rand = null;

  /**
   * Generate a random label
   * @param numWords number of words in the label.  Each word will be separated by a '-'.
   * @return a random generated label, made of pronounceable (if not semantically valid) English words.
   */
  public static String generateRandomLabel(int numWords) {
    if (wordGenerator == null) {
      wordGenerator = new WordGenerator();
      rand = new Random();
    }

    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < numWords; i++) {
      if (i > 0) buf.append('-');
      buf.append(wordGenerator.newWord(rand.nextInt(5) + 5).toLowerCase());
    }
    return buf.toString();
  }

}
