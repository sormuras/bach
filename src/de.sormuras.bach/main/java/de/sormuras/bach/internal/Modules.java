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

package de.sormuras.bach.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Module-related utilities. */
public interface Modules {

  /**
   * Source patterns matching parts of "Module Declarations" grammar.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-7.html#jls-7.7">Module
   *     Declarations</>
   */
  interface Patterns {
    /** Match {@code `module Identifier {. Identifier}`} snippets. */
    Pattern NAME =
        Pattern.compile(
            "(?:module)" // key word
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
                + "\\s*\\{"); // end marker

    /** Match {@code `requires {RequiresModifier} ModuleName ;`} snippets. */
    Pattern REQUIRES =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + "\\s*;"); // end marker
  }

  /** Module descriptor parser. */
  static ModuleDescriptor describe(Path info) {
    try {
      return newModule(Files.readString(info)).build();
    } catch (Exception e) {
      throw new RuntimeException("Describe failed", e);
    }
  }

  /** Module descriptor parser. */
  static ModuleDescriptor.Builder newModule(String source) {
    // `module Identifier {. Identifier}`
    var nameMatcher = Patterns.NAME.matcher(source);
    if (!nameMatcher.find())
      throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
    var name = nameMatcher.group(1).trim();
    var builder = ModuleDescriptor.newModule(name);
    // "requires module /*version*/;"
    var requiresMatcher = Patterns.REQUIRES.matcher(source);
    while (requiresMatcher.find()) {
      var requiredName = requiresMatcher.group(1);
      Optional.ofNullable(requiresMatcher.group(2))
          .ifPresentOrElse(
              version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
              () -> builder.requires(requiredName));
    }
    return builder;
  }
}
