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

package de.sormuras.bach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/*BODY*/
public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default configuration.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(out, err, new Configuration());
  }

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = Bach.of();
    bach.main(args.length == 0 ? List.of("build") : List.of(args));
  }

  /** Text-output writer. */
  final PrintWriter out, err;

  /** Configuration. */
  final Configuration configuration;

  public Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.configuration = Util.requireNonNull(configuration, "configuration");
  }

  void main(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      try {
        switch (argument) {
          case "build":
            build();
            continue;
          case "validate":
            validate();
            continue;
        }
        // Try Bach API method w/o parameter -- single argument is consumed
        var method = Util.findApiMethod(getClass(), argument);
        if (method.isPresent()) {
          method.get().invoke(this);
          continue;
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(argument);
        if (tool.isPresent()) {
          run(new Command(argument).addEach(arguments));
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

  public void build() {
    info();
    validate();
    resolve();
    compile();
  }

  public void clean() {
    Util.treeDelete(configuration.getWorkspaceDirectory());
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
    configuration.print(out);
  }

  public void validate() {
    var home = configuration.getHomeDirectory();
    Validation.validateDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new Validation.Error("home contains a directory", home.toUri());
    var work = configuration.getWorkspaceDirectory();
    if (Files.exists(work)) {
      Validation.validateDirectory(work);
      if (!work.toFile().canWrite())
        throw new Validation.Error("bin is writable: %s", work.toUri());
    } else {
      var parentOfBin = work.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new Validation.Error("parent of work is writable", parentOfBin.toUri());
    }
    var lib = configuration.getLibraryDirectory();
    var libs = configuration.getLibraryPaths();
    Validation.validateDirectoryIfExists(lib);
    if (!libs.contains(lib)) {
      throw new Validation.Error(lib + " is member of paths", libs);
    }
    configuration.getSourceDirectories().forEach(Validation::validateDirectory);
  }

  public void resolve() {
    Resolver.resolve(this);
  }

  public void compile() {
    var main = new Realm("main", configuration);
    compile(main);
    // var test = new Realm("test", configuration, main);
    // compile(test);
  }

  private void compile(Realm realm) {
    var modules = new TreeSet<>(realm.getDeclaredModules());
    if (modules.isEmpty()) {
      out.println("No modules declared, nothing to compile.");
      return;
    }
    var jigsaw = new Jigsaw(this);
    modules.removeAll(jigsaw.compile(realm, modules));
    if (modules.isEmpty()) {
      return;
    }
    throw new IllegalStateException("not compiled modules: " + modules);
  }

  public void version() {
    out.println(getBanner());
  }

  void run(Command command) {
    var name = command.getName();
    var code = run(name, (Object[]) command.toStringArray());
    if (code != 0) {
      throw new AssertionError(name + " exited with non-zero result: " + code);
    }
  }

  int run(String name, Object... arguments) {
    var strings = Arrays.stream(arguments).map(Object::toString).toArray(String[]::new);
    out.println(name + " " + String.join(" ", strings));
    var tool = ToolProvider.findFirst(name).orElseThrow();
    return tool.run(out, err, strings);
  }
}
