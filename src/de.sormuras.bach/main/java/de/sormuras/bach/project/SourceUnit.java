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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A source unit connects a module compilation unit with source directories and resource paths.
 *
 * @see ModuleDescriptor
 */
public final class SourceUnit implements Comparable<SourceUnit> {

  private final ModuleDescriptor descriptor;
  private final SourceDirectoryList sources;
  private final List<Path> resources;

  public SourceUnit(
      ModuleDescriptor descriptor, SourceDirectoryList sources, List<Path> resources) {
    this.descriptor = descriptor;
    this.sources = sources;
    this.resources = List.copyOf(resources);
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public SourceDirectoryList sources() {
    return sources;
  }

  public List<Path> resources() {
    return resources;
  }

  //
  // Configuration API
  //

  @Factory
  public static SourceUnit of(Path path) {
    return of(path, 0);
  }

  @Factory
  public static SourceUnit of(Path path, int defaultJavaRelease) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var descriptor = Modules.describe(info);
    var parent = info.getParent() != null ? info.getParent() : Path.of(".");
    var directories = SourceDirectoryList.of(parent, defaultJavaRelease);
    var resources = resources(parent);
    return new SourceUnit(descriptor, directories, resources);
  }

  static List<Path> resources(Path infoDirectory) {
    var resources = infoDirectory.resolveSibling("resources");
    return Files.isDirectory(resources) ? List.of(resources) : List.of();
  }

  //
  // Normal API
  //

  @Override
  public int compareTo(SourceUnit other) {
    return name().compareTo(other.name());
  }

  public String name() {
    return descriptor().name();
  }

  public List<SourceDirectory> directories() {
    return sources.list();
  }
}
