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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.apache.hive.testutils.dtest.impl.DockerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DockerTest.class);
  private static final String SUMMARY_LOG = "summary";

  private ContainerClient docker;
  private ContainerCommandFactory commandFactory;
  private ResultAnalyzerFactory analyzerFactory;

  /**
   * Run the test.
   * @param args Command line arguments.
   * @param out stdout
   * @param err stderr
   * @return suggested return code for the calling process.
   */
  public int run(String[] args, PrintStream out, PrintStream err) {
    CommandLineParser parser = new GnuParser();

    Options opts = new Options();
    opts.addOption(OptionBuilder
        .withLongOpt("branch")
        .withDescription("git branch to use")
        .isRequired()
        .hasArg()
        .create("b"));

    opts.addOption(OptionBuilder
        .withLongOpt("command-factory")
        .withDescription("Class to build ContainerCommands, defaults to MvnCommandFactory")
        .hasArg()
        .create("C"));

    opts.addOption(OptionBuilder
        .withLongOpt("num-containers")
        .withDescription("number of simultaneous containers to run, defaults to 1")
        .hasArg()
        .create("c"));

    opts.addOption(OptionBuilder
        .withLongOpt("target-directory")
        .withDescription("directory to build dockerfile in")
        .isRequired()
        .hasArg()
        .create("d"));

    opts.addOption(OptionBuilder
        .withLongOpt("container-factory")
        .withDescription("Class to build ContainerClients, defaults to ContainerClientFactory")
        .hasArg()
        .create("F"));

    opts.addOption(OptionBuilder
        .withLongOpt("build-number")
        .withDescription("build number, changing this will force a new container to be built")
        .isRequired()
        .hasArg()
        .create("n"));

    opts.addOption(OptionBuilder
        .withLongOpt("result-analyzer-factory")
        .withDescription("Class to build ResultAnalyzer, default to SimpleResultAnalyzer")
        .hasArg()
        .create("R"));

    opts.addOption(OptionBuilder
        .withLongOpt("repo")
        .withDescription("git repository to use")
        .isRequired()
        .hasArg()
        .create("r"));

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
    } catch (ParseException e) {
      LOG.error("Failed to parse command line: ", e);
      usage(opts);
      return 1;
    }

    int numContainers = cmd.hasOption("c") ? Integer.parseInt(cmd.getOptionValue("c")) : 1;
    String branch = cmd.getOptionValue("b");
    String dir = cmd.getOptionValue("d");
    String repo = cmd.getOptionValue("r");
    int buildNum = Integer.parseInt(cmd.getOptionValue("n"));

    DTestLogger logger = null;
    int rc = 0;
    try {
      logger = new DTestLogger(dir);
      try {
        ContainerClientFactory containerClientFactory =
            ContainerClientFactory.get(cmd.getOptionValue("F"));
        docker = containerClientFactory.getClient(buildNum);
        commandFactory = ContainerCommandFactory.get(cmd.getOptionValue("C"));
        analyzerFactory = ResultAnalyzerFactory.get(cmd.getOptionValue("R"));
      } catch (IOException e) {
        String msg = "Failed to instantiate one of the factories.";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
      try {
        buildDockerImage(dir, repo, branch, buildNum);
      } catch (IOException e) {
        String msg = "Failed to build docker image, might mean your code doesn't compile";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
      try {
        runContainers(logger, numContainers, out);
      } catch (IOException e) {
        String msg = "Failed to run one or more of the containers.";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
    } catch (IOException e) {
      String msg = "Failed to open the logger.  This often means you gave a bogus output directory.";
      err.println(msg);
      LOG.error(msg);
      return 1;
    } finally {
      if (logger != null) {
        try {
          logger.close();
        } catch (IOException e) {
          String msg = "Failed to close the logger.";
          err.println(msg);
          LOG.error(msg);
          rc = 1;
        }
      }
    }
    return rc;
  }

  private void usage(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("docker-test", opts);
  }

  private void buildDockerImage(String dir, String repo, String branch, int buildNumber)
      throws IOException {
    DockerBuilder.createDockerFile(dir, repo, branch, buildNumber);
    docker.buildImage(dir, 30, TimeUnit.MINUTES);
  }

  private void runContainers(DTestLogger logger, int numContainers, PrintStream out)
      throws IOException {
    List<ContainerCommand> taskCmds = commandFactory.getContainerCommands("/root/hive");

    final ResultAnalyzer analyzer = analyzerFactory.getAnalyzer();
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(taskCmds.size());
    ExecutorService executor = Executors.newFixedThreadPool(numContainers);
    for (ContainerCommand taskCmd : taskCmds) {
      tasks.add(executor.submit(() -> {
        ContainerResult result = docker.runContainer(3, TimeUnit.HOURS, taskCmd);
        analyzer.analyzeLog(result);
        StringBuilder statusMsg = new StringBuilder("Task ")
            .append(result.name)
            .append(' ');
        if (analyzer.hadTimeouts()) {
          statusMsg.append(" had TIMEOUTS");
        } else if (analyzer.runSucceeded()) {
          statusMsg.append(" SUCCEEDED (does not mean all tests passed)");
        } else {
          statusMsg.append(" FAILED to run tom completion");
        }
        new DTestLogger().write(result.name, statusMsg.toString());
        return 1;
      }));
    }

    boolean runSucceeded = true;
    for (Future<Integer> task : tasks) {
      try {
        task.get();
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for containers to finish, assuming I was" +
            " told to quit.", e);
        runSucceeded = false;
      } catch (ExecutionException e) {
        LOG.error("Got an exception while running container, that's generally bad", e);
        runSucceeded = false;
      }
    }

    runSucceeded &= analyzer.runSucceeded();

    executor.shutdown();
    if (analyzer.getErrors().size() > 0) {
      logger.write(SUMMARY_LOG, "All Errors:");
      for (String error : analyzer.getErrors()) {
        logger.write(SUMMARY_LOG, error);
      }
    }
    if (analyzer.getFailed().size() > 0) {
      logger.write(SUMMARY_LOG, "All Failures:");
      for (String failure : analyzer.getFailed()) {
        logger.write(SUMMARY_LOG, failure);
      }
    }
    StringBuilder msg = new StringBuilder("Test run ");
    if (!runSucceeded) msg.append("FAILED.  Following numbers are probably meaningless.\n");
    else if (analyzer.hadTimeouts()) msg.append("HAD TIMEOUTS.  Following numbers are incomplete.\n");
    else msg.append("RAN ALL TESTS\n");
    msg.append("Final counts: Succeeded: ")
        .append(analyzer.getSucceeded())
        .append(", Errors: ")
        .append(analyzer.getErrors().size())
        .append(", Failures: ")
        .append(analyzer.getFailed().size());
    logger.write(SUMMARY_LOG, msg.toString());
    out.println(msg.toString());
  }

  /**
   * This calls System.exit, don't call it if you're using a tool.  Use
   * {@link #run(String[], PrintStream, PrintStream)} instead.
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    DockerTest test = new DockerTest();
    System.exit(test.run(args, System.out, System.err));
  }
}
