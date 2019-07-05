package de.sormuras.bach;

import java.io.File;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Modular project model. */
public class Project {

  /** Supported property keys, default values, and descriptions. */
  public enum Property {
    /** Name of the project. */
    NAME("project", "Name of the project."),
    /**
     * Version of the project.
     *
     * @see Version#parse(String)
     */
    VERSION("1.0.0-SNAPSHOT", "Version of the project. Must be parse-able by " + Version.class),
    // Paths
    /** Path to directory containing all Java module sources. */
    PATH_SOURCES("src", "Path to directory containing all Java module sources."),
    // Default options for various tools
    /** List of modules to compile, or '*' indicating all modules. */
    OPTIONS_MODULES("*", "List of modules to compile, or '*' indicating all modules."),
    /** Options passed to all 'javac' calls. */
    OPTIONS_JAVAC("-encoding\nUTF-8\n-parameters\n-Xlint", "Options passed to all 'javac' calls.");

    final String key;
    final String defaultValue;
    final String description;

    Property(String defaultValue, String description) {
      this.key = name().replace('_', '.').toLowerCase();
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
    for (var property : Property.values()) {
      writer.println(property.key + " -> " + property.description);
      writer.println(property.key + " -> " + property.defaultValue);
    }
  }

  /** Create project based on the given path, either a directory or a properties file. */
  public static Project of(Path path) {
    if (Files.isDirectory(path)) {
      var directory = Objects.toString(path.getFileName(), Property.NAME.defaultValue);
      var names = List.of(directory, "bach", "");
      for (var name : names) {
        var file = path.resolve(name + ".properties");
        if (Files.isRegularFile(file)) {
          return of(path, Util.loadProperties(file));
        }
      }
      return of(path, new Properties());
    }
    var home = Optional.ofNullable(path.getParent()).orElse(Path.of(""));
    return of(home, Util.loadProperties(path));
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

  final MainRealm main;
  final TestRealm test;

  private Project(String name, Version version, Paths paths, Options options) {
    this.name = name;
    this.version = version;
    this.paths = paths;
    this.options = options;

    this.main = new MainRealm();
    this.test = new TestRealm(main);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("version=" + version)
        .add("paths=" + paths)
        .add("options=" + options)
        .add("main=" + main)
        .add("test=" + test)
        .toString();
  }

  /** Emit useful information via the given consumer. */
  public void toStrings(Consumer<String> consumer) {
    consumer.accept(String.format("Project %s %s", name, version));
    consumer.accept(String.format("Modules = %s", options.modules));
    main.toStrings(consumer);
    test.toStrings(consumer);
  }

  public abstract class Realm {
    final String name;
    final String moduleSourcePath;
    final Map<String, ModuleDescriptor> declaredModules;
    final Set<String> externalModules;

    Realm(String name) {
      this.name = name;

      var moduleSourcePaths = new TreeSet<String>();
      var modules = new TreeMap<String, ModuleDescriptor>();
      var declarations = Util.find(Util::isModuleInfo, paths.sources);
      for (var declaration : declarations) {
        //  <module>/<realm>/.../module-info.java
        var relative = paths.sources.relativize(declaration);
        var module = relative.getName(0).toString();
        var realm = relative.getName(1).toString();
        if (!options.modules.contains(module)) {
          continue; // module not selected in this project's configuration
        }
        if (!name.equals(realm)) {
          continue; // not our realm
        }
        var descriptor = Modules.parseDeclaration(declaration);
        assert module.equals(descriptor.name()) : module + " expected, but got: " + descriptor;
        modules.put(module, descriptor);
        var offset = relative.subpath(1, relative.getNameCount() - 1).toString();
        moduleSourcePaths.add(String.join(File.separator, paths.sources.toString(), "*", offset));
      }
      this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
      this.declaredModules = Collections.unmodifiableMap(modules);
      this.externalModules = Modules.findExternalModuleNames(modules.values());
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Realm.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("moduleSourcePath='" + moduleSourcePath + "'")
          .add("declaredModules=" + declaredModules)
          .add("externalModules=" + externalModules)
          .toString();
    }

    /** Emit useful information via the given consumer. */
    void toStrings(Consumer<String> consumer) {
      consumer.accept(String.format("Realm '%s'", name));
      consumer.accept(String.format("Declared modules = %s", declaredModules.keySet()));
      consumer.accept(String.format("External modules = %s", externalModules));
    }
  }

  class MainRealm extends Realm {

    MainRealm() {
      super("main");
    }
  }

  class TestRealm extends Realm {

    final MainRealm main;

    TestRealm(MainRealm main) {
      super("test");
      this.main = main;
    }
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
