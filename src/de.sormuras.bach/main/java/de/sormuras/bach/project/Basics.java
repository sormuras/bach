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

/** A name, version, and other basic project properties holder. */
public final class Basics {

  public static Basics of(String name, String version) {
    return new Basics(name, Version.parse(version), JavaRelease.ofRuntime());
  }

  private final String name;
  private final Version version;
  private final JavaRelease release;

  public Basics(String name, Version version, JavaRelease release) {
    this.name = name;
    this.version = version;
    this.release = release;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public JavaRelease release() {
    return release;
  }

  public Basics with(Version version) {
    return new Basics(name, version, release);
  }

  public Basics with(JavaRelease release) {
    return new Basics(name, version, release);
  }
}
