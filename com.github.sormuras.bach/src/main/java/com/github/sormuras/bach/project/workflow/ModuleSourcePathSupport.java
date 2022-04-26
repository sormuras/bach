package com.github.sormuras.bach.project.workflow;

import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ModuleSourcePathSupport {

  public static List<String> compute(Map<String, List<Path>> map, boolean forceSpecificForm) {
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var entry : map.entrySet()) {
      var name = entry.getKey();
      var paths = entry.getValue();
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
    return Stream.concat(
            patterns.stream(),
            specific.entrySet().stream().map(e -> toSpecificForm(e.getKey(), e.getValue())))
        .toList();
  }

  public static String toPatternForm(Path info, String module) {
    var root = info.getRoot();
    var deque = new ArrayDeque<String>();
    if (root != null) deque.add(root.toString());
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

  public static String toSpecificForm(String module, List<Path> paths) {
    return module
        + '='
        + paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }
}
