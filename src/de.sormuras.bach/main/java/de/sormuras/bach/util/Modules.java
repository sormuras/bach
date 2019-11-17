/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** {@link Path}-related helpers. */
public class Modules {
  private Modules() {}

  private interface Patterns {
    Pattern MAIN_CLASS = Pattern.compile("//\\s*(?:--main-class)\\s+([\\w.]+)");

    Pattern NAME =
        Pattern.compile(
            "(?:module)" // key word
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
                + "\\s*\\{"); // end marker

    Pattern REQUIRES =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + "\\s*;"); // end marker

    Pattern PROVIDES =
        Pattern.compile(
            "(?:provides)" // key word
                + "\\s+([\\w.]+)" // service name
                + "\\s+with" // separator
                + "\\s+([\\w.,\\s]+)" // comma separated list of type names
                + "\\s*;"); // end marker
  }

  /** Unchecked exception thrown when a module name is not mapped. */
  public static class UnmappedModuleException extends RuntimeException {
    private static final long serialVersionUID = 6985648789039587478L;

    public UnmappedModuleException(String module) {
      super("Module " + module + " is not mapped");
    }
  }

  /** Declared and requires module and optional version holder. */
  public static /*record*/ class Survey {

    public static Survey of(ModuleFinder finder) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      var stream =
          finder.findAll().stream()
              .map(ModuleReference::descriptor)
              .peek(descriptor -> declaredModules.add(descriptor.name()))
              .map(ModuleDescriptor::requires)
              .flatMap(Set::stream)
              .filter(r -> !r.modifiers().contains(Requires.Modifier.STATIC));
      merge(requiredModules, stream);
      return new Survey(declaredModules, requiredModules);
    }

    public static Survey of(String... sources) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      for (var source : sources) {
        var descriptor = describe(source);
        declaredModules.add(descriptor.name());
        merge(requiredModules, descriptor.requires().stream());
      }
      return new Survey(declaredModules, requiredModules);
    }

    static void merge(Map<String, Set<Version>> requiredModules, Stream<Requires> stream) {
      stream
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
          .forEach(
              requires ->
                  requiredModules.merge(
                      requires.name(),
                      requires.compiledVersion().map(Set::of).orElse(Set.of()),
                      Survey::concat));
    }

    static <E extends Comparable<E>> Set<E> concat(Set<E> s, Set<E> t) {
      return Stream.concat(s.stream(), t.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    public static Survey of(Collection<Path> paths) {
      var sources = new ArrayList<String>();
      for (var path : paths) {
        if (Files.isDirectory(path)) {
          path = path.resolve("module-info.java");
        }
        sources.add(Paths.readString(path));
      }
      return of(sources.toArray(new String[0]));
    }

    final Set<String> declaredModules;
    final Map<String, Set<Version>> requiresMap;

    Survey(Set<String> declaredModules, Map<String, Set<Version>> requiresMap) {
      this.declaredModules = declaredModules;
      this.requiresMap = requiresMap;
    }

    public Set<String> declaredModules() {
      return declaredModules;
    }

    public Set<String> requiredModules() {
      return requiresMap.keySet();
    }

    public Optional<Version> requiredVersion(String requiredModule) {
      var versions = requiresMap.get(requiredModule);
      if (versions == null) {
        throw new UnmappedModuleException(requiredModule);
      }
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }
  }

  /** Module descriptor parser. */
  public static ModuleDescriptor describe(String source) {
    // "module name {"
    var nameMatcher = Patterns.NAME.matcher(source);
    if (!nameMatcher.find()) {
      throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
    }
    var name = nameMatcher.group(1).trim();
    var builder = ModuleDescriptor.newModule(name);
    // "// --main-class name"
    var mainClassMatcher = Patterns.MAIN_CLASS.matcher(source);
    if (mainClassMatcher.find()) {
      var mainClass = mainClassMatcher.group(1);
      builder.mainClass(mainClass);
    }
    // "requires module /*version*/;"
    var requiresMatcher = Patterns.REQUIRES.matcher(source);
    while (requiresMatcher.find()) {
      var requiredName = requiresMatcher.group(1);
      Optional.ofNullable(requiresMatcher.group(2))
          .ifPresentOrElse(
              version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
              () -> builder.requires(requiredName));
    }
    // "provides service with type, type, ...;"
    var providesMatcher = Patterns.PROVIDES.matcher(source);
    while (providesMatcher.find()) {
      var providesService = providesMatcher.group(1);
      var providesTypes = providesMatcher.group(2);
      builder.provides(providesService, List.of(providesTypes.trim().split("\\s*,\\s*")));
    }
    return builder.build();
  }
}
