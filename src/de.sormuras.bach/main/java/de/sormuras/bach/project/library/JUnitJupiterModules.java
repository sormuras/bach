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

package de.sormuras.bach.project.library;

/** Maven GroupID is {@code org.junit.jupiter}. */
public /*static*/ class JUnitJupiterModules extends JUnit5Modules {

  public JUnitJupiterModules() {
    super("org.junit.jupiter", "5.7.0-M1");
    put("", 6368, "f6673ae24dcccc20f3f6d1b2d9c25a76");
    put(".api", 164447, "8ec22878dc0943e723e23957379820de");
    put(".engine", 208475, "6369e33683685751f3b2f852b4f00a3f");
    put(".params", 562041, "a0ed0a9fd50de8b300d6dded3d145d04");
  }
}
