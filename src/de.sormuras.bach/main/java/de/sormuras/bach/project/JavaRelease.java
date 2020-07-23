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
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A feature number of a Java SE release.
 *
 * @see Runtime.Version#feature()
 */
public final class JavaRelease {

  private final int feature;

  public JavaRelease(int feature) {
    this.feature = feature;
  }

  public int feature() {
    return feature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JavaRelease that = (JavaRelease) o;
    return feature == that.feature;
  }

  @Override
  public int hashCode() {
    return Objects.hash(feature);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", JavaRelease.class.getSimpleName() + "[", "]")
        .add("feature=" + feature)
        .toString();
  }

  //
  // Factory API
  //

  private static final JavaRelease RUNTIME = of(Runtime.version().feature());

  @Factory
  public static JavaRelease ofRuntime() {
    return RUNTIME;
  }

  @Factory
  public static JavaRelease of(int feature) {
    return new JavaRelease(feature);
  }
}
