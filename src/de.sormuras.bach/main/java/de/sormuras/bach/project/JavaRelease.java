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

/**
 * A specific Java SE release feature number.
 *
 * @see Runtime.Version#feature()
 */
public final class JavaRelease {

  private static final JavaRelease RUNTIME = of(Runtime.version().feature());

  public static JavaRelease of(int feature) {
    return new JavaRelease(feature);
  }

  public static JavaRelease ofRuntime() {
    return RUNTIME;
  }

  private final int feature;

  public JavaRelease(int feature) {
    this.feature = feature;
  }

  public int feature() {
    return feature;
  }
}
