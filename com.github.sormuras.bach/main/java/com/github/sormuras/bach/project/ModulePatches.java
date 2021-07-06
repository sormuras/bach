package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public record ModulePatches(Map<String, List<Path>> map) {

  public static ModulePatches of() {
    return new ModulePatches(Map.of());
  }

  public ModulePatches with(String module, String... paths) {
    var map = new TreeMap<>(this.map);
    map.put(module, Stream.of(paths).map(Path::of).toList());
    return new ModulePatches(map);
  }
}
