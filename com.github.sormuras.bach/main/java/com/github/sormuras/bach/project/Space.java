package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Project;
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
   * @see #title()
   */
  String name();

  /**
   * Returns the additional arguments to be passed on a per-tool basis.
   *
   * @return the additional arguments to be passed on a per-tool basis
   */
  Map<String, List<String>> tweaks();

  /**
   * Returns the title of this space.
   *
   * @return The string {@code "main"} for the main space, else the non-empty name
   * @see #name()
   */
  default String title() {
    return name().isEmpty() ? "main" : name();
  }

  /**
   * @param project the project
   * @return a path to the classes directory of this space
   */
  default Path classes(Project project) {
    return project.base().classes(name(), Runtime.version().feature());
  }
}
