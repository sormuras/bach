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

/** A module source space configuration and builder class. */
public record ConventionalSpace(
    Bach bach,
    Optional<String> name,
    Optional<Integer> release,
    List<ConventionalModule> modules,
    ModuleSourcePathPatternsOption moduleSourcePaths,
    ModulePathsOption modulePaths)
    implements ConventionalBuilder {

  public static ConventionalSpace of(Bach bach) {
    return new ConventionalSpace(bach).moduleSourcePaths(".");
  }

  public static ConventionalSpace of(Bach bach, String name) {
    var patternJoiner = new StringJoiner(File.separator).add(".").add("*").add(name);
    return new ConventionalSpace(bach)
        .name(name)
        .moduleSourcePaths(patternJoiner.toString(), patternJoiner.add("java").toString());
  }

  public ConventionalSpace(Bach bach) {
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
  public ConventionalSpace space() {
    return this;
  }

  public ConventionalSpace name(String name) {
    return new ConventionalSpace(
        bach, Optional.ofNullable(name), release, modules, moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace release(Integer release) {
    return new ConventionalSpace(
        bach, name, Optional.ofNullable(release), modules, moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace modules(List<ConventionalModule> modules) {
    return new ConventionalSpace(
        bach, name, release, List.copyOf(modules), moduleSourcePaths, modulePaths);
  }

  public ConventionalSpace modulesAddModule(String name) {
    return modulesAddModule(name, UnaryOperator.identity());
  }

  public ConventionalSpace modulesAddModule(
      String name, UnaryOperator<ConventionalModule> operator) {
    var modules = new ArrayList<>(this.modules);
    modules.add(operator.apply(ConventionalModule.of(name)));
    return modules(modules);
  }

  public ConventionalSpace moduleSourcePaths(String... patterns) {
    return new ConventionalSpace(
        bach,
        name,
        release,
        modules,
        new ModuleSourcePathPatternsOption(List.of(patterns)),
        modulePaths);
  }

  public ConventionalSpace modulePaths(Path... paths) {
    return new ConventionalSpace(
        bach, name, release, modules, moduleSourcePaths, new ModulePathsOption(List.of(paths)));
  }

  public List<String> toModuleNames() {
    return modules.stream().map(ConventionalModule::name).toList();
  }
}
