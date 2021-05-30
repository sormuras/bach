package com.github.sormuras.bach;

import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.tool.AnyCall;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Options(
    // <editor-fold desc="Runtime Modifying Options">
    @Help("""
        --verbose
          Output messages about what Bach is doing.""")
        Boolean verbose,
    @Help("""
        --dry-run
          Prevent command execution.
        """)
        Boolean dry_run,
    @Help("""
		--run-commands-sequentially
			Prevent parallel execution of commands.
		""")
        Boolean run_commands_sequentially,
    // </editor-fold>

    // <editor-fold desc="Run'N Exit Command Options">
    @Help("""
        --version
          Print version information and exit.""") Boolean version,
    @Help("""
        --help
          Print this help message and exit.""") Boolean help,
    @Help("""
        --help-extra
          Print help on extra options and exit.""")
        Boolean help_extra,
    @Help("""
		--print-configuration
			Print effective configuration and exit.""")
        Boolean print_configuration,
    @Help(
            """
        --print-modules
          List all (declared, external, and system) modules and exit.""")
        Boolean print_modules,
    @Help(
            """
        --print-declared-modules
          List declared (main and test) modules and exit.""")
        Boolean print_declared_modules,
    @Help("""
        --print-external-modules
          List external modules and exit.""")
        Boolean print_external_modules,
    @Help("""
        --print-system-modules
          List system modules and exit.""")
        Boolean print_system_modules,
    @Help("""
        --print-tools
          List tools and exit.""") Boolean print_tools,
    @Help("""
        --describe-tool TOOL
          Describe tool and exit.""")
        String describe_tool,
    @Help("""
        --load-external-module MODULE
          Load an external module.""")
        String load_external_module,
    @Help("""
		--load-missing-external-modules
			Load all missing external modules.""")
        Boolean load_missing_external_modules,
    @Help(
            """
		--tool NAME [ARGS...]
			Run the specified tool and exit with its return value.""")
        AnyCall tool,
    // </editor-fold>

    // <editor-fold desc="Primordal Options">
    @Extra
        @Help(
            """
						--chroot PATH
							Change virtually into the specified directory by resolving all generated
							paths against the given PATH before passing them as arguments to tools.
						""")
        Path chroot,
    @Extra @Help("""
						--bach-info MODULE
							Defaults to: bach.info
						""")
        String bach_info,
    // </editor-fold>

    // <editor-fold desc="Project Options">
    @Help("""
          --project-name NAME
          """) String project_name,
    @Help("""
          --project-version VERSION
          """) Version project_version,
    @Help("""
			--project-requires MODULE
				This option is repeatable.
			""")
        List<String> project_requires,
    // </editor-fold>

    // <editor-fold desc="Main Code Space Options">
    @Help(
            """
			--main-module-pattern NAME
				Specify where to find module-info.java files for the main code space.
				This option is repeatable.
			""")
        List<String> main_module_pattern,
    @Help(
            """
			--main-module-path PATH
				Specify where to find modules for compiling main modules.
				This option is repeatable.
			""")
        List<String> main_module_path,
    @Help(
            """
			--main-java-release RELEASE
				Compile main modules for the specified Java SE release.
			""")
        Integer main_java_release,
    @Help(
            """
		--main-jar-with-sources
			Include all files found in source folders into their modular JAR files.
		""")
        Boolean main_jar_with_sources,
    // </editor-fold>

    // <editor-fold desc="Test Code Space Options">
    @Help(
            """
			--test-module-pattern NAME
				Specify where to find module-info.java files for the test code space.
				This option is repeatable.
			""")
        List<String> test_module_pattern,
    @Help(
            """
			--test-module-path PATH
				Specify where to find modules for compiling and running test modules.
				This option is repeatable.
			""")
        List<String> test_module_path,
    // </editor-fold>

    // <editor-fold desc="Tools & Tweaks">
    @Help("""
        --limit-tool TOOL(,TOOL)*
        """) List<String> limit_tool,
    @Help("""
        --skip-tool TOOL(,TOOL)*
        """) List<String> skip_tool,
    @Help(
            """
			--tweak SPACE(,SPACE)* TRIGGER COUNT ARG [ARGS...]
				Additional command-line arguments passed to tool runs matching the specified trigger.
				Examples:
					--tweak main javac 1 -parameters
					--tweak main javac 2 -encoding UTF-8
				This option is repeatable.
			""")
        List<Tweak> tweak,
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
        List<ExternalModuleLocation> external_module_location,
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
        List<ExternalLibraryVersion> external_library_version,
    // </editor-fold>

    // <editor-fold desc="Workflows">
    @Help(
            """
		--workflow WORKFLOW
			Execute a workflow specified by its name.
			This option is repeatable.""")
        List<Workflow> workflow
    // </editor-fold>
    ) {

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

  public static Options of(UnaryOperator<ToolCall<?>> operator) {
    var call = new AnyCall("bach");
    return Options.ofCommandLineArguments(operator.apply(call).arguments());
  }

  public static Options ofCommandLineArguments(List<String> args) {
    var arguments = new LinkedList<>(args);
    var options = EMPTY;
    while (!arguments.isEmpty()) {
      var peeked = arguments.peekFirst().strip();
      if (peeked.startsWith("@")) {
        var file = Path.of(arguments.removeFirst().substring(1));
        var lines = Strings.lines(file);
        var iterator = lines.listIterator(lines.size());
        while (iterator.hasPrevious()) arguments.addFirst(iterator.previous().strip());
      }

      var key = arguments.removeFirst().strip();
      if (key.isEmpty()) throw new BachException("Option key must not be empty");
      if (key.lines().count() > 1) throw new BachException("Multi-line key?%n%s", key.indent(2));
      if (!key.startsWith("--")) {
        options = options.with("--workflow", key);
        continue;
      }
      if (FLAGS.contains(key)) {
        options = options.with(key, "true");
        continue;
      }

      var value = arguments.removeFirst().strip();
      if (key.equals("--tool")) {
        var more = arguments.subList(0, arguments.size()).stream().map(String::strip);
        options = options.with(key, value, more.toArray(String[]::new));
        arguments.clear();
        break;
      }

      options = options.with(key, value);
    }
    return options;
  }

  public static Options ofFile(Path file) {
    if (Files.notExists(file)) return Options.of();
    return Options.ofCommandLineArguments(Strings.lines(file));
  }

  public static Options ofDefaultValues() {
    return new Options(
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null,
        null,
        false,
        null,
        Path.of("."),
        "bach.info",
        ".",
        Version.parse("0"),
        List.of(),
        List.of(),
        List.of(),
        null,
        false,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  public static Options ofProjectInfo(ProjectInfo info) {
    var main = info.main();
    var test = info.test();
    var tool = info.tool();
    var external = info.external();
    return new Options(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        info.name(),
        Version.parse(info.version()),
        List.of(info.requires()),
        List.of(main.modulesPatterns()),
        List.of(main.modulePaths()),
        main.javaRelease(),
        main.jarWithSources(),
        List.of(test.modulesPatterns()),
        List.of(test.modulePaths()),
        List.of(tool.limit()),
        List.of(tool.skip()),
        Stream.of(tool.tweaks()).map(Tweak::of).toList(),
        Stream.of(external.modules()).map(ExternalModuleLocation::ofInfo).toList(),
        Stream.of(external.libraries()).map(ExternalLibraryVersion::ofInfo).toList(),
        null);
  }

  public Options underlay(Options... layers) {
    if (layers.length == 0) return this;
    return new Options(
        underlay(verbose, Options::verbose, layers),
        underlay(dry_run, Options::dry_run, layers),
        underlay(run_commands_sequentially, Options::run_commands_sequentially, layers),
        underlay(version, Options::version, layers),
        underlay(help, Options::help, layers),
        underlay(help_extra, Options::help_extra, layers),
        underlay(print_configuration, Options::print_configuration, layers),
        underlay(print_modules, Options::print_modules, layers),
        underlay(print_declared_modules, Options::print_declared_modules, layers),
        underlay(print_external_modules, Options::print_external_modules, layers),
        underlay(print_system_modules, Options::print_system_modules, layers),
        underlay(print_tools, Options::print_tools, layers),
        underlay(describe_tool, Options::describe_tool, layers),
        underlay(load_external_module, Options::load_external_module, layers),
        underlay(load_missing_external_modules, Options::load_missing_external_modules, layers),
        underlay(tool, Options::tool, layers),
        underlay(chroot, Options::chroot, layers),
        underlay(bach_info, Options::bach_info, layers),
        underlay(project_name, Options::project_name, layers),
        underlay(project_version, Options::project_version, layers),
        underlay(project_requires, Options::project_requires, layers),
        underlay(main_module_pattern, Options::main_module_pattern, layers),
        underlay(main_module_path, Options::main_module_path, layers),
        underlay(main_java_release, Options::main_java_release, layers),
        underlay(main_jar_with_sources, Options::main_jar_with_sources, layers),
        underlay(test_module_pattern, Options::test_module_pattern, layers),
        underlay(test_module_path, Options::test_module_path, layers),
        underlay(limit_tool, Options::limit_tool, layers),
        underlay(skip_tool, Options::skip_tool, layers),
        underlay(tweak, Options::tweak, layers),
        underlay(external_module_location, Options::external_module_location, layers),
        underlay(external_library_version, Options::external_library_version, layers),
        underlay(workflow, Options::workflow, layers));
  }

  private static <T> T underlay(T initialValue, Function<Options, T> function, Options... layers) {
    if (initialValue != null) return initialValue;
    for (var layer : layers) {
      var value = function.apply(layer);
      if (value != null) return value;
    }
    return null;
  }

  public Options with(String option, String text) {
    return with(Map.of(option, new Value(text)));
  }

  public Options with(String option, String text, String... more) {
    return with(Map.of(option, new Value(text, more)));
  }

  private record Value(String text, String... more) {}

  private Options with(Map<String, Value> map) {
    var nonValid = new LinkedHashSet<>(map.keySet());
    nonValid.removeAll(VALID_OPTION_KEYS);
    if (!nonValid.isEmpty()) throw new IllegalArgumentException(nonValid.toString());
    return new Options(
        withFlag(verbose, map.get("--verbose")),
        withFlag(dry_run, map.get("--dry-run")),
        withFlag(run_commands_sequentially, map.get("--run-commands-sequentially")),
        withFlag(version, map.get("--version")),
        withFlag(help, map.get("--help")),
        withFlag(help_extra, map.get("--help-extra")),
        withFlag(print_configuration, map.get("--print-configuration")),
        withFlag(print_modules, map.get("--print-modules")),
        withFlag(print_declared_modules, map.get("--print-declared-modules")),
        withFlag(print_external_modules, map.get("--print-external-modules")),
        withFlag(print_system_modules, map.get("--print-system-modules")),
        withFlag(print_tools, map.get("--print-tools")),
        withSetting(describe_tool, map.get("--describe-tool")),
        withSetting(load_external_module, map.get("--load-external-module")),
        withFlag(load_missing_external_modules, map.get("--load-missing-external-modules")),
        withValue(tool, map.get("--tool"), v -> new AnyCall(v.text).withAll(v.more)),
        withSetting(chroot, map.get("--chroot"), Path::of),
        withSetting(bach_info, map.get("--bach-info")),
        withSetting(project_name, map.get("--project-name")),
        withSetting(project_version, map.get("--project-version"), Version::parse),
        withMerging(project_requires, map.get("--project-requires")),
        withMerging(main_module_pattern, map.get("--main-module-pattern")),
        withMerging(main_module_path, map.get("--main-module-path")),
        withSetting(main_java_release, map.get("--main-java-release"), Integer::valueOf),
        withFlag(main_jar_with_sources, map.get("--main-jar-with-sources")),
        withMerging(test_module_pattern, map.get("--test-module-pattern")),
        withMerging(test_module_path, map.get("--test-module-path")),
        withMerging(limit_tool, map.get("--limit-tool")),
        withMerging(skip_tool, map.get("--skip-tool")),
        withMerging(tweak, map.get("--tweak"), Tweak::of),
        withMerging(
            external_module_location,
            map.get("--external-module-location"),
            ExternalModuleLocation::ofCommandLine),
        withMerging(
            external_library_version,
            map.get("--external-library-version"),
            ExternalLibraryVersion::ofCommandLine),
        withMerging(workflow, map.get("--workflow"), Workflow.class));
  }

  private static Boolean withFlag(Boolean old, Value value) {
    return withSetting(old, value, Boolean::valueOf);
  }

  private static String withSetting(String old, Value value) {
    return withSetting(old, value, Function.identity());
  }

  private static <T> T withSetting(T old, Value value, Function<String, T> mapper) {
    if (value == null) return old;
    if (value.text == null) return null;
    return mapper.apply(value.text);
  }

  private static <T> T withValue(T old, Value value, Function<Value, T> mapper) {
    if (value == null) return old;
    if (value.text == null) return null;
    return mapper.apply(value);
  }

  private static List<String> withMerging(List<String> old, Value value) {
    return withMerging(old, value, Function.identity());
  }

  private static <T extends Enum<T>> List<T> withMerging(
      List<T> old, Value value, Class<T> enumType) {
    return withMerging(
        old,
        value,
        string -> Enum.valueOf(enumType, string.toUpperCase(Locale.ROOT).replace('-', '_')));
  }

  private static <T> List<T> withMerging(List<T> old, Value value, Function<String, T> mapper) {
    if (value == null) return old;
    if (value.text == null) return null;
    var list = new ArrayList<T>();
    if (old != null) list.addAll(old);
    list.add(mapper.apply(value.text));
    Stream.of(value.more).map(mapper).forEach(list::add);
    return List.copyOf(list);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Help {
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Extra {}

  private static final Set<String> FLAGS =
      Stream.of(Options.class.getRecordComponents())
          .filter(component -> component.getType() == Boolean.class)
          .map(RecordComponent::getName)
          .map(name -> "--" + name.replace('_', '-'))
          .collect(Collectors.toSet());

  private static final Set<String> VALID_OPTION_KEYS =
      Stream.of(Options.class.getRecordComponents())
          .map(RecordComponent::getName)
          .map(name -> "--" + name.replace('_', '-'))
          .collect(Collectors.toSet());

  private static final Options EMPTY =
      new Options(
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null);

  public static boolean isHelp(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && !component.isAnnotationPresent(Extra.class);
  }

  public static boolean isHelpExtra(RecordComponent component) {
    return component.isAnnotationPresent(Help.class) && component.isAnnotationPresent(Extra.class);
  }
}
