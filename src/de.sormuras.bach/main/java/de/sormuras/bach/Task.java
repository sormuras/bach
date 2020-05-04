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

package de.sormuras.bach;

import de.sormuras.bach.util.ModulesResolver;
import de.sormuras.bach.util.Resources;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** A piece of work to be done or undertaken. */
public /*static*/ class Task {

  public static Task sequence(String label, Task... tasks) {
    return sequence(label, List.of(tasks));
  }

  public static Task sequence(String label, List<Task> tasks) {
    return new Task(label, tasks);
  }

  public static Task runTool(String name, Object... arguments) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    return new RunTool(tool, args);
  }

  private final String label;
  private final List<Task> list;
  private final StringWriter out;
  private final StringWriter err;

  public Task() {
    this("", List.of());
  }

  public Task(String label, List<Task> list) {
    Objects.requireNonNull(label, "label");
    this.label = label.isBlank() ? getClass().getSimpleName() : label;
    this.list = List.copyOf(Objects.requireNonNull(list, "list"));
    this.out = new StringWriter();
    this.err = new StringWriter();
  }

  public String getLabel() {
    return label;
  }

  public List<Task> getList() {
    return list;
  }

  public StringWriter getOut() {
    return out;
  }

  public StringWriter getErr() {
    return err;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
        .add("label='" + label + "'")
        .add("list.size=" + list.size())
        .toString();
  }

  public void execute(Bach bach) throws Exception {}

  public static class RunTool extends Task {

    private final ToolProvider tool;
    private final String[] args;

    public RunTool(ToolProvider tool, String... args) {
      super(tool.name() + " " + String.join(" ", args), List.of());
      this.tool = tool;
      this.args = args;
    }

    @Override
    public void execute(Bach bach) {
      bach.execute(tool, new PrintWriter(getOut()), new PrintWriter(getErr()), args);
    }
  }

  public static class CreateDirectories extends Task {

    private final Path directory;

    public CreateDirectories(Path directory) {
      super("Create directories " + directory.toUri(), List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      Files.createDirectories(directory);
    }
  }

  public static class DeleteDirectories extends Task {

    private final Path directory;

    public DeleteDirectories(Path directory) {
      super("Delete directory " + directory, List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      if (Files.notExists(directory)) return;
      try (var stream = Files.walk(directory)) {
        var paths = stream.sorted((p, q) -> -p.compareTo(q));
        for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
      }
    }
  }

  public static class CreateJar extends Task {

    private static List<Task> list(Path jar, Path classes) {
      return List.of(
          new CreateDirectories(jar.getParent()),
          runTool("jar", "--create", "--file", jar, "-C", classes, "."),
          runTool("jar", "--describe-module", "--file", jar));
    }

    public CreateJar(Path jar, Path classes) {
      super("Create modular JAR file " + jar.getFileName(), list(jar, classes));
    }
  }

  /** Determine and transport missing library modules. */
  public static class ResolveMissingModules extends Task {

    private static String central(String group, String artifact, String version) {
      var host = "https://repo.maven.apache.org/maven2";
      var file = artifact + '-' + version + ".jar";
      return String.join("/", host, group.replace('.', '/'), artifact, version, file);
    }

    private final Map<String, String> map;

    public ResolveMissingModules() {
      super("Resolve missing modules", List.of());
      this.map = new TreeMap<>();
      var jupiter = "org.junit.jupiter";
      map.put(jupiter, central(jupiter, "junit-jupiter", "5.6.2"));
      map.put(jupiter + ".api", central(jupiter, "junit-jupiter-api", "5.6.2"));
      map.put(jupiter + ".engine", central(jupiter, "junit-jupiter-engine", "5.6.2"));
      map.put(jupiter + ".params", central(jupiter, "junit-jupiter-params", "5.6.2"));
      var platform = "org.junit.platform";
      map.put(platform + ".commons", central(platform, "junit-platform-commons", "1.6.2"));
      map.put(platform + ".console", central(platform, "junit-platform-console", "1.6.2"));
      map.put(platform + ".engine", central(platform, "junit-platform-engine", "1.6.2"));
      map.put(platform + ".launcher", central(platform, "junit-platform-launcher", "1.6.2"));
      map.put(platform + ".reporting", central(platform, "junit-platform-reporting", "1.6.2"));
      map.put(platform + ".testkit", central(platform, "junit-platform-testkit", "1.6.2"));
      // various artists
      map.put("org.apiguardian.api", central("org.apiguardian", "apiguardian-api", "1.1.0"));
      map.put("org.opentest4j", central("org.opentest4j", "opentest4j", "1.2.0"));
    }

    @Override
    public void execute(Bach bach) {
      var project = bach.getProject();
      var lib = project.base().directory().resolve("lib");
      class Transporter implements Consumer<Set<String>> {
        @Override
        public void accept(Set<String> modules) {
          var resources = new Resources(HttpClient.newHttpClient());
          for (var module : modules) {
            var uri = map.get(module);
            if (uri == null) continue;
            try {
              var name = module + ".jar";
              var file = resources.copy(URI.create(uri), lib.resolve(name));
              var size = Files.size(file);
              bach.getLogger().log(Level.INFO, "{0} ({1} bytes) << {2}", file, size, uri);
            } catch (Exception e) {
              throw new Error("Resolve module '" + module + "' failed: " + uri +"\n\t" + e, e);
            }
          }
        }
      }
      var declared = project.toDeclaredModuleNames();
      var required = project.toRequiredModuleNames();
      var resolver = new ModulesResolver(new Path[] {lib}, declared, new Transporter());
      resolver.resolve(required);
    }
  }
}
