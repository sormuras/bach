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

import de.sormuras.bach.internal.Factory;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A name of a Java module.
 *
 * @see Module#getName()
 */
public final class ModuleName implements Comparable<ModuleName> {

  private final String name;

  public ModuleName(String name) {
    this.name = Objects.requireNonNull(name);
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleName that = (ModuleName) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ModuleName.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .toString();
  }

  //
  // Factory API
  //

  @Factory
  public static ModuleName of(String name) {
    return new ModuleName(name);
  }

  //
  // Normal API
  //

  @Override
  public int compareTo(ModuleName other) {
    return name.compareTo(other.name);
  }
}
