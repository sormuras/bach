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

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

class BuildJigsawQuickStartWorldWithBach {

  public static void main(String... arguments) {
    var base = new Base(Path.of("doc", "project", "JigsawQuickStartWorld"));
    var main = newMainRealm(base);
    var test = newTestRealm(base, main);
    var project = new Project("Jigsaw Quick-Start World Guide", List.of(main, test));
    var bach = new Bach(project);
    bach.build();
  }

  private static Realm newMainRealm(Base base) {
    var units =
        List.of(
            new Unit(
                ModuleDescriptor.newModule("com.greetings").mainClass("com.greetings.Main").build(),
                List.of(new CreateJar(base, "main", "com.greetings"))),
            new Unit(
                ModuleDescriptor.newModule("org.astro").build(),
                List.of(new CreateJar(base, "main", "org.astro"))));
    var moduleNames = units.stream().map(Unit::name).collect(Collectors.joining(","));
    var moduleSourcePath = String.join(File.separator, base.path.toString(), "*", "main");
    var javac =
        RunTool.of(
            "javac",
            "--module",
            moduleNames,
            "--module-source-path",
            moduleSourcePath,
            "-d",
            base.classes("main"));
    var javadoc =
        Task.sequence(
            "Create API documentation",
            List.of(
                new CreateDirectories(base.api()),
                RunTool.of(
                    "javadoc",
                    "--module",
                    moduleNames,
                    "--module-source-path",
                    moduleSourcePath,
                    "-quiet",
                    "-encoding",
                    "UTF-8",
                    "-locale",
                    "en",
                    "-d",
                    base.api())));
    var jlink =
        Task.sequence(
            "Create custom runtime image",
            List.of(
                new DeleteDirectories(base.image()),
                RunTool.of(
                    "jlink",
                    "--launcher",
                    "greet=com.greetings/com.greetings.Main",
                    "--add-modules",
                    moduleNames,
                    "--module-path",
                    base.modules("main"),
                    "--compress",
                    "2",
                    "--strip-debug",
                    "--no-header-files",
                    "--no-man-pages",
                    "--output",
                    base.image())));
    return new Realm("main", units, javac, List.of(javadoc, jlink));
  }

  private static Realm newTestRealm(Base base, Realm main) {
    var units =
        List.of(
            new Unit(
                ModuleDescriptor.newOpenModule("test.modules").build(),
                List.of(new CreateJar(base, "test", "test.modules"))),
            new Unit(
                ModuleDescriptor.newOpenModule("org.astro").build(),
                List.of(new CreateJar(base, "test", "org.astro"))));
    var moduleNames = units.stream().map(Unit::name).collect(Collectors.joining(","));
    var moduleSourcePath = String.join(File.separator, base.path.toString(), "*", "test");
    var mains = base.modules(main.name);
    var tests = base.modules("test");
    var javac =
        RunTool.of(
            "javac",
            "--module",
            moduleNames,
            "--module-source-path",
            moduleSourcePath,
            "--module-path",
            mains,
            "--patch-module",
            "org.astro=" + base.path("org.astro", main.name),
            "-d",
            base.classes("test"));

    var more =
        List.of(
            new RunTestModule("test.modules", List.of(tests.resolve("test.modules.jar"), mains)),
            new RunTestModule("org.astro", List.of(tests.resolve("org.astro.jar"))));
    return new Realm("test", units, javac, more);
  }

  static final class Base {
    private final Path path;
    private final Path workspace;

    Base(Path path) {
      this(path, path.resolve(".bach/workspace"));
    }

    Base(Path path, Path workspace) {
      this.path = path;
      this.workspace = workspace;
    }

    Path path(String first, String... more) {
      return path.resolve(Path.of(first, more));
    }

    Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }

    Path api() {
      return workspace("api");
    }

    Path classes(String realm) {
      return workspace("classes", realm);
    }

    Path classes(String realm, String module) {
      return workspace("classes", realm, module);
    }

    Path image() {
      return workspace("image");
    }

