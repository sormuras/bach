package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Map;

/** A nominal space for modules. */
public interface Space {

  /**
   * Returns the list of module names in this space.
   *
   * @return the list of module names
   */
  List<String> modules();

  /**
   * Returns the list of module source paths.
   *
   * @return the list of module source paths
   */
  List<String> moduleSourcePaths();

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

  /** @return {@code true} if at least one module name is present in this space */
  default boolean isPresent() {
    return modules().size() >= 1;
  }
}
