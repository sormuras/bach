import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

class BuildJigsawQuickStartWorldWithBach {

  public static void main(String... arguments) {
    var base = new Base(Path.of("doc", "project", "JigsawQuickStartWorld"));

    var main = new Realm();
    main.name = "main";
    main.flags = EnumSet.of(Realm.Flag.CREATE_API_DOCUMENTATION, Realm.Flag.CREATE_RUNTIME_IMAGE);
    main.declares = Set.of("com.greetings", "org.astro");
    main.sourcePathPatterns.add(base.path + File.separator + '*' + File.separator + "main");

    var test = new Realm();
    test.name = "test";
    test.flags = EnumSet.of(Realm.Flag.LAUNCH_TESTS);
    test.declares = Set.of("test.modules", "org.astro");
    test.requires = List.of(main);
    test.sourcePathPatterns.add(base.path + File.separator + '*' + File.separator + "test");
    test.modulePaths.add(base.modules(main.name));
    test.patches.put("org.astro", List.of(base.path("org.astro", "main")));

    var project = new Project();
    project.title = "Jigsaw Quick-Start Guide";
    project.realms.add(main);
    project.realms.add(test);

    var bach = new Bach();
    bach.base = base;
    bach.project = project;
    bach.build();
  }

  static class Arguments {
    private final List<String> list = new ArrayList<>();

    Arguments(Object... arguments) {
      addAll(arguments);
    }

    List<String> build() {
      return List.copyOf(list);
    }

    Arguments add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    Arguments add(String key, Object value) {
      return add(key).add(value);
    }

    Arguments add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }

    Arguments add(boolean predicate, Object first, Object... more) {
      return predicate ? add(first).addAll(more) : this;
    }

    @SafeVarargs
    final <T> Arguments addAll(T... arguments) {
      for (var argument : arguments) add(argument);
      return this;
    }

