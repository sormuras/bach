import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;

@SuppressWarnings("unused")
class boot {

  static void beep() {
    System.out.print("\007"); // 🔔
    System.out.flush();
  }

  interface bach {
    static Bach get() {
      return bach.get();
    }
    static void set(Bach instance) {
      bach.set(instance);
    }
  }

  interface files {

    static void dir() {
      dir("");
    }

    static void dir(String folder) {
      dir(folder, "*");
    }

    static void dir(String folder, String glob) {
      var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
      var directory = Path.of(folder).toAbsolutePath().normalize();
      var paths = new ArrayList<Path>();
      try (var stream = Files.newDirectoryStream(directory, glob)) {
        for (var path : stream) {
          if (win && Files.isHidden(path)) continue;
          paths.add(path);
        }
      } catch (Exception exception) {
        out(exception);
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
        if (Files.isDirectory(path)) out("%-15s %s", "[+]", name);
        else
          try {
            files++;
            var size = Files.size(path);
            bytes += size;
            out("%,15d %s", size, name);
          } catch (Exception exception) {
            out(exception);
            return;
          }
      }
      var all = paths.size();
      if (all == 0) {
        out("Directory %s is empty", directory);
        return;
      }
      out("");
      out("%15d path%s in directory %s", all, all == 1 ? "" : "s", directory);
      out("%,15d bytes in %d file%s", bytes, files, files == 1 ? "" : "s");
    }

    static void tree() {
      tree("");
    }

    static void tree(String folder) {
      tree(folder, name -> name.contains("module-info"));
    }

    static void tree(String folder, Predicate<String> fileNameFilter) {
      var directory = Path.of(folder).toAbsolutePath();
      out("%s", folder.isEmpty() ? directory : folder);
      var files = tree(directory, "  ", fileNameFilter);
      out("");
      out("%d file%s in tree of %s", files, files == 1 ? "" : "s", directory);
    }

    private static int tree(Path directory, String indent, Predicate<String> filter) {
      var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
      var files = 0;
      try (var stream = Files.newDirectoryStream(directory, "*")) {
        for (var path : stream) {
          if (win && Files.isHidden(path)) continue;
          var name = path.getFileName().toString();
          if (Files.isDirectory(path)) {
            out(indent + name + "/");
            if (name.equals(".git")) continue;
            files += tree(path, indent + "  ", filter);
            continue;
          }
          files++;
          if (filter.test(name)) out(indent + name);
        }
      } catch (Exception exception) {
        out(exception);
      }
      return files;
    }
  }

  interface tool {

    static String describe(ToolProvider provider) {
      var name = provider.name();
      var module = provider.getClass().getModule();
      var by =
          Optional.ofNullable(module.getDescriptor())
              .map(ModuleDescriptor::toNameAndVersion)
              .orElse(module.toString());
      var info =
          switch (name) {
            case "jar" -> "Create an archive for classes and resources, and update or restore them";
            case "javac" -> "Read Java class and interface definitions and compile them into classes";
            case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
            case "javap" -> "Disassemble one or more class files";
            case "jdeps" -> "Launch the Java class dependency analyzer";
            case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
            case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
            case "jpackage" -> "Package a self-contained Java application";
            case "junit" -> "Launch the JUnit Platform";
            default -> provider.toString();
          };
      return "%s (provided by module %s)\n%s".formatted(name, by, info.indent(2)).trim();
    }

    static void list() {
      var providers = bach.get().computeToolProviders().toList();
      var size = providers.size();
      providers.stream()
          .map(tool::describe)
          .sorted()
          .map(description -> "\n" + description)
          .forEach(out);
      out("%n-> %d tool%s", size, size == 1 ? "" : "s");
    }

    static void run(String tool, Object... args) {
      var command = Command.of(tool);
      var recording = bach.get().run(args.length == 0 ? command : command.add("", args));
      if (!recording.errors().isEmpty()) out.accept(recording.errors());
      if (!recording.output().isEmpty()) out.accept(recording.output());
      if (recording.isError()) out.accept("Tool " + tool + " returned exit code " + recording.code());
    }
  }

  private static void out(Exception exception) {
    out("""
        #
        # %s
        #
        """, exception);
  }

  private static void out(String format, Object... args) {
    out.accept(args == null || args.length == 0 ? format : String.format(format, args));
  }

  private static final Consumer<Object> out = System.out::println;
  private static final AtomicReference<Bach> bach = new AtomicReference<>(Bach.of("configuration"));

  /** Hidden default constructor. */
  private boot() {}
}
