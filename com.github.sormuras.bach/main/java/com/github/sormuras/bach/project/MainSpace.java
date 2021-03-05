package com.github.sormuras.bach.project;

/**
 * A code space for main modules.
 *
 * @param declarations the list of main modules to compile
 * @param modulePaths the list of module paths
 * @param release the Java version (release feature number) to compile for
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record MainSpace(
    ModuleDeclarations declarations, ModulePaths modulePaths, int release, Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "main";
  }
}
