package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.api.Tweak;
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
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @param id an internal identifier for this options instance
 * @param verbose output messages about what Bach is doing
 * @param actions a list of actions to execute
 */
public record Options(
    @Name("--id") Optional<String> id,

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
        Optional<Tool> tool,
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

    // <editor-fold desc="...and ACTION!">
    @Name("--action")
        @Help(
            """
        --action ACTION
          Execute the specified action.
          This option is repeatable.""")
        List<Action> actions
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
        Usage: bach [OPTIONS] [ACTIONS...]
                 to execute one or more actions in sequence

        Actions may either be passed via their --action <ACTION> option at a random
        location or by their <ACTION> name at the end of the command-line arguments.

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
    var options = Options.of();
    if (arguments.isEmpty()) return options;
    var deque = new ArrayDeque<String>();
    arguments.stream().flatMap(String::lines).map(String::strip).forEach(deque::add);
    Supplier<String> pop = () -> deque.removeFirst().strip();
    while (!deque.isEmpty()) {
      var argument = pop.get();
      options =
          switch (argument) {
            case // flags
            "--version", //
            "--help", "--help-extra", //
            "--verbose", //
            "--dry-run", "--run-commands-sequentially", //
            "--list-configuration", "--list-modules", "--list-tools", //
            "--main-jar-with-sources", //
            "--load-missing-external-modules" //
            -> options.with(argument, true);
            case // single-value properties of type String
            "--id", "--bach-info", //
            "--describe-tool", "--load-external-module", //
            "--project-requires", "--project-name", //
            "--limit-tools", "--skip-tools" //
            -> options.with(argument, pop.get());
            case "--chroot" -> options.with("--chroot", Path.of(pop.get()));
            case "--project-version" -> options.with("--project-version", Version.parse(pop.get()));
            case "--main-module-path", "--main-module-paths" -> options.with(
                "--main-module-path", pop.get());
            case "--main-module-pattern", "--main-module-patterns" -> options.with(
                "--main-module-pattern", pop.get());
            case "--main-java-release" -> options.with(
                "--main-java-release", Integer.parseInt(pop.get()));
            case "--test-module-path", "--test-module-paths" -> options.with(
                "--test-module-path", pop.get());
            case "--test-module-pattern", "--test-module-patterns" -> options.with(
                "--test-module-pattern", pop.get());
            case "--tweak", "--tweaks" -> options.with("tweaks", Tweak.ofCommandLine(deque));
            case "--external-module-location", "--external-module-locations" -> options.with(
                "--external-module-location", ExternalModuleLocation.ofCommandLine(pop));
            case "--external-library-version", "--external-library-versions" -> options.with(
                "--external-library-version", ExternalLibraryVersion.ofCommandLine(pop));
            case "--tool" -> {
              var name = pop.get();
              var args = List.copyOf(deque);
              deque.clear();
              yield options.with(argument, new Tool(name, args));
            }
            case "--action", "--actions" -> options.with("actions", Action.ofCli(pop.get()));
            default -> options.with("actions", Action.ofCli(argument));
          };
    }
    return options;
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
    options = options.with("projectName", info.name());
    options = options.with("projectVersion", Version.parse(info.version()));
    options = options.with("projectRequires", List.of(info.requires()));
    // main
    var main = info.main();
    options = options.with("mainJavaRelease", main.javaRelease());
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
      options = options.with("limitTools", String.join(",", tools.limits()));
    if (!tools.skips().isEmpty())
      options = options.with("skipTools", String.join(",", tools.skips()));
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

  public static Options compose(String id, Logbook logbook, Options... options) {
    /* DEBUG */ {
      logbook.log(Level.DEBUG, "Compose options from " + options.length + " layers");
      for (int i = 0; i < options.length; i++) {
        logbook.log(Level.TRACE, "[" + i + "] = " + options[i].id.orElse("-"));
      }
      logbook.log(Level.DEBUG, "[layer] --<option> <value...>");
    }

    return new Records<>(Options.class)
        .compose(
            Options.of().id(id),
            component -> !component.getName().equals("id"),
            value ->
                value instanceof Boolean flag && flag
                    || value instanceof Optional<?> optional && optional.isPresent()
                    || value instanceof List<?> list && !list.isEmpty(),
            composition ->
                logbook.log(
                    Level.DEBUG,
                    "[%d] %s -> %s"
                        .formatted(
                            composition.index(),
                            composition.component().getName(),
                            composition.newValue())),
            options);
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

  @Override
  public Options with(String name, Object value) {
    return Wither.super.with(name, component -> wrap(component, value), Options::merge);
  }

  private static Object wrap(RecordComponent component, Object newValue) {
    if (component.getType() == Optional.class) {
      return newValue instanceof Optional ? newValue : Optional.ofNullable(newValue);
    }
    if (component.getType() == List.class) {
      return newValue instanceof List ? newValue : List.of(newValue);
    }
    return newValue;
  }

  private static Object merge(Object oldValue, Object newValue) {
    if (oldValue instanceof List<?> oldList && newValue instanceof List<?> newList) {
      return Stream.concat(oldList.stream(), newList.stream()).toList();
    }
    return newValue;
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

  private static final Options EMPTY = new Records<>(Options.class).compose(Options::emptyValue);

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
