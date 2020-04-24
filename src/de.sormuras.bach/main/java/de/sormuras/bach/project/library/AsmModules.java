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

/** Maven GroupID is {@code org.junit.jupiter}. */
public /*static*/ class AsmModules extends Locator.AbstractLocator {

  public AsmModules() {
    put(
        "org.objectweb.asm",
        Maven.central(
            "org.ow2.asm:asm:8.0.1",
            "org.objectweb.asm",
            121772,
            "72c74304fc162ae3b03e34ca6727e19f"));
    put(
        "org.objectweb.asm.commons",
        Maven.central(
            "org.ow2.asm:asm-commons:8.0.1",
            "org.objectweb.asm.commons",
            71563,
            "7f5ce78ad1745d67fb858a3d4fd491e9"));
    put(
        "org.objectweb.asm.tree",
        Maven.central(
            "org.ow2.asm:asm-tree:8.0.1",
            "org.objectweb.asm.tree",
            52628,
            "0c65ea3d5ca385496462f82153edc05c"));
    put(
        "org.objectweb.asm.tree.analysis",
        Maven.central(
            "org.ow2.asm:asm-analysis:8.0.1",
            "org.objectweb.asm.tree.analysis",
            33438,
            "4c89a09f54c8dff3a0751f7b0f383a20"));
    put(
        "org.objectweb.asm.util",
        Maven.central(
            "org.ow2.asm:asm-util:8.0.1",
            "org.objectweb.asm.util",
            84795,
            "a27e03c8e81310ca238d4aeb5686a5ab"));
  }
}
