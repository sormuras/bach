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

package de.sormuras.bach.api;

import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.StringJoiner;

/** A module source description wrapping a module descriptor and associated source directories. */
public /*static*/ class ModuleDescription {

  public static ModuleDescription of(String name, Directory... directories) {
    var descriptor = ModuleDescriptor.newModule(name).build();
    return new ModuleDescription(descriptor, List.of(directories));
  }

  private final ModuleDescriptor descriptor;
  private final List<Directory> directories;

  public ModuleDescription(ModuleDescriptor descriptor, List<Directory> directories) {
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
    return new StringJoiner(", ", ModuleDescription.class.getSimpleName() + "[", "]")
        .add("descriptor=" + descriptor)
        .add("directories=" + directories)
        .toString();
  }
}
