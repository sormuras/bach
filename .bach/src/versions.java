import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolFinder;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.spi.ToolProvider;

class versions {
  public static void main(String... args) {
    var bach = new Bach("--verbose");

    bach.logCaption("Default Tool Finder");
    bach.run("jar", jar -> jar.with("--version"));

    bach.logCaption("Explicit Tool Finder");
    bach.run(ToolFinder.ofSystem(), "jar", jar -> jar.with("--version"));

    bach.logCaption("Start an external process");
    bach.run(Path.of(System.getProperty("java.home"), "bin", "jar"), jar -> jar.with("--version"));

    bach.logCaption("Versions of all tools provided by the runtime system");
    var providers = ToolFinder.ofSystem().findAll().stream()
        .sorted(Comparator.comparing(ToolProvider::name))
        .toList();
    for (var provider : providers) {
      var finder = ToolFinder.of(provider);
      var name = provider.name();
      bach.run(finder, name, call -> call.with(name.equals("javap") ? "-version" : "--version"));
    }
  }
}
