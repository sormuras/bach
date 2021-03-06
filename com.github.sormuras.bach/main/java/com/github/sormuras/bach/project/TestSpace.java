package com.github.sormuras.bach.project;

/**
 * A code space for test modules.
 *
 * @param declarations the list of test modules to compile
 * @param modulePaths the list of module paths
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record TestSpace(ModuleDeclarations declarations, ModulePaths modulePaths, Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "test";
  }
}
