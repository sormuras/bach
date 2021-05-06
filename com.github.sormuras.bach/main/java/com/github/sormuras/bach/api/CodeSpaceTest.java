package com.github.sormuras.bach.api;

public record CodeSpaceTest(DeclaredModuleFinder modules, ModulePaths paths) {
  public static CodeSpaceTest empty() {
    return new CodeSpaceTest(DeclaredModuleFinder.of(), ModulePaths.of());
  }
}
