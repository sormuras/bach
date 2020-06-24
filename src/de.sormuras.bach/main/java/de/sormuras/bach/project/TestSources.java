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

import de.sormuras.bach.tool.Javac;

/** A test module sources descriptor. */
public final class TestSources {

  public static TestSources of() {
    return new TestSources(SourceUnits.of(), Javac.of());
  }

  private final SourceUnits units;
  private final Javac javac;

  public TestSources(SourceUnits units, Javac javac) {
    this.units = units;
    this.javac = javac;
  }

  public String name() {
    return "test";
  }

  public SourceUnits units() {
    return units;
  }

  public Javac javac() {
    return javac;
  }

  public TestSources with(SourceUnit... units) {
    return new TestSources(units().with(units), javac);
  }

  public TestSources with(Javac javac) {
    return new TestSources(units, javac);
  }
}
