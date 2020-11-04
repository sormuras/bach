package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Base;
import java.lang.module.ModuleDescriptor.Version;

/**
 * Bach's project model.
 *
 * @param base the base paths
 * @param name the name of the project
 */
public record Project(Base base, String name, Version version, int java) {

  /**
   * Returns a project model based on walking the given base paths.
   *
   * @param base the base paths to walk
   * @return a project model
   */
  public static Project of(Base base) {
    var name = base.toName();
    var version = Version.parse(ProjectInfo.VERSION_ZERO_EA);
    int java = Runtime.version().feature();
    return new Project(base, name, version, java);
  }

  /**
   * Returns a project model based on the given base paths and the project info annoation.
   *
   * @param base the base paths to use
   * @param info the project info annotation
   * @return a project model
   */
  public static Project of(Base base, ProjectInfo info) {
    var name = info.name();
    var version = Version.parse(info.version());
    int java = info.java() != 0 ? info.java() : Runtime.version().feature();
    return new Project(base, name, version, java);
  }
}
