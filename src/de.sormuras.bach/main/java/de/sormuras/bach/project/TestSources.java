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

/** Source set of {@code test} modules. */
public final class TestSources {

  private final SourceUnitMap units;

  public TestSources(SourceUnitMap units) {
    this.units = units;
  }

  public SourceUnitMap units() {
    return units;
  }

  //
  // Configuration API
  //

  @Factory
  public static TestSources of() {
    return new TestSources(SourceUnitMap.of());
  }

  @Factory(Kind.SETTER)
  public TestSources units(SourceUnitMap units) {
    return new TestSources(units);
  }

  @Factory(Kind.OPERATOR)
  public TestSources with(SourceUnit... moreUnits) {
    return units(units.with(moreUnits));
  }
}
