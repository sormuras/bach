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

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

  /** Return module source path by combining a module-info.java path and a module name. */
  static String moduleSourcePath(Path info, String module) {
    var names = new ArrayList<String>();
    var found = new AtomicBoolean(false);
    for (var element : info.subpath(0, info.getNameCount() - 1)) {
      var name = element.toString();
      if (name.equals(module)) {
        if (found.getAndSet(true))
          throw new IllegalArgumentException(
              String.format("Name '%s' not unique in path: %s", module, info));
        if (names.isEmpty()) names.add("."); // leading '*' are bad
        if (names.size() < info.getNameCount() - 2) names.add("*"); // avoid trailing '*'
        continue;
      }
      names.add(name);
    }
    if (!found.get())
      throw new IllegalArgumentException(
          String.format("Name of module '%s' not found in path's elements: %s", module, info));
    if (names.isEmpty()) return ".";
    return String.join(File.separator, names);
  }

  /** Return modular origin of the given object. */
  static String origin(Object object) {
    var type = object.getClass();
    var module = type.getModule();
    if (module.isNamed()) return module.getDescriptor().toNameAndVersion();
    try {
      return type.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
    } catch (NullPointerException | URISyntaxException ignore) {
      return module.toString();
    }
  }
}
