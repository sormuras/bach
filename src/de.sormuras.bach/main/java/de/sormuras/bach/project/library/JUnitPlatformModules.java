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
    super("org.junit.platform", "1.6.2");
    put(".commons", 96675, "827619f760062525354d47befc86ff9b");
    put(".console", 433740, "c86a03b3bc95477ae55453e2d9dc4212");
    put(".engine", 174108, "b41ff34208cb373de0bf954e70c4d78b");
    put(".launcher", 121929, "efed110dfb13f33a7787b16cfbf8cd2e");
    put(".reporting", 22426, "f99152f2cd481166abf64109b3308825");
    put(".testkit", 42956, "d7a063edea927c01d7eb6d45475f675b");

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
