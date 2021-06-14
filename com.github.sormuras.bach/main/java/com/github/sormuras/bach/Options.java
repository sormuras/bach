package com.github.sormuras.bach;

import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.internal.Strings;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
          Prevent all tool call executions.""") Boolean dry_run,
    @Help("""
        --sequential
          Prevent parallel execution of tool calls.""")
        Boolean sequential,
    // </editor-fold>

    // <editor-fold desc="Primordal Options">
    @Help(
            """
        --chroot PATH
          Change virtually into the specified directory by resolving all generated
          paths against the given PATH before passing them as arguments to tools.""")
        @Extra
        Path chroot,
    @Help(
            """
        --bach-info MODULE
          Defaults to:\s"""
                + "bach.info")
        @Extra
        String bach_info,
    // </editor-fold>

    // <editor-fold desc="Project Options">
    @Help("""
        --project-name NAME""") String project_name,
    @Help("""
        --project-version VERSION""") Version project_version,
    @Help("""
        --project-requires MODULE(,MODULE)*
          This option is repeatable.""")
        @CommaSeparatedValue
        List<String> project_requires,
    // </editor-fold>

    // <editor-fold desc="Main Code Space Options">
    @Help(
            """
        --main-module-pattern NAME
          Specify where to find module-info.java files for the main code space.
          This option is repeatable.""")
        List<String> main_module_pattern,
    @Help(
            """
        --main-module-path PATH
          Specify where to find modules for compiling main modules.
          This option is repeatable.""")
        List<String> main_module_path,
    @Help(
            """
        --main-java-release RELEASE
          Compile main modules for the specified Java SE release.
          Supported releases depend on the JDK version being used.
          Consult `javac --help` for details.""")
        Integer main_java_release,
    @Help(
            """
        --main-jar-with-sources
          Include all files found in source folders into their modular JAR files.""")
        Boolean main_jar_with_sources,
    // </editor-fold>

    // <editor-fold desc="Test Code Space Options">
    @Help(
            """
        --test-module-pattern NAME
          Specify where to find module-info.java files for the test code space.
          This option is repeatable.""")
        List<String> test_module_pattern,
    @Help(
            """
        --test-module-path PATH
          Specify where to find modules for compiling and running test modules.
          This option is repeatable.""")
        List<String> test_module_path,
    // </editor-fold>

    // <editor-fold desc="Tools & Tweaks">
    @Help("""
        --limit-tool TOOL(,TOOL)*""") @CommaSeparatedValue List<String> limit_tool,
    @Help("""
        --skip-tool TOOL(,TOOL)*""") @CommaSeparatedValue List<String> skip_tool,
    @Help(
            """
        --tweak SPACE(,SPACE)* TRIGGER COUNT ARG [ARGS...]
          Additional command-line arguments passed to tool runs matching the specified trigger.
          Examples:
            --tweak main javac 1 -parameters
            --tweak main javac 2 -encoding UTF-8
          This option is repeatable.""")
        List<Tweak> tweak,
    // </editor-fold>

    // <editor-fold desc="External Modules and Libraries">
    @Help(
            """
        --external-module-location MODULE LOCATION
          Specify an external module-location mapping.
          Example:
            --external-module-location
              org.junit.jupiter=org.junit.jupiter:junit-jupiter:5.7.1
          This option is repeatable.""")
        List<ExternalModuleLocation> external_module_location,
    @Help(
            """
        --external-library-version NAME VERSION
          An external library name and version, providing a set of module-location mappings.
          Example:
            --external-library-version
              JUnit=5.7.1
          This option is repeatable.""")
        List<ExternalLibraryVersion> external_library_version
    // </editor-fold>
    ) {

  public static Options of() {
    return EMPTY;
  }

  public static Options ofDefaultValues() {
    return DEFAULTS;
  }

  public static Options ofCommandLineArguments(String... args) {
    return Options.ofCommandLineArguments(List.of(args));
  }

  public static Options ofCommandLineArguments(List<String> args) {
    if (args.isEmpty()) return Options.of();
    var arguments = new LinkedList<>(args);
    var options = Options.of();
    while (!arguments.isEmpty()) {
      var peeked = arguments.peekFirst().strip();
      if (peeked.startsWith("@")) {
        var file = Path.of(arguments.removeFirst().substring(1));
        var lines = Strings.arguments(file);
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
      if (FLAG_KEYS.contains(key)) {
        options = options.with(key, "true");
        continue;
      }

      var value = arguments.removeFirst().strip();
      if (COMMA_SEPARATED_KEYS.contains(key)) {
        for (var string : value.split(",")) options = options.with(key, string);
        continue;
      }

      options = options.with(key, value);
    }
    return options;
  }

  public static Options ofFile(Path file) {
    if (Files.notExists(file)) return Options.of();
    return Options.ofCommandLineArguments(Strings.arguments(file));
  }

  public Options underlay(Options... layers) {
    if (layers.length == 0) return this;
    return new Options(
        underlay(Options::verbose, layers),
        underlay(Options::dry_run, layers),
        underlay(Options::sequential, layers),
        underlay(Options::chroot, layers),
        underlay(Options::bach_info, layers),
        underlay(Options::project_name, layers),
        underlay(Options::project_version, layers),
        underlay(Options::project_requires, layers),
        underlay(Options::main_module_pattern, layers),
        underlay(Options::main_module_path, layers),
        underlay(Options::main_java_release, layers),
        underlay(Options::main_jar_with_sources, layers),
        underlay(Options::test_module_pattern, layers),
        underlay(Options::test_module_path, layers),
        underlay(Options::limit_tool, layers),
        underlay(Options::skip_tool, layers),
        underlay(Options::tweak, layers),
        underlay(Options::external_module_location, layers),
        underlay(Options::external_library_version, layers));
  }

  private <T> T underlay(Function<Options, T> function, Options... layers) {
    var initialValue = function.apply(this);
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
    nonValid.removeAll(ALL_KEYS);
    if (!nonValid.isEmpty()) throw new IllegalArgumentException(nonValid.toString());
    return new Options(
        withFlag(verbose, map.get("--verbose")),
        withFlag(dry_run, map.get("--dry-run")),
        withFlag(sequential, map.get("--sequential")),
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
            ExternalModuleLocation::of),
        withMerging(
            external_library_version,
            map.get("--external-library-version"),
            ExternalLibraryVersion::of));
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

  private static List<String> withMerging(List<String> old, Value value) {
    return withMerging(old, value, Function.identity());
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

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface CommaSeparatedValue {}

  private static final Options EMPTY =
      new Options(
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null, null);

  private static final Options DEFAULTS =
      new Options(
          false,
          false,
          false,
          Path.of("."),
          "bach.info",
          ".",
          Version.parse("0"),
          List.of(), // --project-requires
          List.of("module-info.java", "*", "**"),
          List.of(".bach/external-modules"),
          0,
          false,
          List.of("test", "**/test", "**/test/**"),
          List.of(".bach/workspace/modules", ".bach/external-modules"),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of());

  private static final Set<String> ALL_KEYS = keys(__ -> true);
  private static final Set<String> COMMA_SEPARATED_KEYS = keys(Options::isCommaSeparatedValue);
  private static final Set<String> FLAG_KEYS = keys(Options::isFlag);

  private static Set<String> keys(Predicate<RecordComponent> filter) {
    return Stream.of(Options.class.getRecordComponents())
        .filter(filter)
        .map(RecordComponent::getName)
        .map(name -> "--" + name.replace('_', '-'))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isCommaSeparatedValue(RecordComponent component) {
    return component.getType() == List.class
        && component.isAnnotationPresent(CommaSeparatedValue.class);
  }

  private static boolean isFlag(RecordComponent component) {
    return component.getType() == Boolean.class;
  }
}
