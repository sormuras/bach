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
public /*static*/ class JUnitPlatformModules extends JUnit5Modules {

  public JUnitPlatformModules() {
    super("org.junit.platform", "1.7.0-M1");
    put(".commons", 99315, "836474af0cda44a23b2b9a78843fdc78");
    put(".console", 447037, "ca70ecade7dc3a52aad8a4612f3493e8");
    put(".engine", 175442, "41482c736ce4dbd5f0916d5c5c8c2311");
    put(".launcher", 128322, "1d5e53d41e15af43f1c343854b1c91c0");
    put(".reporting", 22437, "ff52add0e350b6672c0c42b402fa4b2b");
    put(".testkit", 44977, "da59fda877a5a88ebbdc7c78d7e9cc55");

    put(
        "org.apiguardian.api",
        "org.apiguardian:apiguardian-api:1.1.0",
        2387,
        "944805817b648e558ed6be6fc7f054f3");
    put(
        "org.opentest4j",
        "org.opentest4j:opentest4j:1.2.0",
        7653,
        "45c9a837c21f68e8c93e85b121e2fb90");
  }
}
