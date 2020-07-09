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

/** A set of sources. */
public final class Sources {

  private final MainSources mainSources;
  private final TestSources testSources;
  private final TestPreview testPreview;

  public Sources(MainSources mainSources, TestSources testSources, TestPreview testPreview) {
    this.mainSources = mainSources;
    this.testSources = testSources;
    this.testPreview = testPreview;
  }

  public MainSources mainSources() {
    return mainSources;
  }

  public TestSources testSources() {
    return testSources;
  }

  public TestPreview testPreview() {
    return testPreview;
  }

  //
  // Configuration API
  //

  @Factory
  public static Sources of() {
    return new Sources(MainSources.of(), TestSources.of(), TestPreview.of());
  }

  @Factory(Kind.SETTER)
  public Sources mainSources(MainSources mainSources) {
    return new Sources(mainSources, testSources, testPreview);
  }

  @Factory(Kind.SETTER)
  public Sources testSources(TestSources testSources) {
    return new Sources(mainSources, testSources, testPreview);
  }

  @Factory(Kind.SETTER)
  public Sources testPreview(TestPreview testPreview) {
    return new Sources(mainSources, testSources, testPreview);
  }

  //
  // Normal API
  //

  public boolean isEmpty() {
    return mainSources.units().isEmpty()
        && testSources.units().isEmpty()
        && testPreview.units().isEmpty();
  }
}
