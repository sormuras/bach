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

import de.sormuras.bach.project.Locator;

/**
 * Byte Buddy is a code generation and manipulation library.
 *
 * @see <a href="https://bytebuddy.net">bytebuddy.net</a>
 */
public /*static*/ class VariousArtistsModules extends Locator.AbstractLocator {

  public VariousArtistsModules() {
    put(
        "org.apiguardian.api",
        "org.apiguardian:apiguardian-api:1.1.0",
        2387,
        "944805817b648e558ed6be6fc7f054f3");
    put(
        "org.assertj.core",
        Maven.central(
            "org.assertj:assertj-core:3.15.0",
            "org.assertj.core",
            4536021,
            "567e47f8ddde8ec261bd800906c28b92"));
    put(
        "org.opentest4j",
        "org.opentest4j:opentest4j:1.2.0",
        7653,
        "45c9a837c21f68e8c93e85b121e2fb90");
  }
}
