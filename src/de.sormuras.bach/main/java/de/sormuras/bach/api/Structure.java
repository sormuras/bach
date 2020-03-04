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

import java.util.List;

/** Project structure. */
public /*static*/ final class Structure {

  private final Paths paths;
  private final List<Unit> units;
  private final List<Realm> realms;
  private final Tuner tuner;

  public Structure(Paths paths, List<Unit> units, List<Realm> realms, Tuner tuner) {
    this.paths = paths;
    this.units = units;
    this.realms = realms;
    this.tuner = tuner;
  }

  public Paths paths() {
    return paths;
  }

  public List<Unit> units() {
    return units;
  }

  public List<Realm> realms() {
    return realms;
  }

  public Tuner tuner() {
    return tuner;
  }
}
