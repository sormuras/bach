import java.lang.module.ModuleDescriptor.Version;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class Init {

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

  public static Project project() {
    return new Project();
  }

  record Project(Path directory, String name, Version bach) {

    Project() {
      this(Path.of(""), "noname", Version.parse("17-ea"));
    }

    Project in(String directory) {
      return new Project(Path.of(directory), name, bach);
    }

    Project name(String name) {
      return new Project(directory, name, bach);
    }

    Project bach(String version) {
      return new Project(directory, name, Version.parse(version));
    }

    void create() throws Exception {
      out.accept("Create " + this);
      var base = directory.resolve(name);
      if (Files.exists(base)) {
        out.accept("Path already exists: " + base);
        return;
      }
      Files.createDirectories(base);
      createBuildModule();
      createCache();
      createLaunchers();
      out.accept("");
      out.accept("Created project " + name);
      tree(base.toString(), __ -> true);
    }

    void createBuildModule() throws Exception {
      var base = directory.resolve(name);
      var info = base.resolve(".bach/build/module-info.java");
      out.accept("Create build module declaration: " + info);
      Files.createDirectories(info.getParent());
      Files.writeString(
          info,
          """
              // @ProjectInfo()
              module build {
                requires com.github.sormuras.bach;
                // provides com.github.sormuras.bach.Bach with build.CustomBach;
              }
              """);
    }

    void createCache() throws Exception {
      var base = directory.resolve(name);
      var cache = base.resolve(".bach/cache");
      var module = "com.github.sormuras.bach";
      var jar = module + '@' + bach + ".jar";
      var source = "https://github.com/sormuras/bach/releases/download/" + bach + '/' + jar;
      var target = cache.resolve(jar);

      Files.createDirectories(cache);
      try (var stream = new URL(source).openStream()) {
        Files.copy(stream, target);
      }
    }

    void createLaunchers() throws Exception {
      var base = directory.resolve(name);

      var bash = base.resolve("bach");
      out.accept("Create launcher: " + bash);
      Files.writeString(
              bash,
              """
              #!/usr/bin/env bash

              if [[ $1 != 'init' ]]; then
                java --module-path .bach/cache --module com.github.sormuras.bach "$@"
              else
                rm -f .bach/cache/com.github.sormuras.bach@*.jar
                jshell -R-Dreboot -R-Dversion="${2:-17-ea}" https://bit.ly/bach-main-init
              fi
              """)
          .toFile()
          .setExecutable(true);

      var bat = base.resolve("bach.bat");
      out.accept("Create launcher: " + bat);
      Files.writeString(
          bat,
          """
            @ECHO OFF

            IF [%1]==[init] GOTO INIT

            java --module-path .bach\\cache --module com.github.sormuras.bach %*

            GOTO END

            :INIT
            del .bach\\cache\\com.github.sormuras.bach@*.jar >nul 2>&1
            SETLOCAL
            IF [%2]==[] ( SET tag=17-ea ) ELSE ( SET tag=%2 )
            jshell -R-Dreboot -R-Dversion=%tag% https://bit.ly/bach-main-init
            ENDLOCAL

            :END
            """);
    }
  }
}
