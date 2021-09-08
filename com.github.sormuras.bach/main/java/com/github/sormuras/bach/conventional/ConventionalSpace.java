package com.github.sormuras.bach.conventional;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.command.ModulePathsOption;
import com.github.sormuras.bach.command.ModuleSourcePathPatternsOption;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

public record ConventionalSpace(
    Optional<String> name,
    Optional<Integer> release,
    List<ConventionalModule> modules,
    ModuleSourcePathPatternsOption moduleSourcePaths,
    ModulePathsOption modulePaths) {

  public static ConventionalSpace of() {
    return new ConventionalSpace().moduleSourcePaths(".");
  }

  public static ConventionalSpace of(String name) {
    var patternJoiner = new StringJoiner(File.separator).add(".").add("*").add(name);
    return new ConventionalSpace()
        .name(name)
        .moduleSourcePaths(patternJoiner.toString(), patternJoiner.add("java").toString());
  }

  public ConventionalSpace() {
    this(
        Optional.empty(),
        Optional.empty(),
        List.of(),
        ModuleSourcePathPatternsOption.empty(),
        ModulePathsOption.empty());
  }

  public ConventionalSpace name(String name) {
    return new ConventionalSpace(
        Optional.ofNullable(name), release, modules, moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace release(Integer release) {
    return new ConventionalSpace(
        name, Optional.ofNullable(release), modules, moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace modules(List<ConventionalModule> modules) {
    return new ConventionalSpace(
        name, release, List.copyOf(modules), moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace modulesAdd(String name) {
    return modulesAdd(name, UnaryOperator.identity());
  }

  public ConventionalSpace modulesAdd(String name, UnaryOperator<ConventionalModule> operator) {
    var modules = new ArrayList<>(this.modules);
    modules.add(operator.apply(ConventionalModule.of(name)));
    return modules(modules);
  }

  public ConventionalSpace moduleSourcePaths(String... patterns) {
    return new ConventionalSpace(
        name, release, modules, new ModuleSourcePathPatternsOption(List.of(patterns)), modulePaths);
  }

  public ConventionalSpace modulePaths(Path... paths) {
    return new ConventionalSpace(
        name, release, modules, moduleSourcePaths, new ModulePathsOption(List.of(paths)));
  }

  public ConventionalBuilder toBuilder(Bach bach) {
    return new ConventionalBuilder(bach, this);
  }

  public List<String> toModuleNames() {
    return modules.stream().map(ConventionalModule::name).toList();
  }
}
