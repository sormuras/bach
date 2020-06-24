/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.tool.Jar;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * A module source connects a module compilation unit with source directories and resource paths.
 *
 * @see ModuleDescriptor
 */
public final class SourceUnit {

  public static SourceUnit of(Path path) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var parent = info.getParent() != null ? info.getParent() : Path.of(".");
    var descriptor = Modules.describe(info);
    var directories = directories(parent);
    var resources = resources(parent);
    return new SourceUnit(descriptor, directories, resources, Jar.of());
  }

  public static List<SourceDirectory> directories(Path infoDirectory) {
    if (Paths.isMultiReleaseDirectory(infoDirectory)) {
      var map = new TreeMap<Integer, Path>(); // sorted by release number
      var paths = Paths.list(infoDirectory.getParent(), Files::isDirectory);
      for (var path : paths) Paths.findMultiReleaseNumber(path).ifPresent(n -> map.put(n, path));
      var sources = new ArrayList<SourceDirectory>();
      map.forEach((release, path) -> sources.add(new SourceDirectory(path, release)));
      return List.copyOf(sources);
    }
    var info = new SourceDirectory(infoDirectory, 0); // contains module-info.java file
    var java = infoDirectory.resolveSibling("java");
    if (java.equals(infoDirectory) || Files.notExists(java)) return List.of(info);
    return List.of(new SourceDirectory(java, 0), info);
  }

  public static List<Path> resources(Path infoDirectory) {
    var resources = infoDirectory.resolveSibling("resources");
    return Files.isDirectory(resources) ? List.of(resources) : List.of();
  }

  private final ModuleDescriptor module;
  private final List<SourceDirectory> sources;
  private final List<Path> resources;
  private final Jar jar;

  public SourceUnit(
      ModuleDescriptor module, List<SourceDirectory> sources, List<Path> resources, Jar jar) {
    this.module = module;
    this.sources = List.copyOf(sources);
    this.resources = List.copyOf(resources);
    this.jar = jar;
  }

  public ModuleDescriptor module() {
    return module;
  }

  public List<SourceDirectory> sources() {
    return sources;
  }

  public List<Path> resources() {
    return resources;
  }

  public Jar jar() {
    return jar;
  }

  public String name() {
    return module().name();
  }

  public boolean isMultiRelease() {
    if (sources.isEmpty()) return false;
    if (sources.size() == 1) return sources.get(0).isTargeted();
    return sources.stream().allMatch(SourceDirectory::isTargeted);
  }

  /**
   * Return list of module-relevant source path instances of this unit.
   *
   * @return A list with exactly one or two path objects
   */
  public List<Path> toRelevantSourcePaths() {
    var s0 = sources.get(0);
    if (s0.isModuleInfoJavaPresent()) return List.of(s0.path());
    for (var source : sources)
      if (source.isModuleInfoJavaPresent()) return List.of(s0.path(), source.path());
    throw new IllegalStateException("No module-info.java found in: " + sources);
  }

  public SourceUnit with(ModuleDescriptor module) {
    return new SourceUnit(module, sources, resources, jar);
  }

  public SourceUnit with(SourceDirectory additionalDirectory, SourceDirectory... more) {
    var list = new ArrayList<>(sources);
    list.add(additionalDirectory);
    if (more.length > 0) Collections.addAll(list, more);
    return new SourceUnit(module, list, resources, jar);
  }

  public SourceUnit with(List<Path> resources) {
    return new SourceUnit(module, sources, resources, jar);
  }

  public SourceUnit with(Jar jar) {
    return new SourceUnit(module, sources, resources, jar);
  }
}
