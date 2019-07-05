package de.sormuras.bach;

import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Modular project model. */
public class Project {

  /** Supported property keys, default values, and descriptions. */
  public enum Property {
    NAME("project", "Name of the project."),
    VERSION("1.0.0-SNAPSHOT", "Version of the project. Must be parse-able by " + Version.class.getSimpleName()),
    // Paths
    PATH_SOURCES("src", "Path to directory containing all Java module sources."),
    // Default options for various tools
    OPTIONS_MODULES("*", "List of modules to compile, or '*' indicating all modules."),
    OPTIONS_JAVAC("-encoding\nUTF-8\n-parameters\n-Xlint", "Options passed to all 'javac' calls.");

    final String key;
    final String defaultValue;
    final String description;

    Property(String defaultValue, String description) {
      this.key =name().replace('_', '.').toLowerCase();
      this.defaultValue = defaultValue;
      this.description = description;
    }

    public String get(Properties properties) {
      return get(properties, () -> defaultValue);
    }

    public String get(Properties properties, Supplier<Object> defaultValueSupplier) {
      return Util.get(key, properties, defaultValueSupplier);
    }

    public List<String> lines(Properties properties) {
      return get(properties).lines().collect(Collectors.toList());
    }
  }

  public static void help(PrintWriter writer) {
    writer.println("Properties");
    for(var property : Property.values()) {
      writer.println(property.key + " -> " + property.description);
      writer.println(property.key + " -> " + property.defaultValue);
    }
  }

  public static Project of(Path properties) {
    if (!Files.isRegularFile(properties)) {
      throw new IllegalArgumentException("Expected .properties file: '" + properties + "'");
    }
    var home = Optional.ofNullable(properties.getParent()).orElse(Path.of(""));
    return of(home, Util.newProperties(properties));
  }

  private static Project of(Path home, Properties properties) {
    // basics...
    var name = Property.NAME.get(properties, () -> home.toAbsolutePath().getFileName());
    var version = Version.parse(Property.VERSION.get(properties));
    // paths...
    var sources = home.resolve(Property.PATH_SOURCES.get(properties));
    var paths = new Paths(home, sources);
    // options...
    var modules = Options.modules(Property.OPTIONS_MODULES.get(properties), sources);
    var options = new Options(modules, Property.OPTIONS_JAVAC.lines(properties));

    return new Project(name, version, paths, options);
  }

  final String name;
  final Version version;
  final Paths paths;
  final Options options;

  private Project(String name, Version version, Paths paths, Options options) {
    this.name = name;
    this.version = version;
    this.paths = paths;
    this.options = options;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("version=" + version)
        .add("paths=" + paths)
        .add("options=" + options)
        .toString();
  }

  /** Directories, files and other paths. */
  static class Paths {
    final Path home;
    final Path sources;

    Paths(Path home, Path sources) {
      this.home = home;
      this.sources = sources;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Paths.class.getSimpleName() + "[", "]")
          .add("home='" + home + "'")
          .add("sources=" + sources)
          .toString();
    }
  }

  /** Default options passed to various tools. */
  static class Options {

    static List<String> modules(String modules, Path sources) {
      if ("*".equals(modules)) {
        return Util.findDirectoryNames(sources);
      }
      return List.of(modules.split(","));
    }

    final List<String> modules;
    final List<String> javac;

    Options(List<String> modules, List<String> javac) {
      this.modules = modules;
      this.javac = javac;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Options.class.getSimpleName() + "[", "]")
          .add("modules=" + modules)
          .add("javac=" + javac)
          .toString();
    }
  }
}
