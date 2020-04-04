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

package de.sormuras.bach.project.structure;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/** A modular source description wrapping a module descriptor and associated source directories. */
public /*static*/ class Unit {

  private final ModuleDescriptor descriptor;
  private final List<Directory> directories;

  public Unit(ModuleDescriptor descriptor, List<Directory> directories) {
    this.descriptor = descriptor;
    this.directories = directories;
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public List<Directory> directories() {
    return directories;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
        .add("descriptor=" + descriptor)
        .add("directories=" + directories)
        .toString();
  }

  public String name() {
    return descriptor.name();
  }

  public List<String> toRequiresNames() {
    var names =
        descriptor.requires().stream()
            .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
            .map(Requires::name);
    return names.sorted().collect(Collectors.toList());
  }
}
