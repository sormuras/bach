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

package de.sormuras.bach.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Module-related utilities. */
public /*static*/ class Modules {

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

  /** Return name of main class of the specified module. */
  public static Optional<String> findMainClass(Path info, String module) {
    var main = Path.of(module.replace('.', '/'), "Main.java");
    var exists = Files.isRegularFile(info.resolveSibling(main));
    return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
  }

  /** Return name of the main module by finding a single main class containing descriptor. */
  public static Optional<String> findMainModule(Stream<ModuleDescriptor> descriptors) {
    var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
    return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
  }

  /** Parse module definition from the given file. */
  public static ModuleDescriptor describe(Path info) {
    try {
      var module = describe(Files.readString(info));
      var temporary = module.build();
      findMainClass(info, temporary.name()).ifPresent(module::mainClass);
      return module.build();
    } catch (IOException e) {
      throw new UncheckedIOException("Describe failed", e);
    }
  }

  /** Parse module definition from the given file. */
  public static ModuleDescriptor.Builder describe(String source) {
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

  public static Set<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  /** Return distinct names of the given descriptors. */
  public static Set<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public static Set<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  /** Return distinct names of the required modules of each given descriptor. */
  public static Set<String> required(Stream<ModuleDescriptor> descriptors) {
    return descriptors
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
        .map(Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Modules() {}
}
