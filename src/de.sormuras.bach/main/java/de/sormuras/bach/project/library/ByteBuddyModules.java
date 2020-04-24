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
public /*static*/ class ByteBuddyModules extends Locator.AbstractLocator {

  public ByteBuddyModules() {
    put(
        "net.bytebuddy",
        Maven.central(
            "net.bytebuddy:byte-buddy:1.10.9",
            "net.bytebuddy",
            3376059,
            "6a6ce042182446a1a9e1ed95921f9b7c"));
    put(
        "net.bytebuddy.agent",
        Maven.central(
            "net.bytebuddy:byte-buddy-agent:1.10.9",
            "net.bytebuddy.agent",
            259219,
            "43dae8cc4a5ff874473056cbff7d88bf"));
  }
}
