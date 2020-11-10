package com.github.sormuras.bach.project;

import java.util.List;
import java.util.Map;

/**
 * A space for test modules.
 *
 * @param modules the list of modules to compile
 * @param moduleSourcePaths the list of module source path patterns
 * @param modulePaths the list of module paths
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record TestSpace(
    List<String> modules,
    List<String> moduleSourcePaths,
    List<String> modulePaths,
    Map<String, List<String>> tweaks)
    implements Space {

  @Override
  public String name() {
    return "test";
  }
}
