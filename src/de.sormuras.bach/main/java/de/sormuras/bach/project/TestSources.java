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

import de.sormuras.bach.Scribe;
import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;

/** Source set of {@code test} modules. */
public final class TestSources implements Realm<TestSources>, Scribe {

  private final SourceUnitMap units;

  public TestSources(SourceUnitMap units) {
    this.units = units;
  }

  public SourceUnitMap units() {
    return units;
  }

  @Override
  public String name() {
    return "test";
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

  @Override
  public void scribe(Scroll scroll) {
    scroll.append("TestSources.of()");
    scroll.addNewLine().add(".units", units);
  }
}
