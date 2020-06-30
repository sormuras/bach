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

/** Source set of {@code main} modules. */
public final class MainSources {

  public static MainSources of() {
    return new MainSources(JavaRelease.ofRuntime(), SourceUnits.of());
  }

  public MainSources with(JavaRelease release) {
    return new MainSources(release, units);
  }

  public MainSources with(SourceUnits units) {
    return new MainSources(release, units);
  }

  public MainSources with(SourceUnit unit) {
    return with(units.with(unit));
  }

  private final JavaRelease release;
  private final SourceUnits units;

  public MainSources(JavaRelease release, SourceUnits units) {
    this.release = release;
    this.units = units;
  }

  public JavaRelease release() {
    return release;
  }

  public SourceUnits units() {
    return units;
  }
}
