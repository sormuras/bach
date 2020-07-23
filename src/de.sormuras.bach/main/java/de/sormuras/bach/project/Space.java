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

/** A named code space for modular source code units. */
public interface Space<T> {

  /**
   * Return the possibly empty name of the realm.
   *
   * @return An empty string for the main realm, else a non-empty name string
   * @see #title()
   */
  String name();

  /**
   * Return the title of the realm.
   *
   * @return The string {@code "main"} for the main realm, else the non-empty name string
   * @see #name()
   */
  default String title() {
    return name().isEmpty() ? "main" : name();
  }

  /**
   * Return the desired Java release of the realm.
   *
   * @return A {@code JavaRelease} instance
   */
  default Release release() {
    return Release.ofRuntime();
  }

  /**
   * Return the underlying map of modular source units.
   *
   * @return A {@code SourceUnits} instance
   */
  Units units();

  /**
   * Create a new copy instance with setting the given modular source units map.
   *
   * @param units The modular source unit map to set
   * @return A new copy instance
   */
  @Factory(Factory.Kind.SETTER)
  T units(Units units);

  /**
   * Create a new copy instance with the given modular source units added.
   *
   * @param moreUnits Zero or more modular source units to added to the new instance
   * @return A new copy instance
   */
  @Factory(Factory.Kind.OPERATOR)
  default T with(Unit... moreUnits) {
    return units(units().with(moreUnits));
  }
}
