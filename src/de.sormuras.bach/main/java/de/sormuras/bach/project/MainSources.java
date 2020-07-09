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
import de.sormuras.bach.internal.Factory.Kind;

/** Source set of {@code main} modules. */
public final class MainSources {

  private final JavaRelease release;
  private final SourceUnitMap units;

  public MainSources(JavaRelease release, SourceUnitMap units) {
    this.release = release;
    this.units = units;
  }

  public JavaRelease release() {
    return release;
  }

  public SourceUnitMap units() {
    return units;
  }

  //
  // Configuration API
  //

  @Factory
  public static MainSources of() {
    return new MainSources(JavaRelease.ofRuntime(), SourceUnitMap.of());
  }

  @Factory(Kind.SETTER)
  public MainSources release(JavaRelease release) {
    return new MainSources(release, units);
  }

  @Factory(Kind.SETTER)
  public MainSources release(int feature) {
    return release(JavaRelease.of(feature));
  }

  @Factory(Kind.SETTER)
  public MainSources units(SourceUnitMap units) {
    return new MainSources(release, units);
  }

  @Factory(Kind.OPERATOR)
  public MainSources with(SourceUnit... moreUnits) {
    return units(units.with(moreUnits));
  }
}
