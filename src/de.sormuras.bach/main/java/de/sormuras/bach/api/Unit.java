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

/** A module source description. */
public /*static*/ class Unit {

  public static Unit of(String name, Folder... folders) {
    var descriptor = ModuleDescriptor.newModule(name).build();
    return new Unit(descriptor, List.of(folders));
  }

  private final ModuleDescriptor descriptor;
  private final List<Folder> folders;

  public Unit(ModuleDescriptor descriptor, List<Folder> folders) {
    this.descriptor = descriptor;
    this.folders = folders;
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public List<Folder> folders() {
    return folders;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
        .add("descriptor=" + descriptor)
        .add("folders=" + folders)
        .toString();
  }
}
