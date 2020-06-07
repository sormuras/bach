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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Module-related utilities. */
public class Modules {

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

  /** Return list of tool providers found by resolving the specified module. */
  public static List<ToolProvider> findTools(String module, List<Path> modulePaths) {
    var roots = Set.of(module);
    var finder = ModuleFinder.of(modulePaths.toArray(Path[]::new));
    var parent = ClassLoader.getPlatformClassLoader();
    try {
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
      var layer = controller.layer();
      var loader = layer.findLoader(module);
      loader.setDefaultAssertionStatus(true);
      var services = ServiceLoader.load(layer, ToolProvider.class);
      return services.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
    } catch (FindException | ResolutionException exception) {
      var message = new StringJoiner(System.lineSeparator());
      message.add(exception.getMessage());
      message.add("Module path:");
      modulePaths.forEach(path -> message.add("\t" + path));
      message.add("Finder finds module(s):");
      finder.findAll().stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> message.add("\t" + reference));
      message.add("");
      throw new RuntimeException(message.toString(), exception);
    }
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

  /** Return the module-pattern form as specified by the {@code --module-source-path} option. */
  public static String modulePatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
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
