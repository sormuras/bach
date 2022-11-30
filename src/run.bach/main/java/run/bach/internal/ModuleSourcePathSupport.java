package run.bach.internal;

import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public interface ModuleSourcePathSupport {

  static List<String> compute(Map<String, List<Path>> map, boolean forceSpecificForm) {
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
    var list = new ArrayList<String>();
    if (!patterns.isEmpty()) list.add(String.join(File.pathSeparator, patterns));
    specific.forEach((module, paths) -> list.add(toSpecificForm(module, paths)));
    return List.copyOf(list);
  }

  static String toPatternForm(Path info, String module) {
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

  static String toSpecificForm(String module, List<Path> paths) {
    var joined = String.join(File.pathSeparator, paths.stream().map(Path::toString).toList());
    return module + '=' + joined;
  }
}
