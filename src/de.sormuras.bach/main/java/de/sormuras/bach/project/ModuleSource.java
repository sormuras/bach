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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A module source connects a module compilation unit with source directories and resource paths.
 *
 * @see ModuleDescriptor
 */
public final class ModuleSource {

  public static ModuleSource of(Path path) {
    var info = Paths.isModuleInfoJavaFile(path) ? path : path.resolve("module-info.java");
    var parent = info.getParent() != null ? info.getParent() : Path.of(".");
    var moduleDescriptor = Modules.describe(info);
    var sourceDirectory = SourceDirectory.of(parent);
    return new ModuleSource(moduleDescriptor, List.of(sourceDirectory), List.of(), Jar.of());
  }

  private final ModuleDescriptor module;
  private final List<SourceDirectory> sources;
  private final List<Path> resources;
  private final Jar jar;

  public ModuleSource(
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

  public ModuleSource with(ModuleDescriptor module) {
    return new ModuleSource(module, sources, resources, jar);
  }

  public ModuleSource with(SourceDirectory additionalDirectory, SourceDirectory... more) {
    var list = new ArrayList<>(sources);
    list.add(additionalDirectory);
    if (more.length > 0) Collections.addAll(list, more);
    return new ModuleSource(module, list, resources, jar);
  }

  public ModuleSource with(List<Path> resources) {
    return new ModuleSource(module, sources, resources, jar);
  }

  public ModuleSource with(Jar jar) {
    return new ModuleSource(module, sources, resources, jar);
  }
}
