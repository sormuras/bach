/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public /*record*/ class Unit {
  private final Realm realm;
  private final ModuleDescriptor descriptor;
  private final List<Path> patches;

  public Unit(Realm realm, ModuleDescriptor descriptor, List<Path> patches) {
    this.realm = realm;
    this.descriptor = descriptor;
    this.patches = patches;
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public String name() {
    return descriptor.name();
  }

  public Realm realm() {
    return realm;
  }

  public List<Path> patches() {
    return patches;
  }

  @Override
  public String toString() {
    return descriptor.toNameAndVersion();
  }

  public List<Path> resources() {
    var resources = new ArrayList<Path>();
    for (var source : realm.sourcePaths()) {
      var directory = source.getParent().resolve("resources");
      var path = Path.of(directory.toString().replace("{MODULE}", name()));
      resources.add(path);
    }
    return List.copyOf(resources);
  }
}
