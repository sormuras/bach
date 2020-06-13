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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A modular project structure. */
public final class Structure {

  /**
   * Return a new {@code Structure} instance with default component values.
   *
   * @return A new default structure object
   */
  public static Structure of() {
    return new Structure(Paths.of(), Map.of());
  }

  private final Paths paths;
  private final Map<String, Locator> locators;

  /**
   * Initializes a new structure instance with the given component values.
   *
   * @param paths The paths instance
   * @param locators The locator map
   */
  public Structure(Paths paths, Map<String, Locator> locators) {
    this.paths = paths;
    this.locators = Map.copyOf(locators);
  }

  /**
   * Return the {@code Paths} instance of this project.
   *
   * @return A paths object
   */
  public Paths paths() {
    return paths;
  }

  /**
   * Return the locators map of this project.
   *
   * @return A map of locator
   */
  public Map<String, Locator> locators() {
    return locators;
  }

}
