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

/** A record of well-known code spaces. */
public final class Spaces {

  private final MainSpace main;
  private final TestSpace test;
  private final TestSpacePreview preview;

  public Spaces(MainSpace main, TestSpace test, TestSpacePreview preview) {
    this.main = main;
    this.test = test;
    this.preview = preview;
  }

  public MainSpace main() {
    return main;
  }

  public TestSpace test() {
    return test;
  }

  public TestSpacePreview preview() {
    return preview;
  }

  //
  // Configuration API
  //

  @Factory
  public static Spaces of() {
    return new Spaces(MainSpace.of(), TestSpace.of(), TestSpacePreview.of());
  }

  @Factory(Kind.SETTER)
  public Spaces main(MainSpace main) {
    return new Spaces(main, test, preview);
  }

  @Factory(Kind.SETTER)
  public Spaces test(TestSpace test) {
    return new Spaces(main, test, preview);
  }

  @Factory(Kind.SETTER)
  public Spaces preview(TestSpacePreview preview) {
    return new Spaces(main, test, preview);
  }

  //
  // Normal API
  //

  public boolean isEmpty() {
    return main.units().isEmpty() && test.units().isEmpty() && preview.units().isEmpty();
  }
}
