package com.github.sormuras.bach.api;

public record CodeSpaceMain(
    DeclaredModuleFinder modules, ModulePaths paths, int release, Tweaks tweaks) {

  public static CodeSpaceMain empty() {
    return new CodeSpaceMain(DeclaredModuleFinder.of(), ModulePaths.of(), 0, Tweaks.of());
  }
}
