package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.io.File;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record DeclaredModuleFinder(Map<String, DeclaredModule> map) implements ModuleFinder {

  public static DeclaredModuleFinder of(DeclaredModule... modules) {
    var map = Stream.of(modules).collect(Collectors.toMap(DeclaredModule::name, Function.identity()));
    return new DeclaredModuleFinder(map);
  }

  private Optional<DeclaredModule> findDeclaredModule(String name) {
    return Optional.ofNullable(map.get(name));
  }

  @Override
  public Optional<ModuleReference> find(String name) {
    return findDeclaredModule(name).map(DeclaredModule::reference);
  }

  @Override
  public Set<ModuleReference> findAll() {
    return map.values().stream().map(DeclaredModule::reference).collect(Collectors.toSet());
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public int size() {
    return map.size();
  }

  public Stream<String> toNames() {
    return map.keySet().stream().sorted();
  }

  public String toNames(String delimiter) {
    return toNames().collect(Collectors.joining(delimiter));
  }

  public List<String> toModuleSourcePaths(boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var declared : map.values()) {
      var sourcePaths = declared.toModuleSpecificSourcePaths();
      if (forceModuleSpecificForm) {
        specific.put(declared.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) {
          patterns.add(toModuleSourcePathPatternForm(path, declared.name()));
        }
      } catch (FindException e) {
        specific.put(declared.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Strings.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public Map<String, String> toModulePatches(DeclaredModuleFinder upstream) {
    if (map.isEmpty() || upstream.isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var declaration : map.values()) {
      var module = declaration.name();
      upstream
          .findDeclaredModule(module)
          .ifPresent(up -> patches.put(module, up.sources().toModuleSpecificSourcePath()));
    }
    return patches;
  }

  private static String toModuleSourcePathPatternForm(Path info, String module) {
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
