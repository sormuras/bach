package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.PathSupport;
import com.github.sormuras.bach.internal.VersionSupport;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Global settings with nested topic-specific configurations. */
public record Configuration(
    boolean verbose,
    boolean lenient,
    int timeout,
    Pathing pathing,
    Printing printing,
    Tooling tooling,
    Options.ProjectOptions projectOptions) {

  public static final String //
      DEFAULT_PROJECT_NAME = "unnamed",
      DEFAULT_PROJECT_VERSION = "0-ea",
      EXTERNAL_MODULES_DIRECTORY = ".bach/external-modules",
      EXTERNAL_TOOL_LAYERS_DIRECTORY = ".bach/external-tool-layers",
      EXTERNAL_TOOL_PROGRAMS_DIRECTORY = ".bach/external-tool-programs",
      EXTERNAL_TOOL_PROGRAM_ARGSFILE = "java.args",
      LOGBOOK_ARCHIVE_FILE = "logbooks/logbook-{TIMESTAMP}.md",
      LOGBOOK_MARKDOWN_FILE = "logbook.md",
      TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss",
      WORKSPACE_DIRECTORY = ".bach/workspace";

  public static String computeDefaultProjectName() {
    return Configuration.computeDefaultProjectName(Path.of(""));
  }

  public static String computeDefaultProjectName(Path directory) {
    return PathSupport.nameOrElse(directory, DEFAULT_PROJECT_NAME);
  }

  public static Version computeDefaultProjectVersion() {
    return Version.parse(DEFAULT_PROJECT_VERSION);
  }

  public static Path computeJavaExecutablePath(String name) {
    var windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
    return Path.of(System.getProperty("java.home"), "bin", name + (windows ? ".exe" : ""));
  }

  public static String computeJarFileName(String module, Version version) {
    return module + "@" + VersionSupport.toNumberAndPreRelease(version) + ".jar";
  }

  public static Configuration of() {
    var pathing = Pathing.ofCurrentWorkingDirectory();
    var printing =
        new Printing(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    return Configuration.of(pathing, printing);
  }

  public static Configuration of(Pathing pathing, Printing printing) {
    var tooling =
        new Tooling(
            ToolFinder.compose(
                ToolFinder.ofSystem(),
                ToolFinder.ofBach(),
                ToolFinder.ofLayers(pathing.externalToolLayers()),
                ToolFinder.ofPrograms(
                    pathing.externalToolPrograms(),
                    pathing.javaExecutable(),
                    EXTERNAL_TOOL_PROGRAM_ARGSFILE)));
    return new Configuration(
        false,
        false,
        9,
        pathing,
        printing,
        tooling,
        new Options.ProjectOptions(Optional.empty(), Optional.empty()));
  }

  /** {@link Path}-related settings. */
  public record Pathing(
      Path root,
      Path externalModules,
      Path externalToolLayers,
      Path externalToolPrograms,
      Path workspace,
      Path javaExecutable) {

    public static Pathing of(Path root) {
      return new Pathing(
          root,
          root.resolve(EXTERNAL_MODULES_DIRECTORY),
          root.resolve(EXTERNAL_TOOL_LAYERS_DIRECTORY),
          root.resolve(EXTERNAL_TOOL_PROGRAMS_DIRECTORY),
          root.resolve(WORKSPACE_DIRECTORY),
          computeJavaExecutablePath("java"));
    }

    public static Pathing ofCurrentWorkingDirectory() {
      return Pathing.of(Path.of(""));
    }

    public Path root(String first, String... more) {
      return root.resolve(Path.of(first, more));
    }

    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }
  }

  /** Print-related settings and common {@link PrintWriter} objects. */
  public record Printing(PrintWriter out, PrintWriter err) {
    public static Printing ofErrorsOnly() {
      return new Printing(new PrintWriter(Writer.nullWriter()), new PrintWriter(System.err, true));
    }
  }

  /** {@link ToolFinder}-related and {@link java.util.spi.ToolProvider}-related settings. */
  public record Tooling(ToolFinder finder) {}

  public Configuration with(Options options) {
    return new Configuration(
        options.forConfiguration().verbose().orElse(verbose),
        options.forConfiguration().lenient().orElse(lenient),
        options.forConfiguration().timeout().orElse(timeout),
        pathing,
        printing,
        tooling,
        options.forProject());
  }
}
