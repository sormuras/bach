package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import com.github.sormuras.bach.module.ModuleInfoReference;
import com.github.sormuras.bach.project.Library;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.TestSpace;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Bach's project model.
 *
 * <h2>Directory Tree Example</h2>
 *
 * <pre>{@code
 * directory --> jigsaw-quick-start
 *               ├───.bach
 *               │   ├───build
 *               │   │       module-info.java
 *               │   ├───cache
 *               │   │       com.github.sormuras.bach@16.jar
 *               │   │       (more tools and plugins...)
 * libraries --> │   ├───libraries
 *               │   │       org.junit.jupiter.jar
 *               │   │       (more external modules...)
 * workspace --> │   └───workspace
 *               │       ├───classes
 *               │       │   └───11
 *               │       │       ├───com.greetings
 *               │       │       │       module-info.class
 *               │       │       └───com
 *               │       │           └───greetings
 *               │       │                   Main.class
 *               │       ├───classes-test
 *               │       ├───documentation
 *               │       ├───image
 *               │       ├───modules
 *               │       │       com.greetings@2020.jar
 *               │       ├───modules-test
 *               │       ├───reports
 *               │       └───sources
 *               └───com.greetings
 *                   │   module-info.java
 *                   └───com
 *                       └───greetings
 *                               Main.java
 * }</pre>
 *
 * @param name the name of the project
 * @param version the version of the project
 * @param library the external modules manager
 * @param main the main module space
 * @param test the test module space
 */
public record Project(String name, Version version, Library library, MainSpace main, TestSpace test) {

  /** @return a stream of all module names */
  public Stream<String> findAllModuleNames() {
    return Stream.concat(main.modules().stream(), test.modules().stream());
  }

  /** Path to directory with external modules. */
  public static final Path LIBRARIES = Path.of(".bach/libraries");

  /** Path to directory that collects all generated assets. */
  public static final Path WORKSPACE = Path.of(".bach/workspace");

  /**
   * Returns a project model based on walking the current workding directory.
   *
   * @return a project model
   */
  public static Project of() {
    var name = Paths.name(Path.of(""), "unnamed");
    var info = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    return of(name, info);
  }

  /**
   * Returns a project model based on the current workding directory and the given info annotation.
   *
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(ProjectInfo info) {
    return of(info.name(), info);
  }

  static Project of(String name, ProjectInfo info) {
    var version = version(info.version());

    var main = info.main();
    var mainModuleInfoFinder = mainModuleInfoFinder(main.moduleSourcePaths());
    var test = info.test();
    var testModuleInfoFinder = ModuleInfoFinder.of(Path.of(""), test.moduleSourcePaths());

    var finders = ModuleFinder.compose(mainModuleInfoFinder, testModuleInfoFinder);
    var requires = new TreeSet<>(List.of(info.library().requires()));
    requires.addAll(Modules.required(finders));
    requires.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    requires.removeAll(Modules.declared(finders));
    var library = new Library(requires, Map.of());

    return new Project(
        name(name),
        version,
        library,
        new MainSpace(
            modules(main.modules(), mainModuleInfoFinder),
            mainModuleInfoFinder.moduleSourcePaths(),
            List.of(main.modulePaths()),
            release(main.release()),
            jarslug(version),
            main.generateApiDocumentation(),
            tweaks(main.tweaks())),
        new TestSpace(
            modules(test.modules(), testModuleInfoFinder),
            testModuleInfoFinder.moduleSourcePaths(),
            List.of(test.modulePaths()),
            tweaks(test.tweaks())));
  }

  static String name(String name) {
    return System.getProperty("bach.project.name", name);
  }

  static Version version(String version) {
    return Version.parse(System.getProperty("bach.project.version", version));
  }

  static int release(int release) {
    try {
      return Integer.parseInt(System.getProperty("bach.project.main.release"));
    } catch (RuntimeException ignore) {
      return release != 0 ? release : Runtime.version().feature();
    }
  }

  static String jarslug(Version version) {
    return System.getProperty("bach.project.main.jarslug", version.toString());
  }

  static ModuleInfoFinder mainModuleInfoFinder(String[] moduleSourcePaths) {
    var base = Path.of("");
    // try configured or default module source paths, usually patterns
    var finder = ModuleInfoFinder.of(base, moduleSourcePaths);
    // no module declaration found
    if (finder.findAll().isEmpty()) {
      // try single module declaration or what is "simplicissimus"?
      var info = base.resolve("module-info.java");
      if (Files.exists(info)) return ModuleInfoFinder.of(ModuleInfoReference.of(info));
      // assume modules are declared in directories named like modules
      return ModuleInfoFinder.of(base, ".");
    }
    return finder;
  }

  static List<String> modules(String[] modules, ModuleFinder finder) {
    var all = modules.length == 1 && modules[0].equals("*");
    return all ? List.copyOf(Modules.declared(finder)) : List.of(modules);
  }

  static Map<String, List<String>> tweaks(ProjectInfo.Tweak... tweaks) {
    var map = new HashMap<String, List<String>>();
    for (var tweak : tweaks) map.put(tweak.tool(), List.of(tweak.args()));
    return Map.copyOf(map);
  }
}