    <T> Arguments forEach(Iterable<T> iterable, BiConsumer<Arguments, T> consumer) {
      iterable.forEach(argument -> consumer.accept(this, argument));
      return this;
    }
  }

  static class Base {
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

    Path modules(String realm) {
      return workspace("modules", realm);
    }
  }

  static class Realm {

    enum Flag {
      CREATE_API_DOCUMENTATION,
      CREATE_RUNTIME_IMAGE,
      LAUNCH_TESTS
    }

    String name;
    Set<Flag> flags = EnumSet.noneOf(Flag.class);
    Set<String> declares = new TreeSet<>(); // --module
    List<Realm> requires = new ArrayList<>();
    List<String> sourcePathPatterns = new ArrayList<>(); // --module-source-path
    List<Path> modulePaths = new ArrayList<>(); // --module-path
    Map<String, List<Path>> patches = new TreeMap<>(); // --patch-module

    @Override
    public String toString() {
      return name;
    }
  }

  static class Project {
    String title;
    List<Realm> realms = new ArrayList<>();
  }

  static class Task {

    private final String label;
    private final List<Task> list;

    Task() {
      this("", List.of());
    }

    Task(String label, List<Task> list) {
      this.label = label;
      this.list = list;
    }

    boolean runnable() {
      return list.isEmpty();
    }

    void run(Bach bach) {}
  }

  static class Tasks {

    static Task of(Callable<?> callable) {
      class CallableTask extends Task {
        @Override
        void run(Bach bach) {
          try {
            callable.call();
          } catch (Exception e) {
            throw new AssertionError("Computation failed", e);
          }
        }
      }
      return new CallableTask();
    }

    static Task of(Runnable runnable) {
      class RunnableTask extends Task {
        @Override
        void run(Bach bach) {
          runnable.run();
        }
      }
      return new RunnableTask();
    }

    static Task run(String tool, Arguments arguments) {
      return run(tool, arguments.build().toArray(String[]::new));
    }

    static Task run(String tool, String... args) {
      return new RunTool(ToolProvider.findFirst(tool).orElseThrow(), args);
    }

    static Task sequence(String label, List<Task> tasks) {
      return new Task(label, tasks);
    }

    static Task sequence(String label, Task... tasks) {
      return sequence(label, List.of(tasks));
    }

    static class PrintBasics extends Task {
      @Override
      void run(Bach bach) {
        System.out.println("System Properties");
        System.out.println("\tos.name=" + System.getProperty("os.name"));
        System.out.println("\tjava.version=" + System.getProperty("java.version"));
        System.out.println("\tuser.dir=" + System.getProperty("user.dir"));
        var base = bach.base;
        System.out.println("Base");
        System.out.println("\tpath=" + base.path + " -> " + base.path.toUri());
      }
    }

    static class PrintProject extends Task {

      @Override
      void run(Bach bach) {
        var project = bach.project;
        System.out.println("Project");
        System.out.println("\trealms=" + project.realms);
        System.out.println("Realm");
        for (var realm : project.realms) {
          System.out.println("\t" + realm.name);
          System.out.println("\t\tdeclares=" + realm.declares);
        }
      }
    }

    static class RunTool extends Task {
      private final ToolProvider tool;
      private final String[] args;

      RunTool(ToolProvider tool, String... args) {
        this.tool = tool;
        this.args = args;
      }

      @Override
      void run(Bach bach) {
        var call = (tool.name() + ' ' + String.join(" ", args)).trim();
        System.out.println(call);
        var code = tool.run(System.out, System.err, args);
        if (code == 0) return;
        var message = "Exit code " + code + " indicates an error running tool\n\t" + call;
        var error = new AssertionError(message);
        error.setStackTrace(new StackTraceElement[] {error.getStackTrace()[0]});
        throw error;
      }
    }
  }

  static class Bach {
    Base base;
    Project project;

    void build() {
      run(generateBuildTask());
    }

    Task generateBuildTask() {
      return Tasks.sequence(
          "Build project",
          new Tasks.PrintBasics(),
          new Tasks.PrintProject(),
          Tasks.run("javac", "--version"),
          generateCompileAllRealmsTask());
    }

    Task generateCompileAllRealmsTask() {
      var tasks = project.realms.stream().map(this::generateCompileRealmTask);
      return Tasks.sequence("Compile all realms", tasks.collect(Collectors.toList()));
    }

    Task generateCompileRealmTask(Realm realm) {
      var tasks = new ArrayList<Task>();
      var javac = new Arguments();
      var modules = String.join(",", realm.declares);
      var moduleSourcePath = String.join(File.pathSeparator, realm.sourcePathPatterns);
      javac
          .add("--module", modules)
          .add("--module-source-path", moduleSourcePath)
          .add(!realm.modulePaths.isEmpty(), "--module-path", Strings.join(realm.modulePaths))
          .forEach(
              realm.patches.entrySet(),
              (a, e) -> a.add("--patch-module", e.getKey() + '=' + Strings.join(e.getValue())))
          .add("-d", base.classes(realm.name));
      tasks.add(Tasks.run("javac", javac));
      tasks.add(Tasks.of(() -> Files.createDirectories(base.modules(realm.name))));
      for (var module : realm.declares) {
        var file = base.modules(realm.name).resolve(module + ".jar");
        var jar = new Arguments();
        jar.add("--create").add("--file", file).add("-C", base.classes(realm.name, module), ".");
        tasks.add(Tasks.run("jar", jar));
        var describe = new Arguments().add("--describe-module").add("--file", file);
        tasks.add(Tasks.run("jar", describe));
      }
      if (realm.flags.contains(Realm.Flag.CREATE_API_DOCUMENTATION)) {
        var javadoc = new Arguments();
        javadoc
            .add("--module", modules)
            .add("--module-source-path", moduleSourcePath)
            .add("-encoding", "UTF-8")
            .add("-locale", "en")
            .add("-linksource")
            .add("-use")
            .add(project.title != null, "-doctitle", project.title)
            .add(project.title != null, "-windowtitle", project.title)
            .add("-d", base.api());
        tasks.add(Tasks.run("javadoc", javadoc));
        tasks.add(Tasks.of(() -> System.out.println(base.api().resolve("index.html").toUri())));
      }
      return Tasks.sequence("Compile " + realm.name + " realm", tasks);
    }

    void run(Task task) {
      var label = task.label.isEmpty() ? task.getClass().getSimpleName() : task.label;
      if (task.runnable()) {
        System.out.printf("[task] %s%n", label);
        task.run(this);
        return;
      }
      System.out.println("[list] " + label + " begin");
      task.list.forEach(this::run);
      System.out.println("[list] " + label + " end");
    }
  }

  static class Strings {
    static String join(Collection<Path> paths) {
      return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }
  }
}
