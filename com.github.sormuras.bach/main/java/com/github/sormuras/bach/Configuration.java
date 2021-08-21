package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record Configuration(
    boolean verbose,
    boolean lenient,
    int timeout,
    Pathing pathing,
    Printing printing,
    Tooling tooling,
    Options.ProjectOptions projectOptions) {

  public static final String //
      EXTERNAL_MODULES_DIRECTORY = ".bach/external-modules",
      EXTERNAL_TOOL_LAYERS_DIRECTORY = ".bach/external-tool-layers",
      EXTERNAL_TOOL_PROGRAMS_DIRECTORY = ".bach/external-tool-programs",
      EXTERNAL_TOOL_PROGRAM_ARGSFILE = "java.args",
      LOGBOOK_ARCHIVE_FILE = "logbooks/logbook-{TIMESTAMP}.md",
      LOGBOOK_MARKDOWN_FILE = "logbook.md",
      TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss",
      WORKSPACE_DIRECTORY = ".bach/workspace";

  public static Path computeJavaExecutablePath(String name) {
    var windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
    return Path.of(System.getProperty("java.home"), "bin", name + (windows ? ".exe" : ""));
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

  public static record Pathing(
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

    public static List<Path> findJavaFiles(Path root) {
      return PathSupport.find(root, 99, PathSupport::isJavaFile);
    }

    public static List<Path> findModuleInfoJavaFiles(Path root) {
      return PathSupport.find(root, 99, PathSupport::isModuleInfoJavaFile);
    }

    public Path root(String first, String... more) {
      return root.resolve(Path.of(first, more));
    }

    public Path externalModule(String jar) {
      return externalModules.resolve(Path.of(jar));
    }

    public Path externalToolLayer(String name, String... more) {
      return externalToolLayers.resolve(Path.of(name, more));
    }

    public Path externalToolProgram(String name, String... more) {
      return externalToolPrograms.resolve(Path.of(name, more));
    }

    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }
  }

  public record Printing(PrintWriter out, PrintWriter err) {}

  public record Tooling(ToolFinder finder) {}

  public Configuration with(Options options) {
    return new Configuration(
        options.forConfiguration().verbose().orElse(verbose),
        options.forConfiguration().verbose().orElse(lenient),
        options.forConfiguration().timeout().orElse(timeout),
        pathing,
        printing,
        tooling,
        options.forProject());
  }
}
