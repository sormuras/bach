import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class boot {

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

  public static void tree() {
    tree("");
  }

  public static void tree(String folder) {
    tree(folder, name -> name.contains("module-info"));
  }

  public static void tree(String folder, Predicate<String> fileNameFilter) {
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

  private static final Consumer<Object> out = System.out::println;

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

  /** Hidden default constructor. */
  private boot() {}
}
