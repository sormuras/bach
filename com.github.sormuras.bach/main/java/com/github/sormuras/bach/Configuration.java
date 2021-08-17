package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record Configuration(
    boolean verbose,
    boolean lenient,
    int timeout,
    Pathing pathing,
    Printing printing,
    Tooling tooling,
    Options.ProjectOptions projectOptions) {

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
                ToolFinder.ofProviders(pathing.externalToolProviders()),
                ToolFinder.ofPrograms(pathing.externalToolPrograms(), pathing.javaExecutable())));
    return new Configuration(
        false,
        false,
        9,
        pathing,
        printing,
        tooling,
        new Options.ProjectOptions(Optional.empty(), Optional.empty()));
  }

  static final String TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss";

  static final String LOGBOOK_MARKDOWN_FILE = "logbook.md";

  static final String LOGBOOK_ARCHIVE_FILE = "logbooks/logbook-{TIMESTAMP}.md";

  static final String EXTERNAL_MODULES_DIRECTORY = ".bach/external-modules";

  static final String EXTERNAL_TOOL_PROGRAMS_DIRECTORY = ".bach/external-tool-programs";

  static final String EXTERNAL_TOOL_PROVIDERS_DIRECTORY =".bach/external-tool-providers";

  static final String WORKSPACE_DIRECTORY = ".bach/workspace";

  public static record Pathing(
      Path root,
      Path externalModules,
      Path externalToolPrograms,
      Path externalToolProviders,
      Path workspace,
      Path javaExecutable) {

    public static Pathing of(Path root) {
      return new Pathing(
          root,
          root.resolve(EXTERNAL_MODULES_DIRECTORY),
          root.resolve(EXTERNAL_TOOL_PROGRAMS_DIRECTORY),
          root.resolve(EXTERNAL_TOOL_PROVIDERS_DIRECTORY),
          root.resolve(WORKSPACE_DIRECTORY),
          Path.of(System.getProperty("java.home"), "bin", "java"));
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

    public Path externalToolProgram(String name, String... more) {
      return externalToolPrograms.resolve(Path.of(name, more));
    }

    public Path externalToolProvider(String name, String... more) {
      return externalToolProviders.resolve(Path.of(name, more));
    }

    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }
  }

  public record Printing(PrintWriter out, PrintWriter err) {}

  public record Tooling(ToolFinder finder) {}

  public Configuration with(Options options) {
    return new Configuration(
        options.configurationOptions().verbose().orElse(verbose),
        options.configurationOptions().verbose().orElse(lenient),
        options.configurationOptions().timeout().orElse(timeout),
        pathing,
        printing,
        tooling,
        options.projectOptions());
  }
}
