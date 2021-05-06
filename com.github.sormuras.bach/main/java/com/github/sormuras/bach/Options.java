package com.github.sormuras.bach;

import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.api.Tweak;
import java.lang.System.Logger.Level;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
    Optional<String> id,

    // <editor-fold desc="Runtime Modifying Options">
    @Help("""
        --verbose
          Output messages about what Bach is doing.""")
        boolean verbose,
    @Help("""
        --dry-run
          Prevent command execution.
        """) boolean dryRun,
    @Help("""
        --run-commands-sequentially
        """) boolean runCommandsSequentially,
    // </editor-fold>

    // <editor-fold desc="Run'N Exit Command Options">
    @Help("""
        --version
          Print version information and exit.""") boolean version,
    @Help("""
        --help
          Print this help message and exit.""") boolean help,
    @Help("""
        --help-extra
          Print help on extra options and exit.""")
        boolean helpExtra,
    @Help("""
        --list-configuration
          Print effective configuration and exit.""")
        boolean listConfiguration,
    @Help("""
        --list-modules
          List modules and exit.""") boolean listModules,
    @Help("""
        --list-tools
          List tools and exit.""") boolean listTools,
    @Help("""
        --describe-tool TOOL
          Describe tool and exit.""")
        Optional<String> describeTool,
    @Help("""
        --load-external-module MODULE
          Load an external module.""")
        Optional<String> loadExternalModule,
    @Help(
            """
        --load-missing-external-modules
          Load all missing external modules.""")
        boolean loadMissingExternalModules,
    @Help(
            """
        --tool NAME [ARGS...]
          Run the specified tool and exit with its return value.""")
        Optional<Command<?>> tool,
    // </editor-fold>

    // <editor-fold desc="Primordal Options">
    @Extra
        @Help(
            """
                --chroot PATH
                  Change virtually into the specified directory by resolving all generated
                  paths against the given PATH before passing them as arguments to tools.
                """)
        Optional<Path> chroot,
    @Extra
        @Help(
            """
                --bach-info MODULE
                  Defaults to: bach.info
                """)
        Optional<String> bachInfo,
    // </editor-fold>

    // <editor-fold desc="Project Options">
    @Help("""
          --project-name NAME
          """) Optional<String> projectName,
    @Help("""
          --project-version VERSION
          """) Optional<Version> projectVersion,
    @Help(
            """
          --project-requires MODULE
            This option is repeatable.
          """)
        List<String> projectRequires,
    // </editor-fold>

    // <editor-fold desc="Main Code Space Options">
    @Help(
            """
          --main-module-pattern NAME
            Specify where to find module-info.java files for the main code space.
            This option is repeatable.
          """)
        List<String> mainModulePatterns,
    @Help(
            """
          --main-module-path PATH
            Specify where to find modules for compiling main modules.
            This option is repeatable.
          """)
        List<String> mainModulePaths,
    @Help(
            """
          --main-java-release RELEASE
            Compile main modules for the specified Java SE release.
          """)
        Optional<Integer> mainJavaRelease,
    @Help(
            """
        --main-jar-with-sources
          Include all files found in source folders into their modular JAR files.
        """)
        boolean mainJarWithSources,
    // </editor-fold>

    // <editor-fold desc="Test Code Space Options">
    @Help(
            """
          --test-module-pattern NAME
            Specify where to find module-info.java files for the test code space.
            This option is repeatable.
          """)
        List<String> testModulePatterns,
    @Help(
            """
          --test-module-path PATH
            Specify where to find modules for compiling and running test modules.
            This option is repeatable.
          """)
        List<String> testModulePaths,
    // </editor-fold>

    // <editor-fold desc="Tools & Tweaks">
    @Help("""
        --limit-tools TOOL(,TOOL)*
        """) Optional<String> limitTools,
    @Help("""
        --skip-tools TOOL(,TOOL)*
        """) Optional<String> skipTools,
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
    @Help(
            """
        --action ACTION
          Execute the specified action.
          This option is repeatable.""")
        List<Action> actions
    // </editor-fold>
    ) {

  public static String generateHelpMessage(Predicate<RecordComponent> filter) {
    var options =
        Stream.of(canonicalComponents)
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
    return emptyOptions;
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
            case "--id" -> options.id(pop.get());
            case "--verbose" -> options.with("--verbose", true);
            case "--dry-run" -> options.with("--dry-run", true);
            case "--run-commands-sequentially" -> options.with("--run-commands-sequentially", true);
            case "--chroot" -> options.with("--chroot", Path.of(pop.get()));
            case "--bach-info" -> options.with("--bach-info", pop.get());
            case "--version" -> options.with("--version", true);
            case "--help" -> options.with("--help", true);
            case "--help-extra" -> options.with("--help-extra", true);
            case "--list-configuration" -> options.with("--list-configuration", true);
            case "--list-modules" -> options.with("--list-modules", true);
            case "--list-tools" -> options.with("--list-tools", true);
            case "--describe-tool" -> options.with("--describe-tool", pop.get());
            case "--load-external-module" -> options.with("--load-external-module", pop.get());
            case "--load-missing-external-modules" -> options.with(
                "--load-missing-external-modules", true);
            case "--project-name" -> options.with("--project-name", pop.get());
            case "--project-version" -> options.with("--project-version", Version.parse(pop.get()));
            case "--project-requires" -> options.with("--project-requires", pop.get());
            case "--main-module-path", "--main-module-paths" -> options.with(
                "--main-module-paths", pop.get());
            case "--main-module-pattern", "--main-module-patterns" -> options.with(
                "--main-module-patterns", pop.get());
            case "--main-java-release" -> options.with(
                "--main-java-release", Integer.parseInt(pop.get()));
            case "--main-jar-with-sources" -> options.with("--main-jar-with-sources", true);
            case "--test-module-path", "--test-module-paths" -> options.with(
                "--test-module-paths", pop.get());
            case "--test-module-pattern", "--test-module-patterns" -> options.with(
                "--test-module-patterns", pop.get());
            case "--limit-tools" -> options.with("--limit-tools", pop.get());
            case "--skip-tools" -> options.with("--skip-tools", pop.get());
            case "--tweak", "--tweaks" -> options.with("--tweaks", Tweak.ofCommandLine(deque));
            case "--external-module-location", "--external-module-locations" -> options.with(
                "--external-module-locations", ExternalModuleLocation.ofCommandLine(pop));
            case "--external-library-version", "--external-library-versions" -> options.with(
                "--external-library-versions", ExternalLibraryVersion.ofCommandLine(pop));
            case "--tool" -> {
              var name = pop.get();
              var args = deque.toArray(String[]::new);
              deque.clear();
              yield options.with("--tool", Command.of(name, args));
            }
            case "--action", "--actions" -> options.with("--actions", Action.ofCli(pop.get()));
            default -> options.with("--actions", Action.ofCli(argument));
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

  public static Options ofProjectInfoElements(ProjectInfo info) {
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
      logbook.log(Level.DEBUG, "[component] --<option> <value...>");
    }
    var composite = Options.of().id(id);
    component:
    for (var component : canonicalComponents) {
      var name = component.getName();
      if (name.equals("id")) continue; // skip
      for (int i = 0; i < options.length; i++) {
        var layer = options[i];
        var value = access(layer, component);
        if (value instanceof Boolean flag && flag
            || value instanceof Optional<?> optional && optional.isPresent()
            || value instanceof List<?> list && !list.isEmpty()) {
          composite = composite.with(name, value);
          logbook.log(Level.DEBUG, "[%d] %s -> %s".formatted(i, name, value));
          continue component;
        }
      }
    }
    return composite;
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

  public Options with(String option, Object newValue) {
    var name = option.startsWith("--") ? toComponentName(option) : option;
    if (!canonicalNames.contains(name)) throw new IllegalArgumentException(option);
    var values = new ArrayList<>();
    for (var component : canonicalComponents) {
      var oldValue = access(this, component);
      var concatValue = concat(oldValue, wrap(component, newValue));
      values.add(component.getName().equals(name) ? concatValue : oldValue);
    }
    return create(values);
  }

  private Object wrap(RecordComponent component, Object newValue) {
    if (component.getType() == Optional.class) {
      return newValue instanceof Optional ? newValue : Optional.ofNullable(newValue);
    }
    if (component.getType() == List.class) {
      return newValue instanceof List ? newValue : List.of(newValue);
    }
    return newValue;
  }

  private Object concat(Object oldValue, Object newValue) {
    if (oldValue instanceof List<?> oldList) {
      if (newValue instanceof List<?> newList) {
        return Stream.concat(oldList.stream(), newList.stream()).toList();
      }
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

  public static boolean isHelp(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && !component.isAnnotationPresent(Extra.class);
  }

  public static boolean isHelpExtra(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && component.isAnnotationPresent(Extra.class);
  }

  public static String toComponentName(String cli) {
    var codes = cli.substring(2).codePoints().toArray();
    var builder = new StringBuilder(codes.length * 2);
    for (int i = 0; i < codes.length; i++) {
      int point = codes[i];
      if (point == "-".codePointAt(0)) {
        builder.append(Character.toChars(Character.toUpperCase(codes[++i])));
        continue;
      }
      builder.append(Character.toChars(point));
    }
    return builder.toString();
  }

  private static final RecordComponent[] canonicalComponents = Options.class.getRecordComponents();
  private static final Constructor<Options> canonicalConstructor = canonicalConstructor();
  private static final Set<String> canonicalNames = canonicalNames();
  private static final Options emptyOptions = emptyOptions();

  @SuppressWarnings("JavaReflectionMemberAccess")
  private static Constructor<Options> canonicalConstructor() {
    var types = Stream.of(canonicalComponents).map(RecordComponent::getType);
    try {
      return Options.class.getDeclaredConstructor(types.toArray(Class<?>[]::new));
    } catch (NoSuchMethodException exception) {
      throw new Error(exception);
    }
  }

  private static Set<String> canonicalNames() {
    return Stream.of(canonicalComponents)
        .map(RecordComponent::getName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Options emptyOptions() {
    return create(Stream.of(canonicalComponents).map(Options::emptyValue).toList());
  }

  private static Object emptyValue(RecordComponent component) {
    if (boolean.class == component.getType()) return false;
    if (Optional.class == component.getType()) return Optional.empty();
    if (List.class == component.getType()) return List.of();
    throw new Error("Unsupported type: " + component.getType());
  }

  private static Options create(List<Object> arguments) {
    var initargs = arguments.toArray(Object[]::new);
    try {
      return canonicalConstructor.newInstance(initargs);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError("create Options failed for " + arguments, exception);
    }
  }

  private static Object access(Options options, RecordComponent component) {
    try {
      return component.getAccessor().invoke(options);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError("access failed for " + component + " on " + options, exception);
    }
  }
}
