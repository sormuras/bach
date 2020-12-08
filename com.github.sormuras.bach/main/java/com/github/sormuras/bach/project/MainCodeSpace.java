package com.github.sormuras.bach.project;

import java.nio.file.Path;

/**
 * A space for main modules.
 *
 * @param modules the list of main modules to compile
 * @param modulePaths the list of module paths
 * @param release the Java version (release feature number) to compile for
 * @param jarslug the {@code MODULE-NAME "@" jarslug ".jar"} part of the JAR file name
 * @param launcher the launcher configuration
 * @param features the feature set
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record MainCodeSpace(
    ModuleDeclarations modules,
    ModulePaths modulePaths,
    int release,
    String jarslug,
    Launcher launcher,
    Features features,
    Tweaks tweaks)
    implements CodeSpace {

  /**
   * @param feature the feature to test
   * @return {@code true} if the given feature constant is part of the configured feature set
   */
  public boolean is(Feature feature) {
    return features.set().contains(feature);
  }

  @Override
  public String name() {
    return "main";
  }

  /** @return a path to the classes directory of the main space */
  @Override
  public Path classes() {
    return workspace("classes-" + name(), String.valueOf(release));
  }

  /**
   * @param entry the path (directory or regular file) to resolve
   * @return a path within the given project's documentation workspace
   */
  public Path documentation(String entry) {
    return workspace("documentation", entry);
  }
}