    Path modules(String realm) {
      return workspace("modules", realm);
    }
  }

  static final class Unit {

    private final ModuleDescriptor descriptor;
    private final List<? extends Task> tasks;

    public Unit(ModuleDescriptor descriptor, List<? extends Task> tasks) {
      this.descriptor = descriptor;
      this.tasks = tasks;
    }

    public String name() {
      return descriptor.name();
    }
  }

  static final class Realm {
    private final String name;
    private final List<Unit> units;
    private final Task javac;
    private final List<? extends Task> more;

    public Realm(String name, List<Unit> units, Task javac, List<? extends Task> more) {
      this.name = name;
      this.units = units;
      this.javac = javac;
      this.more = more;
    }
  }

  static final class Project {
    private final String title;
    private final List<Realm> realms;

    public Project(String title, List<Realm> realms) {
      this.title = title;
      this.realms = realms;
    }
  }

  static class Bach {

    private final Project project;

    public Bach(Project project) {
      this.project = project;
    }

    public void build() {
      execute(buildSequence());
    }

    Task buildSequence() {
      var tasks = new ArrayList<Task>();
      tasks.add(new PrintSystemProperties());
      tasks.add(new PrintProjectComponents());
      for (var realm : project.realms) {
        tasks.add(realm.javac);
        for (var unit : realm.units) tasks.addAll(unit.tasks);
        tasks.addAll(realm.more);
      }
      return Task.sequence("Build Sequence", tasks);
    }

    void execute(Task task) {
      var tasks = task.list;
      if (tasks.isEmpty()) {
        System.out.println("* " + task.label);
        try {
          task.execute(this);
        } catch (Exception e) {
          throw new Error("Task execution failed", e);
        }
        return;
      }
      System.out.println("+ " + task.label);
      var start = System.currentTimeMillis();
      for (var sub : tasks) execute(sub);
      var duration = System.currentTimeMillis() - start;
      System.out.println("= " + task.label + " took " + duration + " ms");
    }

    void execute(ToolProvider tool, String... args) {
      var call = (tool.name() + ' ' + String.join(" ", args)).trim();
      var code = tool.run(System.out, System.err, args);
      if (code == 0) return;
      var message = "Exit code " + code + " indicates an error running tool\n\t" + call;
      var error = new AssertionError(message);
      error.setStackTrace(new StackTraceElement[] {error.getStackTrace()[0]});
      throw error;
    }
  }

  static class Task {

    public static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }

    private final String label;
    private final List<Task> list;

    public Task() {
      this("", List.of());
    }

    public Task(String label, List<Task> list) {
      this.label = label.isBlank() ? getClass().getSimpleName() : label;
      this.list = list;
    }

    public void execute(Bach bach) throws Exception {}
  }

  static class RunTool extends Task {

    static RunTool of(String name, Object... arguments) {
      var tool = ToolProvider.findFirst(name).orElseThrow();
      var args = new String[arguments.length];
      for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
      return new RunTool(tool, args);
    }

    private final ToolProvider tool;
    private final String[] args;

    public RunTool(ToolProvider tool, String... args) {
      super(tool.name() + " " + String.join(" ", args), List.of());
      this.tool = tool;
      this.args = args;
    }

    @Override
    public void execute(Bach bach) {
      bach.execute(tool, args);
    }
  }

  static class RunTestModule extends Task {
    private final String module;
    private final List<Path> modulePaths;

    RunTestModule(String module, List<Path> modulePaths) {
      this.module = module;
      this.modulePaths = modulePaths;
    }

    @Override
    public void execute(Bach bach) {
      findTestTool(module, modulePaths.toArray(Path[]::new)).ifPresent(bach::execute);
    }

    Optional<ToolProvider> findTestTool(String module, Path... modulePaths) {
      var roots = Set.of(module);
      var finder = ModuleFinder.of(modulePaths);
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var parent = ClassLoader.getPlatformClassLoader();
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
      var layer = controller.layer();
      var loader = layer.findLoader(module);
      loader.setDefaultAssertionStatus(true);
      var services = ServiceLoader.load(layer, ToolProvider.class);
      var providers = services.stream().map(ServiceLoader.Provider::get);
      return providers.filter(provider -> provider.name().equals("test(" + module + ")")).findAny();
    }
  }

  static class PrintSystemProperties extends Task {

    @Override
    public void execute(Bach bach) {
      var lines = List.of("System Properties", line("os.name"), line("user.dir"));
      System.out.println(String.join(System.lineSeparator(), lines));
    }

    private static String line(String key) {
      return "\t" + key + "=" + System.getProperty(key);
    }
  }

  static class PrintProjectComponents extends Task {
    @Override
    public void execute(Bach bach) {
      System.out.println(bach.project.title);
    }
  }

  static class CreateDirectories extends Task {

    final Path directory;

    CreateDirectories(Path directory) {
      super("Create directories " + directory.toUri(), List.of());
      this.directory = directory;
    }

    @Override
    public void execute(Bach bach) throws Exception {
      Files.createDirectories(directory);
    }
  }

  static class DeleteDirectories extends Task {

    final Path directory;

    DeleteDirectories(Path directory) {
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

  static class CreateJar extends Task {

    private static List<Task> list(Base base, String realm, String module) {
      var classes = base.classes(realm, module);
      var modules = base.modules(realm);
      var jar = modules.resolve(module + ".jar");
      return List.of(
          new CreateDirectories(modules),
          RunTool.of("jar", "--create", "--file", jar, "-C", classes, "."),
          RunTool.of("jar", "--describe-module", "--file", jar));
    }

    public CreateJar(Base base, String realm, String module) {
      super("Create and describe JAR file for module " + module, list(base, realm, module));
    }
  }
}
