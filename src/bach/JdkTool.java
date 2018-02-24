/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * You can use the foundation JDK tools and commands to create and build applications.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/9/tools/main-tools-create-and-build-applications.htm">Main
 *     Tools to Create and Build Applications</a>
 */
interface JdkTool {
  /**
   * You can use the javac tool and its options to read Java class and interface definitions and
   * compile them into bytecode and class files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javac.htm">javac</a>
   */
  class Javac implements JdkTool {
    /** (Legacy) class path. */
    List<Path> classPath = List.of();

    /** (Legacy) locations where to find Java source files. */
    @Command.Option("--source-path")
    transient List<Path> classSourcePath = List.of();

    /** Generates all debugging information, including local variables. */
    @Command.Option("-g")
    boolean generateAllDebuggingInformation = false;

    /** Output source locations where deprecated APIs are used. */
    boolean deprecation = true;

    /** The destination directory for class files. */
    @Command.Option("-d")
    Path destination = null;

    /** Specify character encoding used by source files. */
    Charset encoding = StandardCharsets.UTF_8;

    /** Terminate compilation if warnings occur. */
    @Command.Option("-Werror")
    boolean failOnWarnings = true;

    /** Overrides or augments a module with classes and resources in JAR files or directories. */
    Map<String, List<Path>> patchModule = Map.of();

    /** Specify where to find application modules. */
    List<Path> modulePath = List.of();

    /** Where to find input source files for multiple modules. */
    List<Path> moduleSourcePath = List.of();

    /** Specifies root modules to resolve in addition to the initial modules. */
    List<String> addModules = List.of();

    /** Compiles only the specified module and checks timestamps. */
    @Command.Option("--module")
    String module = null;

    /** Generate metadata for reflection on method parameters. */
    boolean parameters = true;

    /** Output messages about what the compiler is doing. */
    boolean verbose = false;

    /** Create javac command with options and source files added. */
    @Override
    public Command toCommand() {
      Predicate<Path> isJavaFile = path -> path.getFileName().toString().endsWith(".java");
      var command = JdkTool.super.toCommand();
      command.mark(10);
      command.addAll(classSourcePath, isJavaFile);
      if (module == null) {
        command.addAll(moduleSourcePath, isJavaFile);
      }
      command.setExecutableSupportsArgumentFile(true);
      return command;
    }
  }

  /**
   * You can use the java command to launch a Java application.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/java.htm">java</a>
   */
  class Java implements JdkTool {
    /**
     * Creates the VM but doesn't execute the main method.
     *
     * <p>This {@code --dry-run} option may be useful for validating the command-line options such
     * as the module system configuration.
     */
    boolean dryRun = false;

    /** The name of the Java Archive (JAR) file to be called. */
    Path jar = null;

    /** Overrides or augments a module with classes and resources in JAR files or directories. */
    Map<String, List<Path>> patchModule = Map.of();

    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** Specifies root modules to resolve in addition to the initial modules. */
    List<String> addModules = List.of();

    /** Initial module to resolve and the name of the main class to execute. */
    @Command.Option("--module")
    String module = null;

    /** Arguments passed to the main entry-point. */
    transient List<Object> args = List.of();

    /** Create java command with options and source files added. */
    @Override
    public Command toCommand() {
      Command command = JdkTool.super.toCommand();
      command.setExecutableSupportsArgumentFile(true);
      command.mark(9);
      command.addAll(args);
      return command;
    }
  }

  /**
   * You use the javadoc tool and options to generate HTML pages of API documentation from Java
   * source files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javadoc.htm">javadoc</a>
   */
  class Javadoc implements JdkTool {
    /** The destination directory for generated files. */
    @Command.Option("-d")
    Path destination = null;

    /** Shuts off messages so that only the warnings and errors appear. */
    boolean quiet = true;
  }

  /**
   * You can use the jar command to create an archive for classes and resources, and manipulate or
   * restore individual classes or resources from an archive.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jar.htm">jar</a>
   */
  class Jar implements JdkTool {
    /** Specify the operation mode for the jar command. */
    @Command.Option("")
    String mode = "--create";

    /** Specifies the archive file name. */
    @Command.Option("--file")
    Path file = Paths.get("out.jar");

    /** Specifies the application entry point for stand-alone applications. */
    String mainClass = null;

    /** Specifies the module version, when creating a modular JAR file. */
    String moduleVersion = null;

    /** Stores without using ZIP compression. */
    boolean noCompress = false;

    /** Sends or prints verbose output to standard output. */
    @Command.Option("--verbose")
    boolean verbose = false;

    /** Changes to the specified directory and includes the files at the end of the command. */
    @Command.Option("-C")
    Path path = null;

    @Override
    public Command toCommand() {
      Command command = JdkTool.super.toCommand();
      if (path != null) {
        command.mark(1);
        command.add(".");
      }
      return command;
    }
  }

  /**
   * You use the jdeps command to launch the Java class dependency analyzer.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jdeps.htm">jdeps</a>
   */
  class Jdeps implements JdkTool {
    /** Specifies where to find class files. */
    List<Path> classpath = List.of();

    /** Recursively traverses all dependencies. */
    boolean recursive = true;

    /** Finds class-level dependencies in JDK internal APIs. */
    boolean jdkInternals = false;

    /** Shows profile or the file containing a package. */
    boolean profile = false;

    /** Restricts analysis to APIs, like deps from the signature of public and protected members. */
    boolean apionly = false;

    /** Prints dependency summary only. */
    boolean summary = false;

    /** Prints all class-level dependencies. */
    boolean verbose = false;
  }

  /**
   * You can use the jlink tool to assemble and optimize a set of modules and their dependencies
   * into a custom runtime image.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jlink.htm">jlink</a>
   */
  class Jlink implements JdkTool {
    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** The directory that contains the resulting runtime image. */
    @Command.Option("--output")
    Path output = null;
  }

  /** Name of this tool, like {@code javac} or {@code jar}. */
  default String name() {
    return getClass().getSimpleName().toLowerCase();
  }

  /**
   * Execute this tool with all options and arguments applied.
   *
   * @throws AssertionError if the execution result is not zero
   */
  default void run() {
    toCommand().run();
  }

  /** Create command instance based on this tool's options. */
  default Command toCommand() {
    return new Command(name()).addAllOptions(this);
  }
}
