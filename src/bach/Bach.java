// THIS FILE WAS GENERATED ON 2019-08-29T17:49:34.831695200Z
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

// default package

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default properties.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var home = Path.of("");
    var work = Path.of("bin");
    return new Bach(out, err, home, work);
  }

  public static void main(String... args) {
    var bach = Bach.of();
    bach.main(args.length == 0 ? List.of("info") : List.of(args));
  }

  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Home directory. */
  private final Path home;
  /** Workspace directory. */
  private final Path work;

  public Bach(PrintWriter out, PrintWriter err, Path home, Path work) {
    this.out = out;
    this.err = err;
    this.home = home;
    this.work = work;
  }

  void main(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      try {
        // Try Bach API method w/o parameter -- single argument is consumed
        var method = Util.findApiMethod(getClass(), argument);
        if (method.isPresent()) {
          method.get().invoke(this);
          continue;
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(argument);
        if (tool.isPresent()) {
          var code = tool.get().run(out, err, arguments.toArray(String[]::new));
          if (code != 0) {
            throw new RuntimeException("Tool " + argument + " returned: " + code);
          }
          return;
        }
      } catch (ReflectiveOperationException e) {
        throw new Error("Reflective operation failed for: " + argument, e);
      }
      throw new IllegalArgumentException("Unsupported argument: " + argument);
    }
  }

  String getBanner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("loading banner resource failed", e);
    }
  }

  public void help() {
    out.println("F1! F1! F1!");
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
    out.printf("  home='%s' -> %s%n", home, home.toUri());
    out.printf("  work='%s'%n", work);
  }

  public void version() {
    out.println(getBanner());
  }

  /** Load required modules. */
  public static class Library {

    public static void main(String... args) {
      System.out.println("Library.main(" + List.of(args) + ")");
      System.out.println("  requires -> " + RequiresMap.of(args));
      var modulePath = new ModulePath(Path.of("demo/lib"));
      System.out.println("--module-path=" + List.of(modulePath.paths));
      System.out.println("  modules  -> " + modulePath.modules());
      System.out.println("  requires -> " + modulePath.requires());
      var sourcePath = new SourcePath(Path.of("demo/src"));
      System.out.println("--module-source-path=" + List.of(sourcePath.paths));
      System.out.println("  modules  -> " + sourcePath.modules());
      System.out.println("  requires -> " + sourcePath.requires());
    }

    static class RequiresMap extends TreeMap<String, Set<Version>> {

      static RequiresMap of(String... strings) {
        var map = new RequiresMap();
        for (var string : strings) {
          var versionMarkerIndex = string.indexOf('@');
          var any = versionMarkerIndex == -1;
          var module = any ? string : string.substring(0, versionMarkerIndex);
          var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
          map.merge(module, any ? Set.of() : Set.of(version));
        }
        return map;
      }

      static <E> Stream<E> merge(Set<E> set1, Set<E> set2) {
        return Stream.concat(set1.stream(), set2.stream()).distinct();
      }

      void merge(ModuleDescriptor.Requires requires) {
        merge(requires.name(), requires.compiledVersion().map(Set::of).orElse(Set.of()));
      }

      void merge(String key, String version) {
        merge(key, version.isEmpty() ? Set.of() : Set.of(Version.parse(version)));
      }

      void merge(String key, Set<Version> value) {
        merge(key, value, (oldSet, newSet) -> Set.of(merge(oldSet, newSet).toArray(Version[]::new)));
      }

      RequiresMap validate() {
        var invalids = entrySet().stream().filter(e -> e.getValue().size() > 1).collect(toList());
        if (invalids.isEmpty()) {
          return this;
        }
        throw new IllegalStateException("Multiple versions mapped: " + invalids);
      }
    }

    static class ModulePath {

      final Path[] paths;

      ModulePath(Path... paths) {
        this.paths = paths;
      }

      Stream<ModuleDescriptor> findAll() {
        return ModuleFinder.of(paths).findAll().stream().map(ModuleReference::descriptor);
      }

      public Set<String> modules() {
        return findAll().map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
      }

      public RequiresMap requires() {
        var map = new RequiresMap();
        findAll().map(ModuleDescriptor::requires).flatMap(Set::stream).forEach(map::merge);
        return map.validate();
      }
    }

    static class SourcePath {

      final Path[] paths;

      SourcePath(Path... paths) {
        this.paths = paths;
      }

      public Set<String> modules() {
        return Set.of();
      }

      public RequiresMap requires() {
        return new RequiresMap();
      }
    }
  }

  /** Static helpers. */
  public static class Util {

    static Optional<Method> findApiMethod(Class<?> container, String name) {
      try {
        var method = container.getMethod(name);
        return isApiMethod(method) ? Optional.of(method) : Optional.empty();
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    static boolean isApiMethod(Method method) {
      if (method.getDeclaringClass().equals(Object.class)) return false;
      if (Modifier.isStatic(method.getModifiers())) return false;
      return method.getParameterCount() == 0;
    }
  }
}
