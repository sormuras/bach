package com.github.sormuras.bach.project;

/**
 * A space for test modules with preview features enabled.
 *
 * @param modules the list of test-preview modules to compile
 * @param modulePaths the list of module paths
 * @param tweaks the additional arguments to be passed on a per-tool basis
 */
public record TestPreviewCodeSpace(
    ModuleDeclarations modules, ModulePaths modulePaths, Tweaks tweaks) implements CodeSpace {

  @Override
  public String name() {
    return "test-preview";
  }
}
