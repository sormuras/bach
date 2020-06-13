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

import java.net.URI;
import java.util.Optional;

/** A modular Java project descriptor. */
public final class Project {

  public static Project of(String name) {
    return new Project(Basics.of(name, "1-ea"), Structure.of());
  }

  private final Basics basics;
  private final Structure structure;

  public Project(Basics basics, Structure structure) {
    this.basics = basics;
    this.structure = structure;
  }

  public Basics basics() {
    return basics;
  }

  public Structure structure() {
    return structure;
  }

  public Optional<URI> findModuleUri(String module) {
    return Optional.ofNullable(structure.locators().get(module)).map(Locator::uri).map(URI::create);
  }

  public String toNameAndVersion() {
    return basics.name() + ' ' + basics.version();
  }

  public Project with(Basics basics) {
    return new Project(basics, structure);
  }

  public Project with(Structure structure) {
    return new Project(basics, structure);
  }
}
