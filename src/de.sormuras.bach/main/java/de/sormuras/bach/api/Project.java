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
import java.nio.file.Path;
import java.util.Objects;

/** Bach's project model. */
public /*static*/ final class Project {

  private final String name;
  private final Version version;
  private final Structure structure;

  public Project(String name, Version version, Structure structure) {
    this.name = Objects.requireNonNull(name, "name");
    this.version = version;
    this.structure = Objects.requireNonNull(structure, "paths");
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

  public Paths paths() {
    return structure().paths();
  }

  public String toNameAndVersion() {
    if (version == null) return name;
    return name + ' ' + version;
  }

  /** A mutable builder for a {@link Project}. */
  public static class Builder {

    private String name;
    private Version version;
    private Paths paths = Paths.of(Path.of(""));

    public Project build() {
      var structure = new Structure(paths);
      return new Project(name, version, structure);
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(Version version) {
      this.version = version;
      return this;
    }

    public Builder version(String version) {
      return version(Version.parse(version));
    }

    public Builder paths(Paths paths) {
      this.paths = paths;
      return this;
    }

    public Builder paths(Path base) {
      return paths(Paths.of(base));
    }

    public Builder paths(String base) {
      return paths(Path.of(base));
    }
  }
}
