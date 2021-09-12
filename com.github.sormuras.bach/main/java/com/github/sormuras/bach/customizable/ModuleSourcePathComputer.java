package com.github.sormuras.bach.customizable;

import com.github.sormuras.bach.command.ModuleSourcePathPatternsOption;
import com.github.sormuras.bach.command.ModuleSourcePathSpecificsOption;
import com.github.sormuras.bach.project.FolderType;
import com.github.sormuras.bach.project.ProjectSpace;
import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/** Computes option usable as module source path arguments. */
record ModuleSourcePathComputer(
    ModuleSourcePathPatternsOption patterns, ModuleSourcePathSpecificsOption specifics) {

  static ModuleSourcePathComputer compute(ProjectSpace space) {
    return ModuleSourcePathComputer.compute(space, false);
  }

  static ModuleSourcePathComputer compute(ProjectSpace space, boolean forceSpecificForm) {
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var module : space.modules()) {
      var name = module.name();
      var paths = module.folders().list(0, FolderType.SOURCES);
      if (forceSpecificForm) {
        specific.put(name, paths);
        continue;
      }
      try {
        for (var path : paths) {
          patterns.add(toPatternForm(path, name));
        }
      } catch (FindException e) {
        specific.put(name, paths);
      }
    }
    var patternsOption = ModuleSourcePathPatternsOption.of(patterns);
    var specificsOption = ModuleSourcePathSpecificsOption.of(specific);
    return new ModuleSourcePathComputer(patternsOption, specificsOption);
  }

  static String toPatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
  }
}
