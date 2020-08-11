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

/** A code space for {@code test} modules. */
public final class TestSpace implements CodeSpace<TestSpace> {

  private final CodeUnits units;

  public TestSpace(CodeUnits units) {
    this.units = units;
  }

  public CodeUnits units() {
    return units;
  }

  @Override
  public String name() {
    return "test";
  }

  @Factory
  public static TestSpace of() {
    return new TestSpace(CodeUnits.of());
  }

  @Factory(Factory.Kind.SETTER)
  public TestSpace units(CodeUnits units) {
    return new TestSpace(units);
  }
}
