package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.tool.AnyCall;
import com.github.sormuras.bach.internal.CommandLineParser;
import com.github.sormuras.bach.internal.Records;
import com.github.sormuras.bach.internal.Records.Name;
import java.lang.System.Logger.Level;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @param id an internal identifier for this options instance
 * @param verbose output messages about what Bach is doing
 * @param workflows a list of workflows to execute
 */
public record Options(
    // <editor-fold desc="Internal Options">
    @Name("--id") //
        Optional<String> id,
    // </editor-fold>

    // <editor-fold desc="Runtime Modifying Options">
    @Name("--verbose")
        @Help("""
        --verbose
          Output messages about what Bach is doing.""")
        boolean verbose,
    @Name("--dry-run")
        @Help("""
        --dry-run
          Prevent command execution.
        """)
        boolean dryRun,
    @Name("--run-commands-sequentially")
        @Help(
            """
        --run-commands-sequentially
          Prevent parallel execution of commands.
        """)
        boolean runCommandsSequentially,
    // </editor-fold>

    // <editor-fold desc="Run'N Exit Command Options">
    @Name("--version")
        @Help("""
        --version
          Print version information and exit.""")
        boolean version,
    @Name("--help") //
        @Help("""
        --help
          Print this help message and exit.""")
        boolean help,
    @Name("--help-extra")
        @Help("""
        --help-extra
          Print help on extra options and exit.""")
        boolean helpExtra,
    @Name("--list-configuration")
        @Help(
            """
        --list-configuration
          Print effective configuration and exit.""")
        boolean listConfiguration,
    @Name("--list-modules") //
        @Help("""
        --list-modules
          List modules and exit.""")
        boolean listModules,
    @Name("--list-tools") //
        @Help("""
        --list-tools
          List tools and exit.""")
        boolean listTools,
    @Name("--describe-tool")
        @Help("""
        --describe-tool TOOL
          Describe tool and exit.""")
        Optional<String> describeTool,
    @Name("--load-external-module")
        @Help("""
        --load-external-module MODULE
          Load an external module.""")
        Optional<String> loadExternalModule,
    @Name("--load-missing-external-modules")
        @Help(
            """
        --load-missing-external-modules
          Load all missing external modules.""")
        boolean loadMissingExternalModules,
    @Name("--tool")
        @Help(
            """
        --tool NAME [ARGS...]
          Run the specified tool and exit with its return value.""")
        Optional<AnyCall> tool,
    // </editor-fold>

    // <editor-fold desc="Primordal Options">
    @Name("--chroot")
        @Extra
        @Help(
            """
                --chroot PATH
                  Change virtually into the specified directory by resolving all generated
                  paths against the given PATH before passing them as arguments to tools.
                """)
        Optional<Path> chroot,
    @Name("--bach-info")
        @Extra
        @Help(
            """
                --bach-info MODULE
                  Defaults to: bach.info
                """)
        Optional<String> bachInfo,
    // </editor-fold>

    // <editor-fold desc="Project Options">
    @Name("--project-name") //
        @Help("""
          --project-name NAME
          """)
        Optional<String> projectName,
    @Name("--project-version") //
        @Help("""
          --project-version VERSION
          """)
        Optional<Version> projectVersion,
    @Name("--project-requires")
        @Help(
            """
          --project-requires MODULE
            This option is repeatable.
          """)
        List<String> projectRequires,
    // </editor-fold>

    // <editor-fold desc="Main Code Space Options">
    @Name("--main-module-pattern")
        @Help(
            """
          --main-module-pattern NAME
            Specify where to find module-info.java files for the main code space.
            This option is repeatable.
          """)
        List<String> mainModulePatterns,
    @Name("--main-module-path")
        @Help(
            """
          --main-module-path PATH
            Specify where to find modules for compiling main modules.
            This option is repeatable.
          """)
        List<String> mainModulePaths,
    @Name("--main-java-release")
        @Help(
            """
          --main-java-release RELEASE
            Compile main modules for the specified Java SE release.
          """)
        Optional<Integer> mainJavaRelease,
    @Name("--main-jar-with-sources")
        @Help(
            """
        --main-jar-with-sources
          Include all files found in source folders into their modular JAR files.
        """)
        boolean mainJarWithSources,
    // </editor-fold>

    // <editor-fold desc="Test Code Space Options">
    @Name("--test-module-pattern")
        @Help(
            """
          --test-module-pattern NAME
            Specify where to find module-info.java files for the test code space.
            This option is repeatable.
          """)
        List<String> testModulePatterns,
    @Name("--test-module-path")
        @Help(
            """
          --test-module-path PATH
            Specify where to find modules for compiling and running test modules.
            This option is repeatable.
          """)
        List<String> testModulePaths,
    // </editor-fold>

    // <editor-fold desc="Tools & Tweaks">
    @Name("--limit-tools") //
        @Help("""
        --limit-tools TOOL(,TOOL)*
        """)
        Optional<String> limitTools,
    @Name("--skip-tools") //
        @Help("""
        --skip-tools TOOL(,TOOL)*
        """)
        Optional<String> skipTools,
    @Name("--tweak")
        @Help(
            """
          --tweak SPACE(,SPACE)* TRIGGER COUNT ARG [ARGS...]
            Additional command-line arguments passed to tool runs matching the specified trigger.
            Examples:
              --tweak main javac 1 -parameters
              --tweak main javac 2 -encoding UTF-8
            This option is repeatable.
          """)
        List<Tweak> tweaks,
    // </editor-fold>

    // <editor-fold desc="External Modules and Libraries">
    @Name("--external-module-location")
        @Help(
            """
          --external-module-location MODULE LOCATION
            Specify an external module-location mapping.
            Example:
              --external-module-location
                org.junit.jupiter
                org.junit.jupiter:junit-jupiter:5.7.1
            This option is repeatable.
          """)
        List<ExternalModuleLocation> externalModuleLocations,
    @Name("--external-library-version")
        @Help(
            """
          --external-library-version NAME VERSION
            An external library name and version, providing a set of module-location mappings.
            Example:
              --external-library-version
                JUnit
                5.7.1
            This option is repeatable.
          """)
        List<ExternalLibraryVersion> externalLibraryVersions,
    // </editor-fold>

    // <editor-fold desc="Workflows">
    @Name("--workflow")
        @Help(
            """
        --workflow WORKFLOW
          Execute a workflow specified by its name.
          This option is repeatable.""")
        List<Workflow> workflows
    // </editor-fold>
    ) implements Wither<Options> {

  public static String generateHelpMessage(Predicate<RecordComponent> filter) {
    var options =
        Stream.of(Options.class.getRecordComponents())
            .filter(filter)
            .sorted(Comparator.comparing(RecordComponent::getName))
            .map(component -> component.getAnnotation(Help.class).value().strip())
            .collect(Collectors.joining(System.lineSeparator()));

    return """
        Usage: bach [OPTIONS] [WORKFLOW...]
                 to execute one or more workflows in sequence

        Workflows may either be passed via their --workflow <WORKFLOW> option at a random
        position or by their <WORKFLOW> name at the end of the command-line arguments.

        OPTIONS include:

        {{OPTIONS}}
        """
        .replace("{{OPTIONS}}", options.indent(2).stripTrailing())
        .stripTrailing();
  }

  public static Options of() {
    return EMPTY;
  }

  public static Options ofCommandLineArguments(String... args) {
    return Options.ofCommandLineArguments(List.of(args));
  }

  public static Options ofCommandLineArguments(List<String> arguments) {
    if (arguments.isEmpty()) return Options.of();
    return new CommandLineParser(arguments).ofCommandLineArguments(arguments);
  }

  public static Options ofFile(Path file) {
    var id = "file options (" + file + ")";
    if (Files.notExists(file)) return Options.of().id(id);
    try {
      var lines = Files.readAllLines(file);
      return Options.ofCommandLineArguments(lines).id(id);
    } catch (Exception exception) {
      throw new BachException("Read all lines failed for: " + file, exception);
    }
  }

  public static Options ofProjectInfo(ProjectInfo info) {
    var options = Options.of().id("@ProjectInfo Options");
    // project
    options = options.with("projectName", Optional.of(info.name()));
    options = options.with("projectVersion", Optional.of(Version.parse(info.version())));
    options = options.with("projectRequires", List.of(info.requires()));
    // main
    var main = info.main();
    options = options.with("mainJavaRelease", Optional.of(main.javaRelease()));
    options = options.with("mainModulePatterns", List.of(main.modulesPatterns()));
    options = options.with("mainModulePaths", List.of(main.modulePaths()));
    options = options.with("mainJarWithSources", main.jarWithSources());
    // test
    var test = info.test();
    options = options.with("testModulePatterns", List.of(test.modulesPatterns()));
    options = options.with("testModulePaths", List.of(test.modulePaths()));
    // tools & tweaks
    var tools = Tools.of(info);
    if (!tools.limits().isEmpty())
      options = options.with("limitTools", Optional.of(String.join(",", tools.limits())));
    if (!tools.skips().isEmpty())
      options = options.with("skipTools", Optional.of(String.join(",", tools.skips())));
    options = options.with("tweaks", tools.tweaks().list());
    // externals
    var external = info.external();
    options =
        options.with(
            "externalModuleLocations",
            Stream.of(external.modules()).map(ExternalModuleLocation::ofInfo).toList());
    options =
        options.with(
            "externalLibraryVersions",
            Stream.of(external.libraries()).map(ExternalLibraryVersion::ofInfo).toList());
    return options;
  }

  public static Options compose(String id, Logbook logbook, Options... layers) {
    /* DEBUG */ {
      logbook.log(Level.DEBUG, "Compose options from " + layers.length + " layers");
      for (int i = 0; i < layers.length; i++) {
        logbook.log(Level.TRACE, "[" + i + "] = " + layers[i].id().orElse("-"));
      }
      logbook.log(Level.DEBUG, "[layer] option value...");
    }
    var records = Records.of(Options.class);
    return records.compose(
        Options.of().id(id),
        component -> !component.getName().equals("id"),
        value ->
            value instanceof Boolean flag && flag
                || value instanceof Optional<?> optional && optional.isPresent()
                || value instanceof List<?> list && !list.isEmpty(),
        (component, index) -> {
          var name = component.getName();
          var value = records.value(layers[index], component);
          logbook.log(Level.DEBUG, "[%d] %s -> %s".formatted(index, name, value));
        },
        layers);
  }

  // ---

  public Path chrootOrDefault() {
    return chroot.map(Path::normalize).orElse(Path.of("."));
  }

  public String bachInfoOrDefault() {
    return bachInfo.orElse("bach.info");
  }

  // ---

  public Options id(String id) {
    return with("id", Optional.ofNullable(id));
  }

  // ---

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Help {
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Extra {}

  private static final Options EMPTY = Records.of(Options.class).compose(Options::emptyValue);

  private static Object emptyValue(RecordComponent component) {
    if (boolean.class == component.getType()) return false;
    if (Optional.class == component.getType()) return Optional.empty();
    if (List.class == component.getType()) return List.of();
    throw new Error("Unsupported type: " + component.getType());
  }

  public static boolean isHelp(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && !component.isAnnotationPresent(Extra.class);
  }

  public static boolean isHelpExtra(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && component.isAnnotationPresent(Extra.class);
  }
}
