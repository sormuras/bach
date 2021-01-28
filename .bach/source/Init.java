import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class Init {

  private static final Consumer<Object> out = System.out::println;

  public static void dir() throws Exception {
    dir("");
  }

  public static void dir(String folder) throws Exception {
    dir(folder, "*");
  }

  public static void dir(String folder, String glob) throws Exception {
    var directory = Path.of(folder).toAbsolutePath().normalize();
    var paths = new ArrayList<Path>();
    try (var stream = Files.newDirectoryStream(directory, glob)) {
      for (var path : stream) {
        // if (Files.isHidden(path)) continue;
        paths.add(path);
      }
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
      else {
        files++;
        var size = Files.size(path);
        bytes += size;
        out.accept(String.format("%,15d %s", size, name));
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

  public static void tree() throws Exception {
    tree("");
  }

  public static void tree(String folder) throws Exception {
    tree(folder, Files::isDirectory);
  }

  public static void tree(String folder, Predicate<Path> filter) throws Exception {
    var root = Path.of(folder);
    try (var stream = Files.walk(root)) {
      stream
          .filter(filter)
          .map(root::relativize)
          .map(Path::toString)
          .filter(Predicate.not(String::isEmpty))
          .map(string -> string.replace('\\', '/'))
          .sorted()
          .forEach(out);
    }
  }

  public static ProjectTemplate newProject(String name) {
    return new ProjectTemplate(Path.of(""), name);
  }

  record ProjectTemplate(Path directory, String name) {

    void createProject() throws Exception {
      out.accept("Create project using " + this);
      var base = directory.resolve(name);
      if (Files.exists(base)) {
        out.accept("Path already exists: " + base);
        return;
      }
      Files.createDirectories(base);
      out.accept("Created project " + name);
      tree(base.toString(), Files::exists);
    }
  }
}
