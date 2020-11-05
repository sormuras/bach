package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Base;
import com.github.sormuras.bach.project.MainSpace;
import java.lang.module.ModuleDescriptor.Version;
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
   * Returns a project model based on walking the given base paths.
   *
   * @param base the base paths to walk
   * @return a project model
   */
  public static Project of(Base base) {
    var info = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    return new Project(
        base,
        base.toName(), // name of base directory
        Version.parse(info.version()),
        new MainSpace(
            List.of(), // TODO Scan directory tree for module names...
            List.of(info.main().moduleSourcePaths()),
            Runtime.version().feature(),
            mapOf(info.main().tweaks())) //
        );
  }

  /**
   * Returns a project model based on the given base paths and the project info annoation.
   *
   * @param base the base paths to use
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(Base base, ProjectInfo info) {
    return new Project(
        base,
        info.name(),
        Version.parse(info.version()),
        new MainSpace(
            List.of(info.main().modules()),
            List.of(info.main().moduleSourcePaths()),
            release(info.main().release()),
            mapOf(info.main().tweaks())) //
        );
  }

  private static int release(int release) {
    return release != 0 ? release : Runtime.version().feature();
  }

  private static Map<String, List<String>> mapOf(ProjectInfo.Tweak... tweaks) {
    var map = new HashMap<String, List<String>>();
    for (var tweak : tweaks) map.put(tweak.tool(), List.of(tweak.args()));
    return Map.copyOf(map);
  }
}
