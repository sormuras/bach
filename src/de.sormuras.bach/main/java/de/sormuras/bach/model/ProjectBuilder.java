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

package de.sormuras.bach.model;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

/** Project model builder. */
public /*static*/ final class ProjectBuilder {

  private String name;
  private Version version;
  private Paths paths = Paths.of(Path.of(""));

  public Project build() {
    var structure = new Structure(paths);
    return new Project(name, version, structure);
  }

  public ProjectBuilder name(String name) {
    this.name = name;
    return this;
  }

  public ProjectBuilder version(Version version) {
    this.version = version;
    return this;
  }

  public ProjectBuilder version(String version) {
    return version(Version.parse(version));
  }

  public ProjectBuilder paths(Paths paths) {
    this.paths = paths;
    return this;
  }

  public ProjectBuilder paths(Path base) {
    return paths(Paths.of(base));
  }

  public ProjectBuilder paths(String base) {
    return paths(Path.of(base));
  }
}
