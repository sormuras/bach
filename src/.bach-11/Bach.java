/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
public class Bach {

  /** Version of the Java Shell Builder. */
  private static final Version VERSION = Version.parse("11.0-ea");

  /** Default logger instance. */
  private static final Logger LOGGER = System.getLogger("Bach.java");

  /** Bach.java's main program entry-point. */
  public static void main(String... args) {
    var bach = new Bach();
    var main = bach.new Main(args);
    var code = main.call();
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  /** Create new project model builder instance for the given name. */
  public static Project.Builder newProject(String name) {
    return new Project.Builder(name);
  }

  /** Logger instance. */
  private final Logger logger;

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Well-known paths. */
  private final Folder folder;

  /** Initialize this instance with default values. */
  public Bach() {
    this(LOGGER, System.out::println, Folder.ofSystem());
  }

  /** Initialize this instance with the specified arguments. */
  public Bach(Logger logger, Consumer<String> printer, Folder folder) {
    this.logger = logger;
    this.printer = printer;
    this.folder = folder;
    logger.log(Level.TRACE, "Initialized Bach.java " + VERSION);
  }

  /** Bach.java's main program class. */
  private class Main implements Callable<Integer> {

    private final Deque<String> operations;

    /** Initialize this instance with the given command line arguments. */
    private Main(String... arguments) {
      this.operations = new ArrayDeque<>(List.of(arguments));
    }

    @Override
    public Integer call() {
      logger.log(Level.DEBUG, "Call main operation(s): " + operations);
      if (operations.isEmpty()) return 0;
      var operation = operations.removeFirst();
      switch (operation) {
        case "help":
          return help();
        case "version":
          return version();
        default:
          throw new UnsupportedOperationException(operation);
      }
    }

    /** Print help screen. */
    public int help() {
      printer.accept("Bach.java " + VERSION + " running on Java " + Runtime.version());
      printer.accept("F1 F1 F1");
      return 0;
    }

    /** Print version. */
    public int version() {
      printer.accept("" + VERSION);
      return 0;
    }
  }

  /** Project model. */
  public static final class Project {

    /** Project descriptor. */
    private final ModuleDescriptor descriptor;

    /** Initialize this project model. */
    public Project(ModuleDescriptor descriptor) {
      this.descriptor = descriptor;
    }

    /** Project model descriptor. */
    public ModuleDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public String toString() {
      return "Project{" + "descriptor=" + descriptor + '}';
    }

    /** Project model builder. */
    public static class Builder {

      /** Project model descriptor builder. */
      private final ModuleDescriptor.Builder descriptor;

      /** Initialize this project model builder with the given name. */
      Builder(String name) {
        var synthetic = Set.of(ModuleDescriptor.Modifier.SYNTHETIC);
        this.descriptor = ModuleDescriptor.newModule(name, synthetic);
      }

      /** Create new project model instance based on this builder's components. */
      public Project build() {
        return new Project(descriptor.build());
      }

      /** Declare a dependence on the specified module and version. */
      public Builder requires(String module, String version) {
        var synthetic = Set.of(ModuleDescriptor.Requires.Modifier.SYNTHETIC);
        descriptor.requires(synthetic, module, Version.parse(version));
        return this;
      }

      /** Set the version of the project. */
      public Builder version(String version) {
        descriptor.version(version);
        return this;
      }
    }
  }

  /** Target paths. */
  public static final class Folder {

    public static Folder of(Path base) {
      return new Folder(base.resolve(".bach"));
    }

    public static Folder ofSystem() {
      return of(Path.of(""));
    }

    private final Path out;

    public Folder(Path out) {
      this.out = out;
    }
  }

  /** Build summary. */
  public static final class Summary {
    private final Project project;

    public Summary(Project project) {
      this.project = project;
    }

    public List<String> toMarkdown() {
      var md = new ArrayList<String>();
      md.add("# Summary");
      md.add("");
      md.add("## Project");
      md.add("`" + project + "`");
      md.add("");
      md.add("## System Properties");
      System.getProperties().stringPropertyNames().stream()
          .sorted()
          .forEach(key -> md.add(String.format("- `%s`: `%s`", key, value(key))));
      return md;
    }

    static String value(String systemPropertyKey) {
      var value = System.getProperty(systemPropertyKey);
      if (!"line.separator".equals(systemPropertyKey)) return value;
      var build = new StringBuilder();
      for (char c : value.toCharArray()) {
        build.append("0x").append(Integer.toHexString(c).toUpperCase());
      }
      return build.toString();
    }

    public Path write(Path directory) {
      var markdown = toMarkdown();
      try {
        return Files.write(Files.createDirectories(directory).resolve("summary.md"), markdown);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
