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

import java.lang.module.ModuleDescriptor.Version;
import java.util.StringJoiner;

/** A modular project description. */
public /*static*/ class Project {

  private final String name;
  private final Version version;
  private final Structure structure;

  public Project(String name, Version version, Structure structure) {
    this.name = name;
    this.version = version;
    this.structure = structure;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public Structure structure() {
    return structure;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("version=" + version)
        .add("structure=" + structure)
        .toString();
  }

  public String toNameAndVersion() {
    return name + ' ' + version;
  }
}
