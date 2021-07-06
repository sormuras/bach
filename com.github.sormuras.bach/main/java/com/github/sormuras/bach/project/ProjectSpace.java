package com.github.sormuras.bach.project;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public record ProjectSpace(
    String name,
    String suffix,
    Set<DeclaredModule> modules,
    Optional<JavaRelease> release,
    Optional<ModuleSourcePaths> moduleSourcePaths,
    Optional<ModulePatches> modulePatches,
    Optional<ModulePaths> modulePaths) {

  public ProjectSpace(String name, String suffix) {
    this(
        name,
        suffix,
        Set.of(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public ProjectSpace with(DeclaredModule module) {
    var modules = new TreeSet<>(this.modules);
    modules.add(module);
    return new ProjectSpace(
        name, suffix, modules, release, moduleSourcePaths, modulePatches, modulePaths);
  }

  public ProjectSpace with(JavaRelease release) {
    return new ProjectSpace(
        name,
        suffix,
        modules,
        Optional.ofNullable(release),
        moduleSourcePaths,
        modulePatches,
        modulePaths);
  }

  public ProjectSpace with(ModuleSourcePaths moduleSourcePaths) {
    return new ProjectSpace(
        name,
        suffix,
        modules,
        release,
        Optional.ofNullable(moduleSourcePaths),
        modulePatches,
        modulePaths);
  }

  public ProjectSpace with(ModulePatches modulePatches) {
    return new ProjectSpace(
        name,
        suffix,
        modules,
        release,
        moduleSourcePaths,
        Optional.ofNullable(modulePatches),
        modulePaths);
  }

  public ProjectSpace with(ModulePaths modulePaths) {
    return new ProjectSpace(
        name,
        suffix,
        modules,
        release,
        moduleSourcePaths,
        modulePatches,
        Optional.ofNullable(modulePaths));
  }

  public ProjectSpace withModule(String path) {
    return with(DeclaredModule.of(path));
  }

  public ProjectSpace withJavaRelease(int feature) {
    return with(new JavaRelease(feature));
  }

  public ProjectSpace withModuleSourcePaths(String... patterns) {
    return with(ModuleSourcePaths.ofPatterns(patterns));
  }

  public ProjectSpace withPatchModule(String module, String... paths) {
    return with(modulePatches.orElseGet(ModulePatches::of).with(module, paths));
  }

  public ProjectSpace withModulePaths(String... paths) {
    return with(ModulePaths.of(paths));
  }
}
