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

import java.lang.module.ModuleDescriptor.Version;

/** Bach's project model. */
public final class Project {

  public static Project ofSystem() {
    var name = System.getProperty("bach.project.name", "unnamed");
    var version = System.getProperty("bach.project.version", "1-ea");
    return of(name, version);
  }

  public static Project of(String name, String version) {
    return of(name, Version.parse(version));
  }

  public static Project of(String name, Version version) {
    return new Project(Base.of(), name, version);
  }

  public Project with(Base base) {
    return new Project(base, name, version);
  }

  public Project with(String name) {
    return new Project(base, name, version);
  }

  public Project with(Version version) {
    return new Project(base, name, version);
  }

  private final Base base;
  private final String name;
  private final Version version;

  public Project(Base base, String name, Version version) {
    this.base = base;
    this.name = name;
    this.version = version;
  }

  public Base base() {
    return base;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public String toNameAndVersion() {
    return name + ' ' + version;
  }
}
