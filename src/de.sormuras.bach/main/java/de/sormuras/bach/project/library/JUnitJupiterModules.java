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
    super("org.junit.jupiter", "5.6.2");
    put("", 6359, "f516ecfd11b70dc28a1793ae5e48c6ea");
    put(".api", 154036, "134c39075fcc504a722b1b33432a1111");
    put(".engine", 209317, "34cae629d115762add3318dcc902706f");
    put(".params", 562271, "0dc5639e8cfec8b920869f1ee16746c2");
  }
}
