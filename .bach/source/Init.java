import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class Init {

  public static final Version DEFAULT_BACH_VERSION = Version.parse("16-ea");

  private static final Consumer<Object> out = System.out::println;

  public static void dir() {
    dir("");
  }

  public static void dir(String folder) {
    dir(folder, "*");
  }

  public static void dir(String folder, String glob) {
    var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
    var directory = Path.of(folder).toAbsolutePath().normalize();
    var paths = new ArrayList<Path>();
    try (var stream = Files.newDirectoryStream(directory, glob)) {
      for (var path : stream) {
        if (win && Files.isHidden(path)) continue;
        paths.add(path);
      }
    } catch (Exception exception) {
      out.accept(exception);
    }
    paths.sort(
        (Path p1, Path p2) -> {
          var one = Files.isDirectory(p1);
          var two = Files.isDirectory(p2);
          if (one && !two) return -1; // directory before file
          if (!one && two) return 1; // file after directory
          return p1.compareTo(p2); // order lexicographically
        });
    long files = 0;
    long bytes = 0;
    for (var path : paths) {
      var name = path.getFileName().toString();
      if (Files.isDirectory(path)) out.accept(String.format("%-15s %s", "[+]", name));
      else
        try {
          files++;
          var size = Files.size(path);
          bytes += size;
          out.accept(String.format("%,15d %s", size, name));
        } catch (Exception exception) {
          out.accept(exception);
          return;
        }
    }
    var all = paths.size();
    if (all == 0) {
      out.accept(String.format("Directory %s is empty", directory));
      return;
    }
    out.accept("");
    out.accept(String.format("%15d path%s in directory %s", all, all == 1 ? "" : "s", directory));
    out.accept(String.format("%,15d bytes in %d file%s", bytes, files, files == 1 ? "" : "s"));
  }

  public static void tree() {
    tree("");
  }

  public static void tree(String folder) {
    tree(folder, name -> name.contains("module-info"));
  }

  public static void tree(String folder, Predicate<String> fileNameFilter) {
    var directory = Path.of(folder).toAbsolutePath();
    out.accept(folder.isEmpty() ? directory : folder);
    var files = tree(directory, "  ", fileNameFilter);
    out.accept("  ");
    out.accept(String.format("%d file%s in tree of %s", files, files == 1 ? "" : "s", directory));
  }

  private static int tree(Path directory, String indent, Predicate<String> filter) {
    var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
    var files = 0;
    try (var stream = Files.newDirectoryStream(directory, "*")) {
      for (var path : stream) {
        if (win && Files.isHidden(path)) continue;
        var name = path.getFileName().toString();
        if (Files.isDirectory(path)) {
          out.accept(indent + name + "/");
          if (name.equals(".git")) continue;
          files += tree(path, indent + "  ", filter);
          continue;
        }
        files++;
        if (filter.test(name)) out.accept(indent + name);
      }
    } catch (Exception exception) {
      out.accept(exception);
    }
    return files;
  }

  public static Project projectJigsawQuickStartGreetings() {
    return new Project()
        .withName("jigsaw-quick-start-greetings")
        .withModule(ModuleDescriptor.newModule("com.greetings").build());
  }

  public static Project projectJigsawQuickStartWorld() {
    return new Project()
        .withName("jigsaw-quick-start-world")
        .withModule(ModuleDescriptor.newModule("com.greetings").requires("org.astro").build())
        .withModule(
            ModuleDescriptor.newModule("org.astro").exports("org.astro").build(),
            Map.of(
                Path.of("org/astro/World.java"),
                """
                package org.astro;
                public class World {}
                """));
  }

  record Project(String name, Version version, Map<String, ModuleSource> modules) {

    record ModuleSource(ModuleDescriptor descriptor, Map<Path, String> files) {}

    Project() {
      this("noname", Version.parse("0-ea"), Map.of());
    }

    Project withName(String name) {
      return new Project(name, version, modules);
    }

    Project withVersion(String version) {
      return withVersion(Version.parse(version));
    }

    Project withVersion(Version version) {
      return new Project(name, version, modules);
    }

    Project withModule(ModuleDescriptor descriptor) {
      return withModule(descriptor, Map.of());
    }

    Project withModule(ModuleDescriptor descriptor, Map<Path, String> files) {
      return withModule(new ModuleSource(descriptor, files));
    }

    Project withModule(ModuleSource module) {
      var modules = new TreeMap<>(this.modules);
      modules.put(module.descriptor.name(), module);
      return new Project(name, version, modules);
    }

    void create() throws Exception {
      creator().create();
    }

    Creator creator() {
      return new Creator(DEFAULT_BACH_VERSION, Path.of(""), this, Set.of());
    }
  }

  record Creator(Version bach, Path directory, Project project, Set<Flag> flags) {

    enum Flag {
      DRY_RUN
    }

    Creator withBachVersion(String version) {
      var bach = Version.parse(version);
      return new Creator(bach, directory, project, flags);
    }

    Creator withParentDirectory(String first, String... more) {
      var directory = Path.of(first, more);
      return new Creator(bach, directory, project, flags);
    }

    Creator withDryRunFlag() {
      return with(Flag.DRY_RUN);
    }

    Creator with(Flag flag, Flag... more) {
      var copy = EnumSet.of(flag, more);
      copy.addAll(flags);
      return new Creator(bach, directory, project, copy);
    }

    boolean isDryRun() {
      return flags.contains(Flag.DRY_RUN);
    }

    boolean isDryRun(String format, Object... args) {
      var dry = isDryRun();
      out.accept(String.format((dry ? "[dry-run] " : "") + format, args));
      return dry;
    }

    boolean isNormalRun() {
      return !isDryRun();
    }

    void create() throws Exception {
      var base = directory.resolve(project.name);
      if (isNormalRun()) {
        if (Files.exists(base)) {
          out.accept("Path already exists: " + base);
          return;
        }
        Files.createDirectories(base);
      }
      createBuildModule();
      createCacheDirectoryByDownloadBach();
      createLaunchers();
      createModules();
      if (isNormalRun()) {
        out.accept("");
        out.accept("Created project " + project.name + " in " + directory.toAbsolutePath());
        tree(base.toString(), __ -> true);
        out.accept("");
        out.accept("Next steps:");
        out.accept(" - /exit to return to your shell");
        out.accept(" - cd " + base);
        out.accept(" - bach build");
      }
    }

    void createBuildModule() throws Exception {
      var base = directory.resolve(project.name);
      var info = base.resolve(".bach/build/module-info.java");
      var text =
          """
          // @com.github.sormuras.bach.ProjectInfo()
          module build {
            requires com.github.sormuras.bach;
            // provides com.github.sormuras.bach.Bach with build.CustomBach;
          }
          """;

      if (isDryRun("Create build module declaration: %s", info)) return;

      Files.createDirectories(info.getParent());
      Files.writeString(info, text);
    }

    void createCacheDirectoryByDownloadBach() throws Exception {
      var base = directory.resolve(project.name);
      var cache = base.resolve(".bach/cache");
      var module = "com.github.sormuras.bach";
      var jar = module + '@' + bach + ".jar";
      var source = "https://github.com/sormuras/bach/releases/download/" + bach + '/' + jar;
      var target = cache.resolve(jar);

      if (isDryRun("Create cache by downloading %s to %s", jar, cache)) return;

      Files.createDirectories(cache);
      try (var stream = new URL(source).openStream()) {
        Files.copy(stream, target);
      }
    }

    void createLaunchers() throws Exception {
      createLauncherScriptForUnix();
      createLauncherScriptForWindows();
    }

    void createLauncherScriptForUnix() throws Exception {
      var base = directory.resolve(project.name);
      var bash = base.resolve("bach");
      var text =
          """
          #!/usr/bin/env bash
          java --module-path .bach/cache --module com.github.sormuras.bach "$@"
          """;

      if (isDryRun("Create launcher for Linux/MacOS: %s", bash)) {
        out.accept(text.indent(10 + 4).stripTrailing());
        return;
      }

      Files.writeString(bash, text).toFile().setExecutable(true);
    }

    void createLauncherScriptForWindows() throws Exception {
      var base = directory.resolve(project.name);
      var bat = base.resolve("bach.bat");
      var text =
          """
          @ECHO OFF
          java --module-path .bach\\cache --module com.github.sormuras.bach %*
          """;

      if (isDryRun("Create launcher for Windows: %s", bat)) {
        out.accept(text.indent(10 + 4).stripTrailing());
        return;
      }

      Files.writeString(bat, text);
    }

    void createModules() throws Exception {
      createModulesOfMainModuleSpace();
      createModulesOfTestModuleSpace();
    }

    void createModulesOfMainModuleSpace() throws Exception {
      var base = directory.resolve(project.name);
      var offset = Path.of("main/java");
      // module descriptor files
      for (var source : project.modules.values()) {
        var module = source.descriptor;
        var file = base.resolve(module.name()).resolve(offset).resolve("module-info.java");
        var text = new StringJoiner(System.lineSeparator());
        var open = module.isOpen() ? "open " : "";
        text.add(open + "module " + module.name() + " {");
        // exports PACKAGE [to MODULE[, ...]] ;
        for (var exports : new TreeSet<>(module.exports())) {
          var directive = new StringJoiner(" ");
          directive.add("exports");
          directive.add(exports.source());
          if (!exports.targets().isEmpty()) {
            directive.add("to");
            directive.add(String.join(", ", new TreeSet<>(exports.targets())));
          }
          text.add("  " + directive + ";");
        }
        // requires [STATIC] [TRANSITIVE] MODULE ;
        for (var requires : new TreeSet<>(module.requires())) {
          var modifiers = requires.modifiers();
          if (modifiers.contains(Requires.Modifier.MANDATED)) continue;
          var directive = new StringJoiner(" ");
          directive.add("requires");
          if (modifiers.contains(Requires.Modifier.STATIC)) directive.add("static");
          if (modifiers.contains(Requires.Modifier.TRANSITIVE)) directive.add("transitive");
          directive.add(requires.name());
          text.add("  " + directive + ";");
        }
        text.add("}");

        if (isDryRun("Create module declaration: %s", file)) {
          out.accept(text.toString().indent(10 + 4).stripTrailing());
          continue;
        }

        Files.createDirectories(file.getParent());
        Files.writeString(file, text.toString());
      }
      // files
      for (var source : project.modules.values()) {
        var module = source.descriptor;
        for (var entry : source.files.entrySet()) {
          var file = base.resolve(module.name()).resolve(offset).resolve(entry.getKey());
          if (isDryRun("Create file: %s", file)) {
            out.accept(entry.getValue().indent(10 + 4).stripTrailing());
            continue;
          }
          Files.createDirectories(file.getParent());
          Files.writeString(file, entry.getValue());
        }
      }
    }

    void createModulesOfTestModuleSpace() {}
  }

  /** Hidden default constructor. */
  private Init() {}
}
