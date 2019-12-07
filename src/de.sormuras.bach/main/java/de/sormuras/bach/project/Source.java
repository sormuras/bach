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

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.Set;

/** Single source path with optional release directive. */
public /*record*/ class Source {

  /** Source-specific modifier enumeration. */
  public enum Modifier {
    /** Store binary assets in {@code META-INF/versions/${release}/} directory of the jar. */
    VERSIONED
  }

  /** Create default non-targeted source for the specified path and optional modifiers. */
  public static Source of(Path path, Modifier... modifiers) {
    return new Source(path, 0, Set.of(modifiers));
  }

  /** Create targeted source for the specified release and path. */
  public static Source of(Path path, int release) {
    return new Source(path, release, Set.of(Modifier.VERSIONED));
  }

  private final Path path;
  private final int release;
  /** Optional modifiers. */
  private final Set<Modifier> modifiers;

  public Source(Path path, int release, Set<Modifier> modifiers) {
    this.path = path;
    this.release = release;
    this.modifiers = Set.copyOf(modifiers);
  }

  /** Source path. */
  public Path path() {
    return path;
  }

  /** Java feature release target number, with zero indicating the current runtime release. */
  public int release() {
    return release;
  }

  /** This source modifiers. */
  public Set<Modifier> modifiers() {
    return modifiers;
  }

  public boolean isTargeted() {
    return release != 0;
  }

  /** Optional Java feature release target number. */
  public OptionalInt target() {
    return isTargeted() ? OptionalInt.of(release) : OptionalInt.empty();
  }

  public boolean isVersioned() {
    return modifiers.contains(Modifier.VERSIONED);
  }

  @Override
  public String toString() {
    return String.format("Source{path=%s, release=%d, modifiers=%s", path, release, modifiers);
  }
}
