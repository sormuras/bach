package com.github.sormuras.bach.simple;

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

/** A simplified module source space configuration and builder class. */
public record SimpleSpace(
    Bach bach,
    Optional<String> name,
    Optional<Integer> release,
    List<SimpleModule> modules,
    ModuleSourcePathPatternsOption moduleSourcePaths,
    ModulePathsOption modulePaths)
    implements SimpleBuilder {

  public static SimpleSpace of(Bach bach) {
    return new SimpleSpace(bach).withModuleSourcePaths(".");
  }

  public static SimpleSpace of(Bach bach, String name) {
    var patternJoiner = new StringJoiner(File.separator).add(".").add("*").add(name);
    return new SimpleSpace(bach)
        .withName(name)
        .withModuleSourcePaths(patternJoiner.toString(), patternJoiner.add("java").toString());
  }

  public SimpleSpace(Bach bach) {
    this(
        bach,
        Optional.empty(),
        Optional.empty(),
        List.of(),
        ModuleSourcePathPatternsOption.empty(),
        ModulePathsOption.empty());
  }

  @Override
  public Bach bach() {
    return bach;
  }

  @Override
  public SimpleSpace space() {
    return this;
  }

  public SimpleSpace withName(String name) {
    return new SimpleSpace(
        bach, Optional.ofNullable(name), release, modules, moduleSourcePaths, modulePaths);
  }

  public SimpleSpace withRelease(Integer release) {
    return new SimpleSpace(
        bach, name, Optional.ofNullable(release), modules, moduleSourcePaths, modulePaths);
  }

  public SimpleSpace withModules(List<SimpleModule> modules) {
    return new SimpleSpace(
        bach, name, release, List.copyOf(modules), moduleSourcePaths, modulePaths);
  }

  public SimpleSpace withModule(String name) {
    return withModule(name, UnaryOperator.identity());
  }

  public SimpleSpace withModule(String name, UnaryOperator<SimpleModule> operator) {
    var modules = new ArrayList<>(this.modules);
    modules.add(operator.apply(SimpleModule.of(name)));
    return withModules(modules);
  }

  public SimpleSpace withModuleSourcePaths(String... patterns) {
    return new SimpleSpace(
        bach,
        name,
        release,
        modules,
        new ModuleSourcePathPatternsOption(List.of(patterns)),
        modulePaths);
  }

  public SimpleSpace withModulePaths(Path... paths) {
    return new SimpleSpace(
        bach, name, release, modules, moduleSourcePaths, new ModulePathsOption(List.of(paths)));
  }

  public List<String> toModuleNames() {
    return modules.stream().map(SimpleModule::name).toList();
  }
}
