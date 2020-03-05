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

/** Tune tool options via side-effects. */
public /*static*/ class Tuner {
  /** Tune any tool options according to the given parameters, which may be {@code null}. */
  public void tune(Tool.Any any, Project project, Realm realm, Unit unit) {}

  /** Tune {@code javac} options according to the given parameters, which may be {@code null}. */
  public void tune(Tool.JavaCompiler javac, Project project, Realm realm) {}
}
