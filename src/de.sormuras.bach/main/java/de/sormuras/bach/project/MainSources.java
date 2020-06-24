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
import de.sormuras.bach.tool.Javadoc;

/** A main module sources descriptor. */
public final class MainSources {

  public static MainSources of() {
    return new MainSources(SourceUnits.of(), Javac.of(), Javadoc.of());
  }

  private final SourceUnits units;
  private final Javac javac;
  private final Javadoc javadoc;

  public MainSources(SourceUnits units, Javac javac, Javadoc javadoc) {
    this.units = units;
    this.javac = javac;
    this.javadoc = javadoc;
  }

  public SourceUnits units() {
    return units;
  }

  public Javac javac() {
    return javac;
  }

  public Javadoc javadoc() {
    return javadoc;
  }

  public MainSources with(SourceUnit... units) {
    return new MainSources(units().with(units), javac, javadoc);
  }

  public MainSources with(Javac javac) {
    return new MainSources(units, javac, javadoc);
  }

  public MainSources with(Javadoc javadoc) {
    return new MainSources(units, javac, javadoc);
  }
}
