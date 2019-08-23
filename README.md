# Docker Test
## Overview
DockerTest (aka DTest) is a tool that allows users to run their tests in containers. 
The intended
users are large projects  with many tests that take more than a few minutes to run.
Using this tool a build machine can compile the project once and then run tests in
containers in parallel.  
 
DTest uses Git, Maven, Docker, and Jenkins.  To build the user provides a configuration
profile (more about this later) a git repo and a branch.  The provided git repo is cloned,
the indicated branch checked
out, and then the code is build and a docker image created.  This results in a 
large docker image (currently several G in the case of Apache Hive) but has the 
benefit that the build is only done once.  Since the build is done in a new
container all maven dependencies are pulled fresh for the build.  Then, as indicated by
the configuration in the profile a set of tests are run in separate containers in 
parallel.  The number of simultaneous containers is controlled by the installation,
with the intention of having one per core on the machine.  When
all the tests have finished an HTML report is generated with the status.  If any tests
failed logs from those tests are included in the report.

The docker image created by DTest is labled using the name of the branch.  The image
is constructed in such a way that subsequent builds will generate new images rather than
pull from the cache.

The simplest configuration is  to have one test execution container per directory.
Directories that take significantly longer than others can be split into multiple
containers.  Tests that need to be run alone can be isolated in their own containers.
Bad tests can be skipped.

## Building with Jenkins
Go to your Jenkins server and click on **Build with Parameters**.  There are three parameters:
* **Profile**: Profiles will be specific to your project, but often reflect major branches of
your project, for example `master` or `version-3`.
* **Branch**:  Branch in your repository to build from.  This will often be the branch you will
be submitting a pull request from, for example `bug1234`.
* **Repository**:  Git repository to clone the code from, it must be public and contain the indicated
branch.

If you do not see the profile you need or your repository is not available in the drop down talk
to the administrator for your Jenkins build.

When the build is finished DTest creates an HTML report.  This report includes
the status of the build, links to the Dockerfile used to create the image and
to the Log4j log created by DTest, and links to logs for any tests that failed
or returned errors.

Five return states are possible.  Other than `Success` the states are layered, so
a run that had test time outs may also have had failures, but the test time outs
will be reflected in the status as that is deemed to be a larger issue. 
 1. Success:  all the tests were run and all passed
 2. Had failures or errors:  all the tests were run, some failed or returned errors
 3. Tests timed out:  all tests were run, but some did not complete in the alloted time.
 3. Containers timed out:  one or more containers did not complete in the allotted time.
 4. Build failed:  DTest failed.  Usually this is caused by the image creation failing,
    usually because `mvn install` failed.
    
## Setting Up Jenkins
Configuration for using DTest on Jenkins to test Apache Hive is included in the 
distribution.  The file includes instructions on how to import the job into Jenkins.
Search for `jenkins-config.xml` in the source tree.  You can modify this to build
your own Jenkins implementation.  You should only need to modify the config directories
and github repos.

To add new profiles or repositories to your build go to the job in Jenkins and click on
**Configure**.  In the **General** section modify the *Profile* and *Repository* parameters to
add the new options you need.  In the case of profile, there must be corresponding 
`dtest.yaml` and `dtest.properties` files in the config directory.

## Configuration
### DTest Configuration
Configuration for DTest wide values are defined in the file `dtest.properties`.  You will
find this file in the config profile directory.

