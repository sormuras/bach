package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import com.github.sormuras.bach.project.Base;
import com.github.sormuras.bach.project.MainSpace;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bach's project model.
 *
 * @param base the base paths
 * @param name the name of the project
 * @param version the version of the project
 * @param main the main module space
 */
public record Project(Base base, String name, Version version, MainSpace main) {

  /**
   * Returns a copy of this project with the given version.
   *
   * @param version the new version component
   * @return a copy of this project with the given version
   */
  public Project with(Version version) {
    return new Project(base, name, version, main);
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
    var mainFinder = ModuleInfoFinder.of(base.directory(), List.of(main.moduleSourcePaths()));
    return new Project(
        base,
        name,
        Version.parse(info.version()),
        new MainSpace(
            modules(main.modules(), mainFinder),
            List.of(main.moduleSourcePaths()),
            release(main.release()),
            main.generateApiDocumentation(),
            tweaks(main.tweaks())) //
        );
  }

  static List<String> modules(String[] modules, ModuleFinder finder) {
    return modules.length == 1 && modules[0].equals("*")
        ? List.copyOf(Modules.declared(finder))
        : List.of(modules);
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
