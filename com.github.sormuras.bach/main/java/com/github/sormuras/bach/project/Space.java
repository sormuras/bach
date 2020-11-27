package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** A nominal space for modules. */
public /*sealed*/ interface Space /*permits MainSpace, TestSpace, PreviewSpace*/ {

  /**
   * Returns the list of module names in this space.
   *
   * @return the list of module names
   */
  List<String> modules();

  /**
   * Returns the list of module source path patterns.
   *
   * @return the list of module source path patterns
   */
  List<String> moduleSourcePaths();

  /**
   * Returns the list of module paths.
   *
   * @return the list of module paths
   */
  List<String> modulePaths();

  /**
   * Returns the possibly empty name of this space.
   *
   * @return an empty string for the main space, else a non-empty name
   */
  String name();

  /**
   * Returns the additional arguments to be passed on a per-tool basis.
   *
   * @return the additional arguments to be passed on a per-tool basis
   */
  Map<String, List<String>> tweaks();

  /**
   * Returns a resolved path.
   *
   * @param entry first path to resolve
   * @param more more paths to resolve
   * @return a resolved path
   */
  default Path workspace(String entry, String... more) {
    return Bach.WORKSPACE.resolve(Path.of(entry, more));
  }

  /**
   * Returns a resolved path.
   *
   * @param release the Java release feature number to resolve
   * @return a resolved path
   */
  default Path classes(int release) {
    return workspace("classes-" + name(), String.valueOf(release));
  }

  /**
   * Returns a resolved path.
   *
   * @param release the Java release feature number to resolve
   * @param module the name of the module to resolve
   * @return a resolved path
   */
  default Path classes(int release, String module) {
    return classes(release).resolve(module);
  }
}