Property | Explanation | Default
---------|-------------|--------
dtest.core.buildinfo.basedir | Working directory for the build on the build machine | *DTEST_HOME*/log/*label*-*buildid*
dtest.core.buildinfo.label |  Label for this build | Branch name
dtest.core.buildyaml.impl | Class to use to interpret `dtest.yaml`.  It must either be or subclass `BuildYaml`|  `BuildYaml`
dtest.core.codesource.impl | Subclass of `CodeSource` to use, which controls how DTest interacts with the source control system.  | `GitSource`
dtest.core.containerclient.impl | Subclass of `ContainerClient` to use, which handles container operations | `DockerContainerClient`
dtest.core.containerclient.containerruntime | Maximum runtime for a single container.  If any container exceeds this value the build will be marked as timed out.  You should set this higher than `dtest.core.containercommand.singletestruntime` so that you can distinguish between a container timing out and a test timing out.|  30 minutes  
dtest.core.containerclient.imagebuildtime | Maximum runtime for the initial compilation and image build.  This should be set to a long enough time to download the base image, install any additional packages, and run a compile with no tests.  If the image fails to build in this amount of time the build will be marked as timed out. | 30 minutes  
dtest.core.containercommand.singtestruntime | Timeout for a single test.  If a test exceeds this amount of time the test will be marked as timed out and the build as having timeouts.  | 5 minutes
dtest.core.containercommandfactory.impl | Subclass of `ContainerCommandFactory` to use to build container commands | `MavenContainerCommandFactory`
dtest.core.containercommandfactory.testspercontainer | When splitting tests, tests to run per container.  This will only be used in directories where tests are split.  Setting this higher cuts down on setup and teardown time for tests, but extends the runtime of each container and can require more memory, disk, etc. per container.  | 10
dtest.core.dockertest.numcontainers | Number of containers to run, should be 1 per core | 2
dtest.core.resultanalyzer.impl | Subclass of `ResultAnalyzer` to use to analyze the output of the container commands and interpret test success, error, failure, or timeout |  `MavenResultAnalyzer`
dtest.docker.dockercontainerclient.dockerpath | Path to the docker executable | `/usr/local/bin/docker`

### Profile Configuration
Each profile is controlled by a `dtest.yaml` file.  This file controls what code is 
checked out, what tests are run, and how those tests are split up.

#### Entries
The entries are presented here, with nesting in the list representing nesting in `dtest.yaml`.  There are
also multiple examples in the source code, with Hive having some very complex `dtest.yaml` files.
* `baseImage`: Base docker image to use.  Currently supported values are `centos`, `ubuntu`, or `debian`. 
These can also include a version number if desired.  **Required**
* `requiredPackages`: A list of required packages that should be installed for your build to work. 
These must match package names fetchable by the package manager for the `baseImage` you have chosen.
* `projectName`: Name of the project.  When using git, this needs to match the directory name of your
project when the project is cloned. **Required**
* `projectDir`: Directory in the project to run `mvn install` in when building the image. 
Defaults to `projectName`.
* `repo`: Default repository to use for source control.  This is overridden by values passed on
the command line.
* `branch`: Default branch to use with source control.  This is overridden by values passed on
the command line.  The default for this value is source control specific, for git it is `master`.
* `javaPackages`: A list of top level Java packages that the tests are in.  These are not the
individual modules but top level ones, such as `org.dtest`.
* `additionalLogs`: A list of any additional log files that should be picked up as part of th
collection of log files to ship back to the user.  By default the system picks up output from
the `surefire-reports` directory.  If your system uses log4j or a similar package and you want to
fetch the resulting logs you should place that log in this list as DTest cannot determine how the
logging is configured and where the logfile is.
* `comment:` Free form, all for you to comment as you please.
* `dirs`: A list of test groups to run.  Required for each directory.  DTest will not assume every
(or any ) directories in your build have tests that need run.
  * `dir`: Directory to run tests in.  With no other values all tests in this directory will be
  run in a single command, `mvn test`.
    * `needsSplit`: Whether to run all tests in this directory in a single container or split
    them up.  Default is to run them all in the same container.  You should split them up if
    running all of them in a single container will exceed the container timeout or require too
    many resources.
    * `singleTest`: If set, then only run one test in this container.  This differs from
    `isolatedTests` in that all other tests in this directory will be ignored.
    * `testsPerContainer`: If `needsSplit` is set, how many tests to run per container. 
    Defaults to `dtest.core.containercommandfactory.testspercontainer`.
    * `isolatedTests`:  List of tests that to run in their own container.  Some tests take
    a lot of resources, take a long time, or don't play well with others.  `needsSplit` should
    be set to true if this value is set.
    * `skippedTests`:  List of tests to skip.
    * `env`: Map of environment variables to set when running the tests.
    * `properties`: Map of Java properties to set when running the tests.

Example `dtest.yaml`
```
baseImage: centos
requiredPackages:
  - java-1.8.0-openjdk-devel
projectName: myproject
javaPackages:
  - org.myproject
dirs:
  - dir: core
  - dir: apps
    skippedTests:
      - TestThatDoesNotWork
    needsSplit: true
```

## Hive Specific Usage
DTest was initially built to use with Apache Hive.  The core is built and tested independent of 
Hive.  Hive extends a number of the existing classes as its build has some non-standard features.
In particular the Hive qfile tests require special handling, both in launching the tests
and evaluating success or failure.  Hive adds the following additional entries to the `dir` structure
in `dtest.yaml`.  All of these are intended for use with qfiles, and assume that the directory has
been configured to run only a `singleTest`, the driver for the qfile tests (e.g. `TestCliDriver`).
* `qfilesDir`:  Directory containing qfiles to run.  If this is set all qfiles in this directory will
be run (but see `excludedQFilesProperties` and `excludedQFiles` below for caveats).
* `qfiles`:  List of qfiles to test with.  If set, only this list of qfiles will be tested.
* `includedQFilesProperties`:  List of properties in the Hive file `testconfiguration.properties`\*
to read to determine which qfiles to run.  All qfiles indicates in these properties will be tested, 
accept those excluded via `excludedQFiles`.
* `excludedQFiles`:  List of qfiles to exclude from this test.  This will override qfiles found in 
`qfilesDir` or indicated in `includedQFilesProperty`
* `excludedQFilesProperties`:  List of properties in the Hive file `testconfiguration.properties`\*
to read to determine which qfiles not to run.  This is used in conjunction with `qfilesDir` to
exclude certain qfiles.
* `isolatedQFiles`: List of qfiles that do not play well with others or take a long time and need
to be run in a separate container.

\*Located in `itests/src/test/resources/`

## Command Line Usage
You can also use DTest directly from the command line.
```
dtest -c <confdir> [-b <branch>] [-i <id>] [-n] [-r <repo>]
-b --branch <branch>     Branch in the source control to use when building.
                         If set this overrides the value of branch in
                         dtest.yaml.  If no value is specified on the command
                         line or in dtest.yaml, then a default that makes sense
                         for the source control system in use will be used
                         (e.g., master for git).
-c --conf-dir <confdir>  The configuration directory that contains the
                         dtest.properties and dtest.yaml files for this build.
                         This is required.
-Dkey=value              Standard Java properties.  These will override
                         values set in dtest.properties.
-i --build-id <id>       The identifier for this build.  This will be used in
                         filenames.  If not set defaults to current datetime.
-n --no-cleanup          Do not cleanup images and containers after the build.
                         Usually you want to cleanup to avoid polluting the
                         build machine.  This is useful for debugging.
-r --repo <repo>         Source repository from which the code will be checked
                         out.  If set this overrides the value of repo in
                         dtest.yaml.  This must be set in one of those places.
```
## Extending DTest
You can extend DTest for your project.  For projects that run standard maven builds and use 
standard JUnit, no extension is necessary.  

### Implementing an Extension
DTest includes a number of classes and interfaces that control how containers are run,
tests executed, and results analyzed.  Which instances of these classes are used is
controlled by configuration so that you can easily add your own classes and use
them without changes to DTest core.

`BuildYaml`: Controls how `dtest.yaml` is interpreted.  Defaults to `BuildYaml`.  You will need
to extend this if you have implemented other classes that require additional information beyond
what is supported in `BuildYaml`.

`CodeSource`: Controls interactions with the source control system.  Defaults to `GitSource`.
You will only need to extend this if you want to use a version control system other than git.
If it is a standard VCS (e.g. SVN) please consider contributing it back.

`ContainerClient`: Creates the container image and handles spinning up and down 
container instances, as well as fetching logs from them.  Defaults to `DockerContainerClient`.
You will need to extend this if your build requires special container handling.

`ContainerCommand`: Generates commands to be run inside the container.  Defaults to 
`MavenContainerCommand`.  These are the shell commands run in the container (like 
`mvn install`).  You will need to extend this for other build systems or if your 
project requires special handling in maven.  If you implement it for other standard
build systems such as groovy or ant please consider contributing it back.

`ContainerCommandFactory`:  Builds the list of `ContainerCommand`s.  Defaults to 
`MavenContainerCommandFactory`.  If you need to extend `ContainerCommand`, you will 
need to extend this one too.

`ResultAnalyzer`: Analyzes the output of tests and decides whether the test succeeded, failed,
threw an error, or timed out.  Defaults to `MavenResultAnalyzer`, which really should be 
named MavenJUnitResultAnalyzer.  You will need to extend this if you are building
with something other than maven, using a unit test runner other than JUnit, or you JUnit
tests require special interpretation to decide whether they succeeded or failed (e.g. Hive's 
qfile tests).  If you implement it for other standard build or unit tests systems (e.g.
groovy or TestNG) please consider contributing it back.

### Extending Configuration
If you are extending existing classes in DTest and need to add your own configuration you
can do so easily. DTest stores its configuration as Java properties.  The `Config` object
contains a reference to the properties and provides helper methods to fetch these properties
as the desired type (`String`, `int`, time (`long` milliseconds since the epoch), `Class`). 
Classes define their own configuration
keys.  The convention is to name the key `dtest.`*package.classname.configval* and define a
`public static final String` variable in your class `CFG_`*CLASSNAME_CONFIGVAL*.  For example,
the class `ResultAnalyzer` defines the configuration key `dtest.core.resultanalyzer.impl`
that is uses to determine the subclass of `ResultAnalyzer` to instantiate.  It defines
the public final String `CFG_RESULTANALYZER_IMPL`. The intent is that only classes that
define configuration keys use those keys.
