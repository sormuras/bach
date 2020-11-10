package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import com.github.sormuras.bach.project.Base;
import com.github.sormuras.bach.project.Library;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.TestSpace;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Bach's project model.
 *
 * @param base the base paths
 * @param name the name of the project
 * @param version the version of the project
 * @param main the main module space
 * @param test the test module space
 */
public record Project(Base base, String name, Version version, Library library, MainSpace main, TestSpace test) {

  /**
   * Returns a copy of this project with the given version.
   *
   * @param version the new version component
   * @return a copy of this project with the given version
   */
  public Project with(Version version) {
    return new Project(base, name, version, library, main, test);
  }

  /**
   * Returns a project model based on walking the given base paths.
   *
   * @param base the base paths to walk
   * @return a project model
   */
  public static Project of(Base base) {
    return of(base, base.name(), Bach.class.getModule().getAnnotation(ProjectInfo.class));
  }

  /**
   * Returns a project model based on the given base paths and the project info annoation.
   *
   * @param base the base paths to use
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(Base base, ProjectInfo info) {
    return of(base, info.name(), info);
  }

  static Project of(Base base, String name, ProjectInfo info) {
    var main = info.main();
    var mainModuleInfoFinder = mainModuleInfoFinder(base, info);
    var test = info.test();
    var testModuleInfoFinder = ModuleInfoFinder.of(base.directory(), test.moduleSourcePaths());

    var finders = ModuleFinder.compose(mainModuleInfoFinder, testModuleInfoFinder);
    var requires = new TreeSet<>(List.of(info.library().requires()));
    requires.addAll(Modules.required(finders));
    requires.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    requires.removeAll(Modules.declared(finders));
    var library = new Library(requires, Map.of());

    return new Project(
        base,
        name,
        Version.parse(info.version()),
        library,
        new MainSpace(
            modules(main.modules(), mainModuleInfoFinder),
            mainModuleInfoFinder.moduleSourcePaths(),
            List.of(main.modulePaths()),
            release(main.release()),
            main.generateApiDocumentation(),
            tweaks(main.tweaks())),
        new TestSpace(
            modules(test.modules(), testModuleInfoFinder),
            List.of(test.moduleSourcePaths()),
            List.of(test.modulePaths()),
            tweaks(test.tweaks())));
  }

  static ModuleInfoFinder mainModuleInfoFinder(Base base, ProjectInfo info) {
    var finder = ModuleInfoFinder.of(base.directory(), info.main().moduleSourcePaths());
    if (finder.findAll().isEmpty()) return ModuleInfoFinder.of(base.directory(), ".");
    return finder;
  }

  static List<String> modules(String[] modules, ModuleFinder finder) {
    var all = modules.length == 1 && modules[0].equals("*");
    return all ? List.copyOf(Modules.declared(finder)) : List.of(modules);
  }

  static int release(int release) {
    return release != 0 ? release : Runtime.version().feature();
  }

  static Map<String, List<String>> tweaks(ProjectInfo.Tweak... tweaks) {
    var map = new HashMap<String, List<String>>();
    for (var tweak : tweaks) map.put(tweak.tool(), List.of(tweak.args()));
    return Map.copyOf(map);
  }
}
